package com.creativetechguy;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(TreeDespawnTimerConfig.GROUP_NAME)
public interface TreeDespawnTimerConfig extends Config {
    String GROUP_NAME = "tree-despawn-timer";

    @ConfigItem(
            keyName = "timerType",
            name = "Timer Display Type",
            position = 1,
            description = "How would you like the estimated remaining time to be displayed?"
    )
    default TimerTypes timerType() {
        return TimerTypes.PIE;
    }

    @ConfigItem(
            keyName = "highlightPopularTrees",
            name = "Highlight Popular Trees",
            position = 2,
            description = "Should the timer be emphasized when 10+ players are chopping a tree?"
    )
    default boolean highlightPopularTrees() {
        return true;
    }
}
