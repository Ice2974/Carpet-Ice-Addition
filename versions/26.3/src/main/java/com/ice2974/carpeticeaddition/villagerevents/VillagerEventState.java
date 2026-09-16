package com.ice2974.carpeticeaddition.villagerevents;

public interface VillagerEventState {
    void carpetIceAddition$beginConversion(VillagerEventSnapshot snapshot);
    boolean carpetIceAddition$conversionActive();
    void carpetIceAddition$recordConversionSpawn(boolean accepted);
    void carpetIceAddition$recordConversionDiscard();
    boolean carpetIceAddition$finishConversion(boolean returnedEntity);
    void carpetIceAddition$abortConversion();
    boolean carpetIceAddition$convertedDuringDeath();
    void carpetIceAddition$clearVillagerEventState();
}
