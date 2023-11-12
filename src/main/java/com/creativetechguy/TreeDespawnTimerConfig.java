package com.creativetechguy;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.*;

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

    @ConfigSection(
            name = "Advanced UI Customization",
            description = "Detailed options for customizing timer overlay",
            position = 3,
            closedByDefault = true
    )
    String uiCustomizationSection = "uiCustomizationSection";

    @ConfigItem(
            keyName = "timerColorLow",
            name = "Timer Color Low",
            position = 0,
            description = "Overlay color when the tree is about to despawn.",
            section = uiCustomizationSection
    )
    default Color timerColorLow() {
        return new Color(220, 0, 0);
    }

    @ConfigItem(
            keyName = "timerColorMedium",
            name = "Timer Color Medium",
            position = 1,
            description = "Overlay color when the tree is nearly running out of time.",
            section = uiCustomizationSection
    )
    default Color timerColorMedium() {
        return new Color(230, 160, 0);
    }

    @ConfigItem(
            keyName = "timerColorHigh",
            name = "Timer Color High",
            position = 2,
            description = "Overlay color when the tree is fairly new.",
            section = uiCustomizationSection
    )
    default Color timerColorHigh() {
        return new Color(230, 230, 0);
    }

    @ConfigItem(
            keyName = "timerColorFull",
            name = "Timer Color Full",
            position = 3,
            description = "Overlay color when the tree is extremely new.",
            section = uiCustomizationSection
    )
    default Color timerColorFull() {
        return new Color(0, 255, 0);
    }

    String UI_SIZE_NORMAL = "uiSizeNormal";

    @ConfigItem(
            keyName = UI_SIZE_NORMAL,
            name = "UI Size Normal",
            position = 4,
            description = "Size of the timer.",
            section = uiCustomizationSection
    )
    default int uiSizeNormal() {
        return 16;
    }

    String UI_SIZE_POPULAR = "uiSizePopular";

    @ConfigItem(
            keyName = UI_SIZE_POPULAR,
            name = "UI Size Popular",
            position = 5,
            description = "Size of the timer for a highlighted popular tree.",
            section = uiCustomizationSection
    )
    default int uiSizePopular() {
        return 25;
    }
}
