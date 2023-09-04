package com.creativetechguy;

import lombok.Getter;
import net.runelite.api.GameObject;
import net.runelite.api.NullObjectID;
import net.runelite.api.ObjectID;

import java.util.HashMap;

public enum TreeConfig {
    // Seconds from: https://oldschool.runescape.wiki/w/Woodcutting#Mechanics
    // Tree Ids from: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/woodcutting/Tree.java
    OAK(27, new int[]{ObjectID.OAK_TREE_4540, ObjectID.OAK_TREE_10820}),
    WILLOW(30,
            new int[]{ObjectID.WILLOW_TREE_10819, ObjectID.WILLOW_TREE_10829, ObjectID.WILLOW_TREE_10831, ObjectID.WILLOW_TREE_10833}),
    TEAK(30, new int[]{ObjectID.TEAK_TREE, ObjectID.TEAK_TREE_36686, ObjectID.TEAK_TREE_40758}),
    MAPLE(60, new int[]{ObjectID.MAPLE_TREE_10832, ObjectID.MAPLE_TREE_36681, ObjectID.MAPLE_TREE_40754}),
    HOLLOW(36, new int[]{ObjectID.HOLLOW_TREE_10821, ObjectID.HOLLOW_TREE_10830}),
    MAHOGANY(60, new int[]{ObjectID.MAHOGANY_TREE, ObjectID.MAHOGANY, ObjectID.MAHOGANY_TREE_40760}),
    ARCTIC_PINE(60 + 24, new int[]{ObjectID.ARCTIC_PINE_TREE}),
    YEW(60 + 54,
            new int[]{ObjectID.YEW_TREE_10822, NullObjectID.NULL_10823, ObjectID.YEW_TREE_36683, ObjectID.YEW_TREE_40756}),
    MAGIC(60 * 3 + 54, new int[]{ObjectID.MAGIC_TREE_10834, NullObjectID.NULL_10835}),
    REDWOOD(60 * 4 + 24,
            new int[]{ObjectID.REDWOOD_TREE, ObjectID.REDWOOD_TREE_29670, NullObjectID.NULL_34633, NullObjectID.NULL_34635, NullObjectID.NULL_34637, NullObjectID.NULL_34639, ObjectID.REDWOOD_TREE_34284, ObjectID.REDWOOD_TREE_34286, ObjectID.REDWOOD_TREE_34288, ObjectID.REDWOOD_TREE_34290});


    @Getter
    private int maxTicks;
    private int[] treeIds;
    private static HashMap<Integer, TreeConfig> treeMap = new HashMap<>();

    static {
        for (TreeConfig treeConfig : values()) {
            for (int treeId : treeConfig.treeIds) {
                treeMap.put(treeId, treeConfig);
            }
        }
    }

    TreeConfig(int maxSeconds, int[] treeIds) {
        this.maxTicks = (int) Math.round(maxSeconds / 0.6d);
        this.treeIds = treeIds;
    }

    static TreeConfig getTreeById(int gameObjectId) {
        return treeMap.get(gameObjectId);
    }

    static boolean isTree(GameObject gameObject) {
        return treeMap.containsKey(gameObject.getId());
    }

}
