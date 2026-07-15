package com.ice2974.carpeticeaddition.villagerevents;

public interface VillagerEventState26 {
    void carpetIceAddition$beginConversion(VillagerEventSnapshot26 snapshot);
    void carpetIceAddition$recordConversionSpawn(boolean accepted);
    void carpetIceAddition$recordConversionDiscard();
    boolean carpetIceAddition$finishConversion(boolean returnedEntity);
    boolean carpetIceAddition$convertedDuringDeath();
}
