//#if MC<260000
package com.ice2974.carpeticeaddition.villagerevents;

/** State is mixed into the source villager; it is never kept in a global conversion variable. */
public interface VillagerEventState {
    void carpetIceAddition$beginDeath(VillagerEventSnapshot snapshot);
    VillagerEventSnapshot carpetIceAddition$deathSnapshot();
    void carpetIceAddition$beginConversion(VillagerEventSnapshot snapshot);
    VillagerEventSnapshot carpetIceAddition$conversionSnapshot();
    boolean carpetIceAddition$conversionActive();
    void carpetIceAddition$recordConversionSpawn(boolean accepted);
    void carpetIceAddition$recordConversionDiscard();
    boolean carpetIceAddition$finishConversion(boolean returnedEntity);
    void carpetIceAddition$abortConversion();
    void carpetIceAddition$clearVillagerEventState();
}
//#endif
