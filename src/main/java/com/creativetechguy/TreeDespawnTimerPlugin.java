package com.creativetechguy;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.Angle;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
        name = "Tree Despawn Timer",
        description = "Show an estimate of the remaining time until a tree is chopped down",
        configName = TreeDespawnTimerConfig.GROUP_NAME
)
public class TreeDespawnTimerPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private TreeDespawnTimerConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private TreeDespawnTimerOverlay treeDespawnTimerOverlay;

    private final Pattern WOOD_CUT_PATTERN = Pattern.compile("You get (?:some|an)[\\w ]+(?:logs?|mushrooms)\\.");
    private final HashMap<WorldPoint, TreeState> treeAtLocation = new HashMap<>();
    protected HashSet<TreeState> uniqueTrees = new HashSet<>();
    private final HashMap<Player, TreeState> playerTreeChopping = new HashMap<>();
    private final HashMap<Player, Integer> newlySpawnedPlayers = new HashMap<>();
    private final ArrayList<Runnable> deferTickQueue = new ArrayList<>();
    int nextGarbageCollect = 100;

    @Provides
    TreeDespawnTimerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(TreeDespawnTimerConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(treeDespawnTimerOverlay);
        deferTickQueue.add(() -> {
            client.getPlayers().forEach(player -> {
                onPlayerSpawned(new PlayerSpawned(player));
                handlePlayerChopping(player);
            });
        });
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(treeDespawnTimerOverlay);
        treeAtLocation.clear();
        uniqueTrees.clear();
        playerTreeChopping.clear();
        newlySpawnedPlayers.clear();
        deferTickQueue.clear();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.HOPPING || event.getGameState() == GameState.LOGGING_IN) {
            treeAtLocation.clear();
            uniqueTrees.clear();
            playerTreeChopping.clear();
            newlySpawnedPlayers.clear();
            deferTickQueue.clear();
        }
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        deferTickQueue.forEach(Runnable::run);
        deferTickQueue.clear();

        uniqueTrees.forEach(TreeState::tick);
        nextGarbageCollect--;
        if (nextGarbageCollect <= 0) {
            ArrayList<TreeState> toDelete = new ArrayList<>();
            nextGarbageCollect = 25;
            uniqueTrees.forEach(entry -> {
                // Cleanup untouched trees far away from the player
                if ((entry.worldPoint.distanceTo(client.getLocalPlayer()
                        .getWorldLocation()) > 150 && !entry.shouldShowTimer()) || (entry.getTimeTicks() == 0 && entry.playersChopping.size() == 0)) {
                    toDelete.add(entry);
                }
            });
            toDelete.forEach(this::deleteTree);
        }
        newlySpawnedPlayers.entrySet().removeIf(p -> {
            p.setValue(p.getValue() - 1);
            return p.getValue() <= 0;
        });
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        GameObject gameObject = event.getGameObject();
        if (TreeConfig.isTree(gameObject)) {
            TreeState treeState = new TreeState(gameObject, client);
            if (treeAtLocation.containsKey(gameObject.getWorldLocation())) {
                return;
            }
            treeState.points.forEach(point -> treeAtLocation.put(point, treeState));
            uniqueTrees.add(treeState);
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        GameObject gameObject = event.getGameObject();
        if (!TreeConfig.isTree(gameObject)) {
            return;
        }
        TreeState treeState = treeAtLocation.get(gameObject.getWorldLocation());
        if (treeState == null) {
            return;
        }
        deleteTree(treeState);
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        if (event.getActor() instanceof Player) {
            Player player = (Player) event.getActor();
            deferTickQueue.add(() -> this.handlePlayerChopping(player));
        }
    }

    @Subscribe
    public void onPlayerSpawned(PlayerSpawned event) {
        Player player = event.getPlayer();
        if (player.equals(client.getLocalPlayer())) {
            return;
        }
        newlySpawnedPlayers.put(player, 8);
    }

    @Subscribe
    public void onPlayerDespawned(PlayerDespawned event) {
        Player player = event.getPlayer();
        if (playerTreeChopping.containsKey(player)) {
            TreeState treeState = playerTreeChopping.get(player);
            treeState.hasUnrenderedPlayersChopping = true;
            treeState.playersChopping.remove(player);
            playerTreeChopping.remove(player);
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.SPAM
                && event.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }
        if (WOOD_CUT_PATTERN.matcher(event.getMessage()).matches()) {
            TreeState interactingTree = playerTreeChopping.get(client.getLocalPlayer());
            if (interactingTree != null) {
                interactingTree.haveYouChoppedLog = true;
            }
        }
    }

    void handlePlayerChopping(Player player) {
        boolean isNewPlayer = newlySpawnedPlayers.containsKey(player);
        if (playerTreeChopping.containsKey(player)) {
            TreeState treeState = playerTreeChopping.get(player);
            treeState.playersChopping.remove(player);
            if (player.equals(client.getLocalPlayer())) {
                treeState.haveYouChoppedLog = false;
            }
            playerTreeChopping.remove(player);
        }
        if (isWoodcutting(player)) {
            TreeState interactingTree = findClosetFacingTree(player);
            if (interactingTree == null) {
                return;
            }
            // A player spawned in and nearly immediately started chopping, assume they've been chopping for a while
            if (isNewPlayer && !interactingTree.hasUnrenderedPlayersChopping && interactingTree.playersChopping.isEmpty()) {
                interactingTree.hideTree = true;
            }
            interactingTree.playersChopping.add(player);
            playerTreeChopping.put(player, interactingTree);
        }
    }

    void deleteTree(TreeState treeState) {
        treeState.playersChopping.forEach(playerTreeChopping::remove);
        treeState.points.forEach(treeAtLocation::remove);
        uniqueTrees.remove(treeState);
    }

    @Nullable
    TreeState findClosetFacingTree(Player player) {
        WorldPoint actorLocation = player.getWorldLocation();
        Direction direction = new Angle(player.getOrientation()).getNearestDirection();
        WorldPoint facingPoint = neighborPoint(actorLocation, direction);
        return treeAtLocation.get(facingPoint);
    }

    private WorldPoint neighborPoint(WorldPoint point, Direction direction) {
        switch (direction) {
            case NORTH:
                return point.dy(1);
            case SOUTH:
                return point.dy(-1);
            case EAST:
                return point.dx(1);
            case WEST:
                return point.dx(-1);
            default:
                throw new IllegalStateException();
        }
    }

    // From: https://github.com/Infinitay/tree-count-plugin/blob/master/src/main/java/treecount/TreeCountPlugin.java#L356
    private boolean isWoodcutting(Actor actor) {
        switch (actor.getAnimation()) {
            case AnimationID.WOODCUTTING_BRONZE:
            case AnimationID.WOODCUTTING_IRON:
            case AnimationID.WOODCUTTING_STEEL:
            case AnimationID.WOODCUTTING_BLACK:
            case AnimationID.WOODCUTTING_MITHRIL:
            case AnimationID.WOODCUTTING_ADAMANT:
            case AnimationID.WOODCUTTING_RUNE:
            case AnimationID.WOODCUTTING_GILDED:
            case AnimationID.WOODCUTTING_DRAGON:
            case AnimationID.WOODCUTTING_DRAGON_OR:
            case AnimationID.WOODCUTTING_INFERNAL:
            case AnimationID.WOODCUTTING_3A_AXE:
            case AnimationID.WOODCUTTING_CRYSTAL:
            case AnimationID.WOODCUTTING_TRAILBLAZER:
            case 2876: // Dragon axe (Lumber Up) Special Attack
                return true;
            default:
                return false;
        }
    }
}
