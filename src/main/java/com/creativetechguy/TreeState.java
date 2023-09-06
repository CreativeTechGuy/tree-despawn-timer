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
    List<WorldPoint> points;
    boolean hasUnrenderedPlayersChopping = false;
    boolean haveYouChoppedLog = false;
    boolean hideTree = false;

    private int ticksLeft;
    private final int maxTicks;
    private final Client client;


    public TreeState(GameObject tree, Client client) {
        worldPoint = tree.getWorldLocation();
        this.client = client;
        TreeConfig config = TreeConfig.getTreeById(tree.getId());
        maxTicks = config.getMaxTicks();
        ticksLeft = maxTicks;
        centerOffset = getCenterOffset(tree);
        points = getPoints(tree);
    }

    void tick() {
        if (!playersChopping.isEmpty() || hasUnrenderedPlayersChopping) {
            if (playersChopping.size() >= 2 || !playersChopping.contains(client.getLocalPlayer()) || haveYouChoppedLog || hasUnrenderedPlayersChopping) {
                if (ticksLeft > 0) {
                    ticksLeft--;
                }
            }
        } else if (ticksLeft < maxTicks) {
            ticksLeft++;
        }
    }

    boolean shouldShowTimer() {
        if (hideTree) {
            return false;
        }
        if (playersChopping.size() > 0) {
            return true;
        }
        return ticksLeft < maxTicks;
    }

    Color getTimerColor() {
        double percent = getTimePercent() * 100;
        if (percent < 15) {
            return new Color(220, 0, 0);
        }
        if (percent < 40) {
            return new Color(230, 160, 0);
        }
        if (percent < 80) {
            return new Color(230, 230, 0);
        }
        return new Color(0, 255, 0);
    }

    Float getTimePercent() {
        return Math.max(ticksLeft / (float) maxTicks, 0);
    }

    Integer getTimeTicks() {
        return Math.max(ticksLeft, 0);
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
}
