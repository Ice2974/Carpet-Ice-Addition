package com.ice2974.carpeticeaddition.villagerevents;

/** State is mixed into the source villager; it is never kept in a global conversion variable. */
public interface VillagerEventState {
    void carpetIceAddition$beginDeath(VillagerEventSnapshot121 snapshot);
    VillagerEventSnapshot121 carpetIceAddition$deathSnapshot();
    void carpetIceAddition$beginConversion(VillagerEventSnapshot121 snapshot);
    VillagerEventSnapshot121 carpetIceAddition$conversionSnapshot();
    boolean carpetIceAddition$conversionActive();
    void carpetIceAddition$recordConversionSpawn(boolean accepted);
    void carpetIceAddition$recordConversionDiscard();
    boolean carpetIceAddition$finishConversion(boolean returnedEntity);
    void carpetIceAddition$clearVillagerEventState();
}
