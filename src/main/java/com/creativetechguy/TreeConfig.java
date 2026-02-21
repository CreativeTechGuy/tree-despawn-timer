package com.creativetechguy;

import lombok.Getter;
import net.runelite.api.GameObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.runelite.api.gameval.ObjectID;

public enum TreeConfig {
    // Seconds from: https://oldschool.runescape.wiki/w/Woodcutting#Mechanics
	OAK(27, new int[]{ObjectID.POH_SMALL_TREE3_5, ObjectID.OAKTREE, ObjectID.AVIUM_OAK_1}),
	WILLOW(30,
		new int[]{ObjectID.WILLOWTREE, ObjectID.WILLOW_TREE2, ObjectID.WILLOW_TREE3, ObjectID.WILLOW_TREE4}),
	TEAK(30, new int[]{ObjectID.TEAKTREE, ObjectID.PRIF_TEAKTREE, ObjectID.TEAKTREE_UPDATE}),
	MAPLE(60, new int[]{ObjectID.MAPLETREE, ObjectID.PRIF_MAPLETREE, ObjectID.MAPLETREE_UPDATE}),
	HOLLOW(36, new int[]{ObjectID.HOLLOW_TREE, ObjectID.HOLLOW_TREE_BIG}),
	MAHOGANY(60,
		new int[]{ObjectID.MAHOGANYTREE, ObjectID.PRIF_MAHOGANYTREE, ObjectID.MAHOGANYTREE_UPDATE}),
	ARCTIC_PINE(60 + 24, new int[]{ObjectID.ARCTIC_PINE}),
	YEW(60 + 54,
		new int[]{ObjectID.YEWTREE, ObjectID.DEADMAN_YEWTREE,
			// 10828 = Lumbridge Graveyard tree
			ObjectID.DEADMAN_YEWTREE_INSURANCESTALL, ObjectID.PRIF_YEWTREE, ObjectID.YEWTREE_UPDATE, ObjectID.TREE_YEW_DEFAULT01}),
	CAMPHOR(60 + 54, new int[]{ObjectID.CAMPHOR_TREE}),
	MAGIC(60 * 3 + 54, new int[]{ObjectID.MAGICTREE, ObjectID.DEADMAN_MAGICTREE}),
	IRONWOOD(60 * 3 + 54, new int[]{ObjectID.IRONWOOD_TREE}),
	REDWOOD(60 * 4 + 24,
		new int[]{ObjectID.REDWOODTREE_L, ObjectID.REDWOODTREE_R}),
	ROSEWOOD(60 * 4 + 24, new int[]{ObjectID.ROSEWOOD_TREE});


    private static final ArrayList<Integer> blockedRegions = new ArrayList<>(List.of(
            // Miscellania
            10044,
            // Etcetria
            10300
    ));

    @Getter
    private final int maxTicks;
    private final int[] treeIds;
    private static final HashMap<Integer, TreeConfig> treeMap = new HashMap<>();

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
        if (blockedRegions.contains(gameObject.getWorldLocation().getRegionID())) {
            return false;
        }
        return treeMap.containsKey(gameObject.getId());
    }
}
