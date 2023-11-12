package com.creativetechguy;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ProgressPieComponent;

import javax.inject.Inject;
import java.awt.*;
import java.time.Duration;

public class TreeDespawnTimerOverlay extends Overlay {

    private final TreeDespawnTimerPlugin plugin;
    private final TreeDespawnTimerConfig config;
    private final Client client;

    @Inject
    private TreeDespawnTimerOverlay(TreeDespawnTimerPlugin plugin, TreeDespawnTimerConfig config, Client client) {
        this.plugin = plugin;
        this.config = config;
        this.client = client;
        setLayer(OverlayLayer.UNDER_WIDGETS);
        setPosition(OverlayPosition.DYNAMIC);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        for (TreeState treeState : plugin.uniqueTrees) {
            if (!treeState.shouldShowTimer()) {
                continue;
            }
            LocalPoint lp = LocalPoint.fromWorld(client, treeState.worldPoint);
            if (lp == null) {
                continue;
            }
            LocalPoint centeredPoint = new LocalPoint(lp.getX() + treeState.centerOffset.getX(),
                    lp.getY() + treeState.centerOffset.getY());
            Point point = Perspective.localToCanvas(client,
                    centeredPoint,
                    client.getPlane());
            if (point == null) {
                continue;
            }
            boolean isPopularTree = config.highlightPopularTrees() && treeState.playersChopping.size() >= 10 && treeState.canShowPopularIndicator();
            if (config.timerType() == TimerTypes.PIE) {
                ProgressPieComponent pie = new ProgressPieComponent();
                pie.setPosition(point);
                pie.setBorderColor(treeState.getTimerColor());
                pie.setDiameter(config.uiSizeNormal());
                if (isPopularTree) {
                    pie.setBorder(Color.BLACK, 2);
                    pie.setDiameter(config.uiSizePopular());
                }
                pie.setFill(treeState.getTimerColor());
                pie.setProgress(treeState.getTimePercent());
                pie.render(graphics);
            } else if (config.timerType() == TimerTypes.TICKS) {
                String text = treeState.getTimeTicks().toString();
                CustomTextComponent textComponent = new CustomTextComponent(text,
                        new java.awt.Point(point.getX(), point.getY()));
                if (isPopularTree) {
                    textComponent.setEmphasize(true);
                }
                textComponent.setColor(treeState.getTimerColor());
                textComponent.render(graphics);
            } else if (config.timerType() == TimerTypes.SECONDS) {
                Duration duration = Duration.ofSeconds(treeState.getTimeSeconds(plugin.getSubTick()));
                String text = String.format("%d:%02d", duration.toMinutesPart(), duration.toSecondsPart());
                CustomTextComponent textComponent = new CustomTextComponent(text,
                        new java.awt.Point(point.getX(), point.getY()));
                if (isPopularTree) {
                    textComponent.setEmphasize(true);
                }
                textComponent.setColor(treeState.getTimerColor());
                textComponent.render(graphics);
            }
        }
        return null;
    }
}
