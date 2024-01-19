package com.creativetechguy;

public enum DebugLevel {
    NONE(0),
    BASIC(1),
    VERBOSE(2),
    SILLY(3);
    private final int value;

    private DebugLevel(int value) {
        this.value = value;
    }

    public boolean shouldShow(DebugLevel userLevel) {
        return userLevel.value >= this.value;
    }
}
