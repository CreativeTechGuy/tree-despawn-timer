package com.creativetechguy;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.Angle;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Getter
    private int subTick = 0;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> subTickFuture;
    private final HashMap<WorldPoint, TreeState> treeAtLocation = new HashMap<>();
    protected HashSet<TreeState> uniqueTrees = new HashSet<>();
    private final HashMap<Player, TreeState> playerTreeChopping = new HashMap<>();
    private final HashMap<Player, Integer> newlySpawnedPlayers = new HashMap<>();
    private final HashMap<Player, WorldPoint> playerSpawnLocation = new HashMap<>();
    private final ArrayList<Runnable> deferTickQueue = new ArrayList<>();
    private final HashMap<Player, Integer> playerRecentlySpeced = new HashMap<>();
    private int nextGarbageCollect = 25;
    private int localPlayerRecentlyClimbedRedwood = 0;
    private int currentPlayerPlane = 0;
    private final int playerSpawnedTicksMax = 8;
    private final int playerSpecTicksMax = 8;
    private int nextAnimationRecheck = 0;
    private final int animationRecheckTicks = 4;

    @Provides
    TreeDespawnTimerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(TreeDespawnTimerConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        CustomTextComponent.updateFontSizes(config.uiSizeNormal(), config.uiSizePopular());
        overlayManager.add(treeDespawnTimerOverlay);
        deferTickQueue.add(() -> {
            client.getPlayers().forEach(player -> {
                onPlayerSpawned(new PlayerSpawned(player));
                handlePlayerChopping(player);
            });
        });
        subTickFuture = executor.scheduleAtFixedRate(
                () -> {
                    subTick += Constants.CLIENT_TICK_LENGTH;
                },
                0,
                Constants.CLIENT_TICK_LENGTH,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(treeDespawnTimerOverlay);
        treeAtLocation.clear();
        uniqueTrees.clear();
        playerTreeChopping.clear();
        newlySpawnedPlayers.clear();
        playerSpawnLocation.clear();
        deferTickQueue.clear();
        playerRecentlySpeced.clear();
        subTickFuture.cancel(false);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals(TreeDespawnTimerConfig.GROUP_NAME)) {
            return;
        }
        if (event.getKey().equals(TreeDespawnTimerConfig.UI_SIZE_NORMAL) || event.getKey()
                .equals(TreeDespawnTimerConfig.UI_SIZE_POPULAR)) {
            CustomTextComponent.updateFontSizes(config.uiSizeNormal(), config.uiSizePopular());
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.HOPPING || event.getGameState() == GameState.LOGGING_IN) {
            treeAtLocation.clear();
            uniqueTrees.clear();
            playerTreeChopping.clear();
            newlySpawnedPlayers.clear();
            playerSpawnLocation.clear();
            deferTickQueue.clear();
            playerRecentlySpeced.clear();
            localPlayerRecentlyClimbedRedwood = playerSpawnedTicksMax;
        }
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        computePlayerPlaneChange();
        if (localPlayerRecentlyClimbedRedwood > 0) {
            localPlayerRecentlyClimbedRedwood--;
        }
        deferTickQueue.forEach(Runnable::run);
        deferTickQueue.clear();

        uniqueTrees.forEach(TreeState::tick);
        nextGarbageCollect--;
        if (nextGarbageCollect <= 0) {
            ArrayList<TreeState> toDelete = new ArrayList<>();
            nextGarbageCollect = 25; // 15 seconds
            uniqueTrees.forEach(tree -> {
                // Cleanup untouched trees far away from the player
                boolean isFarAway = tree.worldPoint.getPlane() == client.getLocalPlayer()
                        .getWorldLocation()
                        .getPlane() && tree.worldPoint.distanceTo(client.getLocalPlayer()
                        .getWorldLocation()) > 150;
                if ((isFarAway && !tree.shouldShowTimer(DebugLevel.NONE)) || (tree.getTimeTicks() <= 0 && tree.playersChopping.isEmpty())) {
                    toDelete.add(tree);
                }
            });
            toDelete.forEach(this::deleteTree);
        }
        // Chopping animations frequently face the wrong direction initially, this rechecks the animation state every few seconds
        nextAnimationRecheck--;
        if (nextAnimationRecheck <= 0) {
            nextAnimationRecheck = animationRecheckTicks;
            client.getPlayers().forEach(this::handlePlayerChopping);
        }
        newlySpawnedPlayers.entrySet().removeIf(p -> {
            p.setValue(p.getValue() - 1);
            if (p.getValue() <= 0) {
                playerSpawnLocation.remove(p.getKey());
            }
            return p.getValue() <= 0;
        });
        playerRecentlySpeced.entrySet().removeIf(p -> {
            p.setValue(p.getValue() - 1);
            return p.getValue() <= 0;
        });
        subTick = 0;
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        GameObject gameObject = event.getGameObject();
        if (TreeConfig.isTree(gameObject)) {
            TreeState treeState = new TreeState(gameObject, client, config);
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
        if (DebugLevel.VERBOSE.shouldShow(config.debugLevel())) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE,
                    "TDT DEBUG",
                    treeState.treeName.toLowerCase() + " despawned. P:" + treeState.playersChopping.size() + ", UPC:" + treeState.unrenderedPlayersChopping.size() + "," + (treeState.haveYouChoppedLog ? " HYCL " : " ") + treeState.getTimeSeconds(
                            getSubTick()) + "s remaining" + (treeState.shouldShowTimer(DebugLevel.NONE) ? "" : " (hidden)"),
                    "");
        }
        deleteTree(treeState);
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (!DebugLevel.SILLY.shouldShow(config.debugLevel())) {
            return;
        }
        if (!event.getMenuOption().equals("Examine")) {
            return;
        }
        Tile tile = client.getSelectedSceneTile();
        assert tile != null;
        GameObject[] gameObjects = Optional.ofNullable(tile.getGameObjects()).orElse(new GameObject[]{});
        TreeState treeState = null;
        for (GameObject gameObject : gameObjects) {
            if (gameObject != null && treeAtLocation.containsKey(gameObject.getWorldLocation())) {
                treeState = treeAtLocation.get(gameObject.getWorldLocation());
            }
        }
        if (treeState != null && (treeState.playersChopping.size() > 0 || treeState.unrenderedPlayersChopping.size() > 0)) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE,
                    "TDT DEBUG",
                    "P: " + treeState.playersChopping.stream()
                            .map(Actor::getName)
                            .collect(Collectors.joining(", ")) + " UPC: " + treeState.unrenderedPlayersChopping.stream()
                            .map(Actor::getName)
                            .collect(Collectors.joining(", ")),
                    "");
        }
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
        newlySpawnedPlayers.put(player, playerSpawnedTicksMax);
        playerSpawnLocation.put(player, player.getWorldLocation());
    }

    @Subscribe
    public void onPlayerDespawned(PlayerDespawned event) {
        Player player = event.getPlayer();
        if (playerTreeChopping.containsKey(player)) {
            TreeState treeState = playerTreeChopping.get(player);
            if (treeState.treeName.equals(TreeConfig.REDWOOD.name())) {
                computePlayerPlaneChange();
            }

            if (!treeState.treeName.equals(TreeConfig.REDWOOD.name()) || hasLocalPlayerRecentlyClimbedRedwood()) {
                treeState.unrenderedPlayersChopping.add(player);
            }
            treeState.playersChopping.remove(player);
            playerTreeChopping.remove(player);
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event) {
        if (event.getSkill() == Skill.WOODCUTTING) {
            TreeState interactingTree = playerTreeChopping.get(client.getLocalPlayer());
            if (interactingTree != null) {
                interactingTree.haveYouChoppedLog = true;
            }
        }
    }

    void handlePlayerChopping(Player player) {
        // If the player has moved since they spawned, they weren't already chopping
        if (hasMoved(player)) {
            newlySpawnedPlayers.remove(player);
            playerSpawnLocation.remove(player);
        }
        boolean isNewPlayer = newlySpawnedPlayers.containsKey(player);
        TreeState previousTree = playerTreeChopping.get(player);
        TreeState newTree = null;
        if (isWoodcutting(player)) {
            TreeState interactingTree = findClosetFacingTree(player);
            if (interactingTree == null) {
                return;
            }
            newTree = interactingTree;
            // A player spawned in and nearly immediately started chopping, assume they've been chopping for a while
            if (isNewPlayer && !interactingTree.hasUnrenderedPlayersChopping() && interactingTree.playersChopping.isEmpty()) {
                if (interactingTree.treeName.equals(TreeConfig.REDWOOD.name())) {
                    // Local player just climbed a redwood tree, so assume they've been chopping for a while
                    if (hasLocalPlayerRecentlyClimbedRedwood()) {
                        interactingTree.hideTree = true;
                    }
                    // If the local player has already been at redwoods and a new player shows up,
                    // ignore that they are a new player since that player likely just climbed the ladder too.
                } else {
                    interactingTree.hideTree = true;
                }
            }
            interactingTree.playersChopping.add(player);
            playerTreeChopping.put(player, interactingTree);
            interactingTree.unrenderedPlayersChopping.removeIf((p) -> Objects.equals(p.getName(), player.getName()));
        }
        if (previousTree != newTree && previousTree != null) {
            previousTree.playersChopping.remove(player);
            if (newTree == null) {
                playerTreeChopping.remove(player);
            }
        }
    }

    void deleteTree(TreeState treeState) {
        treeState.playersChopping.forEach(playerTreeChopping::remove);
        treeState.points.forEach(treeAtLocation::remove);
        uniqueTrees.remove(treeState);
    }

    void computePlayerPlaneChange() {
        int newPlane = client.getLocalPlayer().getWorldLocation().getPlane();
        if (newPlane != currentPlayerPlane) {
            currentPlayerPlane = newPlane;
            localPlayerRecentlyClimbedRedwood = playerSpawnedTicksMax;
        }
    }

    boolean hasLocalPlayerRecentlyClimbedRedwood() {
        return localPlayerRecentlyClimbedRedwood > 0;
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

    // https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/woodcutting/WoodcuttingPlugin.java#L103
    private boolean isWoodcutting(Player player) {
        if (playerRecentlySpeced.containsKey(player)) {
            return true;
        }
        switch (player.getAnimation()) {
            // 1H Axes
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
            case 11939: // Trailblazer Reloaded Infernal Axe variant
            case AnimationID.WOODCUTTING_3A_AXE:
            case AnimationID.WOODCUTTING_CRYSTAL:
            case AnimationID.WOODCUTTING_TRAILBLAZER:
                // 2H Axes
            case AnimationID.WOODCUTTING_2H_BRONZE:
            case AnimationID.WOODCUTTING_2H_IRON:
            case AnimationID.WOODCUTTING_2H_STEEL:
            case AnimationID.WOODCUTTING_2H_BLACK:
            case AnimationID.WOODCUTTING_2H_MITHRIL:
            case AnimationID.WOODCUTTING_2H_ADAMANT:
            case AnimationID.WOODCUTTING_2H_RUNE:
            case AnimationID.WOODCUTTING_2H_DRAGON:
            case AnimationID.WOODCUTTING_2H_CRYSTAL:
            case AnimationID.WOODCUTTING_2H_CRYSTAL_INACTIVE:
            case AnimationID.WOODCUTTING_2H_3A:
                return true;
            // Special Attack
            case 2876: // (Lumber Up) Special Attack
                playerRecentlySpeced.put(player, playerSpecTicksMax);
                return true;
            default:
                return false;
        }
    }

    private boolean hasMoved(Player player) {
        WorldPoint spawnLocation = playerSpawnLocation.get(player);
        if (spawnLocation == null) {
            return false;
        }
        return !player.getWorldLocation().equals(spawnLocation);
    }
}
