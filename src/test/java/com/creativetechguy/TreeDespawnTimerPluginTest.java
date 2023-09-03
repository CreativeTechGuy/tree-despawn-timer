package com.creativetechguy;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TreeDespawnTimerPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(TreeDespawnTimerPlugin.class);
        RuneLite.main(args);
    }
}