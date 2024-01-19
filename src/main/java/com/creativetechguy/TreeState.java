package com.creativetechguy;

import net.runelite.api.Point;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

public class TreeState {

    WorldPoint worldPoint;
    Point centerOffset;
    HashSet<Player> playersChopping = new HashSet<>();
    HashSet<Player> unrenderedPlayersChopping = new HashSet<>();
    List<WorldPoint> points;
    String treeName;
    boolean haveYouChoppedLog = false;
    boolean hideTree = false;

    private int ticksLeft;
    private final int maxTicks;
    private final Client client;
    private final TreeDespawnTimerConfig config;

    public TreeState(GameObject tree, Client client, TreeDespawnTimerConfig config) {
        worldPoint = tree.getWorldLocation();
        this.client = client;
        this.config = config;
        TreeConfig treeConfig = TreeConfig.getTreeById(tree.getId());
        treeName = treeConfig.name();
        maxTicks = treeConfig.getMaxTicks();
        ticksLeft = maxTicks;
        centerOffset = getCenterOffset(tree);
        points = getPoints(tree);
    }

    boolean hasUnrenderedPlayersChopping() {
        return !unrenderedPlayersChopping.isEmpty();
    }

    int getTickDelta() {
        if (!playersChopping.isEmpty() || hasUnrenderedPlayersChopping()) {
            if (playersChopping.size() >= 2 || !playersChopping.contains(client.getLocalPlayer()) || haveYouChoppedLog || hasUnrenderedPlayersChopping()) {
                return -1;
            }
        } else if (ticksLeft < maxTicks) {
            return 1;
        } else if (haveYouChoppedLog && ticksLeft == maxTicks) {
            haveYouChoppedLog = false;
        }
        return 0;
    }

    void tick() {
        ticksLeft += getTickDelta();
    }

    boolean shouldShowTimer(DebugLevel debugLevel) {
        if (DebugLevel.VERBOSE.shouldShow(debugLevel)) {
            return true;
        }
        if (DebugLevel.BASIC.shouldShow(debugLevel) && ticksLeft < maxTicks) {
            return true;
        }
        if (hideTree) {
            return false;
        }
        if (playersChopping.size() > 0) {
            return true;
        }
        return ticksLeft < maxTicks;
    }

    boolean shouldShowTimer() {
        return shouldShowTimer(config.debugLevel());
    }

    Color getTimerColor() {
        // This is used for debugging only
        if (hideTree) {
            return Color.GRAY;
        }
        double percent = getTimePercent() * 100;
        if (percent < 15) {
            return config.timerColorLow();
        }
        if (percent < 40) {
            return config.timerColorMedium();
        }
        if (percent < 80) {
            return config.timerColorHigh();
        }
        return config.timerColorFull();
    }

    Float getTimePercent() {
        return Math.max(ticksLeft / (float) maxTicks, 0);
    }

    Integer getTimeTicks() {
        if (DebugLevel.VERBOSE.shouldShow(config.debugLevel())) {
            return ticksLeft;
        }
        return Math.max(ticksLeft, 0);
    }

    Integer getTimeSeconds(int subTickMs) {
        int secondsLeft = (int) Math.floor((ticksLeft * Constants.GAME_TICK_LENGTH + subTickMs * getTickDelta()) / 1000f);
        if (DebugLevel.VERBOSE.shouldShow(config.debugLevel())) {
            return secondsLeft;
        }
        return Math.max(secondsLeft, 0);
    }

    private List<WorldPoint> getPoints(GameObject gameObject) {
        WorldPoint minPoint = getSWWorldPoint(gameObject);
        WorldPoint maxPoint = getNEWorldPoint(gameObject);

        if (minPoint.equals(maxPoint)) {
            return Collections.singletonList(minPoint);
        }

        final int plane = minPoint.getPlane();
        final List<WorldPoint> list = new ArrayList<>();
        for (int x = minPoint.getX(); x <= maxPoint.getX(); x++) {
            for (int y = minPoint.getY(); y <= maxPoint.getY(); y++) {
                list.add(new WorldPoint(x, y, plane));
            }
        }
        return list;
    }

    private Point getCenterOffset(GameObject gameObject) {
        int x = 0;
        int y = 0;
        if (gameObject.sizeX() % 2 == 0) {
            x = (gameObject.sizeX() - 1) * Perspective.LOCAL_HALF_TILE_SIZE;
        }
        if (gameObject.sizeY() % 2 == 0) {
            y = (gameObject.sizeY() - 1) * Perspective.LOCAL_HALF_TILE_SIZE;
        }
        return new Point(x, y);
    }

    private WorldPoint getSWWorldPoint(GameObject gameObject) {
        return getWorldPoint(gameObject, GameObject::getSceneMinLocation);
    }

    private WorldPoint getNEWorldPoint(GameObject gameObject) {
        return getWorldPoint(gameObject, GameObject::getSceneMaxLocation);
    }

    private WorldPoint getWorldPoint(GameObject gameObject, Function<GameObject, Point> pointFunction) {
        Point point = pointFunction.apply(gameObject);
        return WorldPoint.fromScene(client, point.getX(), point.getY(), gameObject.getPlane());
    }

    public boolean canShowPopularIndicator() {
        int regionID = worldPoint.getRegionID();
        // Woodcutting guild doesn't use Forestry's tree popularity system
        if (regionID == 6198 || regionID == 6454) {
            return false;
        }
        return true;
    }
}
