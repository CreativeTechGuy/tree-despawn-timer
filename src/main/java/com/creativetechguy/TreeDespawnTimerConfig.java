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
            description = "The UI style for displaying the estimated remaining time on each tree."
    )
    default TimerTypes timerType() {
        return TimerTypes.PIE;
    }

    @ConfigItem(
            keyName = "highlightPopularTrees",
            name = "Popular Tree Indicator",
            position = 2,
            description = "Increases the size of the timer when there are 10+ players chopping the tree."
    )
    default boolean highlightPopularTrees() {
        return true;
    }
}
