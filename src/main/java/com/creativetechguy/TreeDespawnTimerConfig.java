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
            description = "How would you like the estimated remaining time on the tree to be displayed?"
    )
    default TimerTypes timerType() {
        return TimerTypes.PIE;
    }
}
