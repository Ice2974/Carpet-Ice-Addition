package com.ice2974.carpeticeaddition.rules;

import carpet.utils.SpawnOverrides;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.world.entity.MobCategory;
//#if MC>=260200
//$$import net.minecraft.world.entity.EntityTypes;
//#else
import net.minecraft.world.entity.EntityType;
//#endif
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
//#if MC<12105
//$$import net.minecraft.util.random.WeightedRandomList;
//#else
import net.minecraft.util.random.WeightedList;
//#endif
//#if MC>=260300
//$$import net.minecraft.util.valueproviders.ConstantInt;
//#endif

import java.util.function.BooleanSupplier;

public final class DrownedOceanRuinSpawns {
    private static volatile boolean registered;

    private DrownedOceanRuinSpawns() {
    }

    public static void register() {
        registered = false;
        BooleanSupplier enabled = () -> registered && CarpetIceAdditionSettings.drownedSpawningInOceanRuins;
//#if MC>=260300
//$$        var spawns = WeightedList.of(new MobSpawnSettings.SpawnerData(EntityTypes.DROWNED, ConstantInt.of(1)));
//#elseif MC>=260200
//$$        var spawns = WeightedList.of(new MobSpawnSettings.SpawnerData(EntityTypes.DROWNED, 1, 1));
//#elseif MC>=12105
        var spawns = WeightedList.of(new MobSpawnSettings.SpawnerData(EntityType.DROWNED, 1, 1));
//#else
//$$        var spawns = WeightedRandomList.create(new MobSpawnSettings.SpawnerData(EntityType.DROWNED, 5, 1, 1));
//#endif
        SpawnOverrides.addOverride(enabled, MobCategory.MONSTER, BuiltinStructures.OCEAN_RUIN_COLD,
                StructureSpawnOverride.BoundingBoxType.PIECE, spawns);
        SpawnOverrides.addOverride(enabled, MobCategory.MONSTER, BuiltinStructures.OCEAN_RUIN_WARM,
                StructureSpawnOverride.BoundingBoxType.PIECE, spawns);
        registered = true;
    }
}
