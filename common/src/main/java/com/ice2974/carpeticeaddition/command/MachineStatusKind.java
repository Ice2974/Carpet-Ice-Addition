package com.ice2974.carpeticeaddition.command;

public enum MachineStatusKind {
    INVALID(0, "invalid"),
    RUNNING(1, "running"),
    STOPPED(2, "stopped"),
    UNLOADED(3, "unloaded");

    private final int sortOrder;
    private final String translationSuffix;

    MachineStatusKind(int sortOrder, String translationSuffix) {
        this.sortOrder = sortOrder;
        this.translationSuffix = translationSuffix;
    }

    public int sortOrder() {
        return sortOrder;
    }

    public String translationKey() {
        return "command.carpet-ice-addition.machine_status.status." + translationSuffix;
    }
}
