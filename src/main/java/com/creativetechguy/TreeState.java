package com.creativetechguy;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

public class TreeState {
    int ticksLeft;
    int maxTicks;
    GameObject tree;
    HashSet<Player> playersChopping = new HashSet<>();
    boolean isLoaded = true;
    boolean hasUnrenderedPlayersChopping = false;
    boolean haveYouChoppedLog = false;
    List<WorldPoint> points;
    Client client;

    TreeState(GameObject tree, Client client) {
        this.tree = tree;
        this.client = client;
        maxTicks = TreeConfig.getTreeById(tree.getId()).getMaxTicks();
        ticksLeft = maxTicks;
        points = getPoints(tree);
    }

    /**
     * Returns true if ticks remaining changed
     */
    boolean tick() {
        if (!playersChopping.isEmpty() || hasUnrenderedPlayersChopping) {
            if (playersChopping.size() >= 2 || !playersChopping.contains(client.getLocalPlayer()) || haveYouChoppedLog || ticksLeft < maxTicks || hasUnrenderedPlayersChopping) {
                if (ticksLeft == maxTicks) {
                    System.out.println(tree.getWorldLocation() + " - Start time: " + Instant.now());
                }
                ticksLeft--;
                return true;
            }
        } else if (ticksLeft < maxTicks && ticksLeft > 0) {
            ticksLeft++;
            return true;
        }
        return false;
    }

    boolean shouldShowTimer() {
        if (playersChopping.size() > 0) {
            return true;
        }
        return ticksLeft < maxTicks;
    }

    Color getTimerColor() {
        double percent = getTimePercent() * 100;
        if (percent < 15) {
            return Color.RED;
        }
        if (percent < 40) {
            return Color.ORANGE;
        }
        if (percent < 75) {
            return Color.YELLOW;
        }
        return Color.GREEN;
    }

    Double getTimePercent() {
        return Math.max(ticksLeft / (double) maxTicks, 0);
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
