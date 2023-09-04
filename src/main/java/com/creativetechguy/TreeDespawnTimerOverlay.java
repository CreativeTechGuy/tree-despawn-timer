package com.creativetechguy;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.components.ProgressPieComponent;

import javax.inject.Inject;
import java.awt.*;

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
            Point point = Perspective.localToCanvas(client,
                    lp,
                    client.getPlane());
            if (point == null) {
                continue;
            }
            if (config.timerType() == TimerTypes.PIE) {
                ProgressPieComponent pie = new ProgressPieComponent();
                pie.setPosition(point);
                pie.setBorderColor(treeState.getTimerColor());
                pie.setFill(treeState.getTimerColor());
                pie.setDiameter(15);
                pie.setProgress(treeState.getTimePercent());
                pie.render(graphics);
            } else if (config.timerType() == TimerTypes.TICKS) {
                String text = treeState.getTimeTicks().toString();
                int textWidth = graphics.getFontMetrics().stringWidth(text);
                int textHeight = graphics.getFontMetrics().getAscent();
                OverlayUtil.renderTextLocation(graphics,
                        new Point(point.getX() - textWidth / 2, point.getY() + textHeight / 2),
                        text,
                        treeState.getTimerColor());
            }
        }
        return null;
    }
}
