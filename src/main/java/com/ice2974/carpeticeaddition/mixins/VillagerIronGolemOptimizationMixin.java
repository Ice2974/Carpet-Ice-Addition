//#if MC<260000
package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationAccess;
import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * ironGolemSpawningOptimization 规则（MC 1.21.x）：村民侧名称命中缓存。
 * 命中结果在 setCustomName 时刷新（setter 驱动），NBT 载入等未经过 setter 的路径
 * 由首次读取时的惰性计算兜底；启动否决热路径只读两个布尔字段，不做 Text / getString 解析。
 * 缓存只反映名称形态，与规则开关无关；不写入任何实体 NBT，卸载后无残留。
 */
@Mixin(Villager.class)
public abstract class VillagerIronGolemOptimizationMixin implements IronGolemVillagerOptimizationAccess {

    @Unique
    private boolean carpetIceAddition$ironGolemNameMatch;

    @Unique
    private boolean carpetIceAddition$ironGolemNameMatchComputed;

    @Override
    public boolean carpetIceAddition$isIronGolemOptimizationTarget() {
        if (!this.carpetIceAddition$ironGolemNameMatchComputed) {
            this.carpetIceAddition$refreshIronGolemNameMatch();
        }
        return this.carpetIceAddition$ironGolemNameMatch;
    }

    @Override
    public void carpetIceAddition$refreshIronGolemNameMatch() {
        Component customName = ((Villager) (Object) this).getCustomName();
        this.carpetIceAddition$ironGolemNameMatch = customName != null
                && IronGolemVillagerOptimizer.matchesOptimizedVillagerName(customName.getString());
        this.carpetIceAddition$ironGolemNameMatchComputed = true;
    }
}
//#endif
