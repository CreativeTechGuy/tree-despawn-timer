package com.creativetechguy;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;

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

    @Provides
    TreeDespawnTimerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(TreeDespawnTimerConfig.class);
    }

    @Override
    protected void startUp() throws Exception {

    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {

    }
}
