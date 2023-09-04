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
import java.time.Instant;
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

    Pattern WOOD_CUT_PATTERN = Pattern.compile("You get (?:some|an)[\\w ]+(?:logs?|mushrooms)\\.");
    HashMap<WorldPoint, TreeState> treeStates = new HashMap<>();
    HashSet<TreeState> uniqueTrees = new HashSet<>();
    HashMap<Player, TreeState> playerTreeChopping = new HashMap<>();
    HashMap<Player, Integer> newlySpawnedPlayers = new HashMap<>();
    boolean firstTick = true;

    @Provides
    TreeDespawnTimerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(TreeDespawnTimerConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(treeDespawnTimerOverlay);
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(treeDespawnTimerOverlay);
        treeStates.clear();
        uniqueTrees.clear();
        playerTreeChopping.clear();
        firstTick = true;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOADING) {
            treeStates.clear();
            uniqueTrees.clear();
            playerTreeChopping.clear();
            firstTick = true;
        }
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        firstTick = false;
        ArrayList<TreeState> toDelete = new ArrayList<>();
        uniqueTrees.forEach(entry -> {
            if (!entry.tick() && !entry.isLoaded) {
                toDelete.add(entry);
            }
        });
        toDelete.forEach(this::deleteTree);
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
            treeState.points.forEach(point -> treeStates.put(point, treeState));
            uniqueTrees.add(treeState);
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        GameObject gameObject = event.getGameObject();
        if (!TreeConfig.isTree(gameObject)) {
            return;
        }
        TreeState treeState = treeStates.get(gameObject.getWorldLocation());
        if (treeState == null) {
            return;
        }
        System.out.println(treeState.tree.getWorldLocation() + " - Tree despawned with " + treeState.ticksLeft + " / " + treeState.maxTicks + " ticks remaining at " + Instant.now());
        deleteTree(treeState);
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        if (firstTick) {
            return;
        }
        if (event.getActor() instanceof Player) {
            Player player = (Player) event.getActor();
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
                System.out.println("Interacting Tree: " + interactingTree.tree.getWorldLocation() + " p:" + interactingTree.playersChopping.size() + " up:" + interactingTree.hasUnrenderedPlayersChopping);
                if (isNewPlayer) {
                    System.out.println("New player ticks:" + newlySpawnedPlayers.get(player));
                }
                if (isNewPlayer && !interactingTree.hasUnrenderedPlayersChopping && interactingTree.playersChopping.isEmpty()) {
                    System.out.println("FRESH player is already woodcutting");
                    deleteTree(interactingTree);
                } else {
                    interactingTree.playersChopping.add(player);
                    playerTreeChopping.put(player, interactingTree);
                }
            }

        }
    }

    @Subscribe
    public void onPlayerSpawned(PlayerSpawned event) {
        Player player = event.getPlayer();
        newlySpawnedPlayers.put(player, 6);
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
                && event.getType() != ChatMessageType.GAMEMESSAGE
                && event.getType() != ChatMessageType.MESBOX) {
            return;
        }
        if (WOOD_CUT_PATTERN.matcher(event.getMessage()).matches()) {
            TreeState interactingTree = playerTreeChopping.get(client.getLocalPlayer());
            if (interactingTree != null) {
                interactingTree.haveYouChoppedLog = true;
            }
        }
    }

    void deleteTree(TreeState treeState) {
        treeState.playersChopping.forEach(player -> playerTreeChopping.remove(player));
        treeState.points.forEach(point -> treeStates.remove(point));
        uniqueTrees.remove(treeState);
    }

    @Nullable
    TreeState findClosetFacingTree(Player player) {
        WorldPoint actorLocation = player.getWorldLocation();
        Direction direction = new Angle(player.getOrientation()).getNearestDirection();
        WorldPoint facingPoint = neighborPoint(actorLocation, direction);
        return treeStates.get(facingPoint);
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
                return true;
            default:
                return false;
        }
    }

    private void listTrees() {
        uniqueTrees.forEach(t -> {
            log.debug("Tree: Pos:" + t.tree.getWorldLocation().toString() + "P: " + t.playersChopping.size());
        });
    }
}
