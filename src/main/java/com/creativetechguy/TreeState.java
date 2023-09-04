package com.creativetechguy;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

public class TreeState {
    int ticksLeft;
    int maxTicks;
    WorldPoint worldPoint;
    String name;
    HashSet<Player> playersChopping = new HashSet<>();
    boolean hasUnrenderedPlayersChopping = false;
    boolean haveYouChoppedLog = false;
    List<WorldPoint> points;
    Client client;

    TreeState(GameObject tree, Client client) {
        this.worldPoint = tree.getWorldLocation();
        this.client = client;
        TreeConfig config = TreeConfig.getTreeById(tree.getId());
        maxTicks = config.getMaxTicks();
        ticksLeft = maxTicks;
        name = config.name();
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
        if (percent < 80) {
            return Color.YELLOW;
        }
        return Color.GREEN;
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
