package carpet_ybs_extension.mixins;

import carpet_ybs_extension.YBSOwnSettings;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Unique;

import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;

@Mixin(ZombifiedPiglin.class)
public class ZombifiedPiglinMixin {
    private static final Logger LOGGER = Logger.getLogger("CarpetYBSAddition");
    
    // 用于跟踪每个僵尸猪灵实体的上一次规则状态
    @Unique
    private static final Map<Integer, Boolean> lastMobPiglinNeutralizerState = new HashMap<>();

    // 阻止僵尸猪灵设置愤怒目标并控制移动
    @Inject(method = "setPersistentAngerTarget", at = @At("HEAD"), cancellable = true, remap = true)
    private void onSetPersistentAngerTarget(java.util.UUID uuid, CallbackInfo ci) {
        try {
            // 直接检查具体功能开关，不再依赖总控开关
            if (YBSOwnSettings.mobPiglinNeutralizer) {
                // 取消设置愤怒目标
                ci.cancel();
                // 在Mixin中安全地获取实体实例
                ZombifiedPiglin zombifiedPiglin = (ZombifiedPiglin)(Object)this;
                // 清除当前目标
                zombifiedPiglin.setTarget(null);
                // 停止导航
                zombifiedPiglin.getNavigation().stop();
                // 设置无动作时间，确保不会攻击
                zombifiedPiglin.setNoActionTime(1000);
                // 重置移动速度
                zombifiedPiglin.setDeltaMovement(0, 0, 0);
                // 添加额外的控制措施来彻底禁用移动
                disableAllMovement();
            }
        } catch (Exception e) {
            LOGGER.warning("Error in ZombifiedPiglinMixin.onSetPersistentAngerTarget: " + e.getMessage());
        }
    }

    // 添加对aiStep方法的注入，确保mobPiglinNeutralizer规则在所有维度都能持续生效
    @Inject(method = "aiStep", at = @At("HEAD"), remap = true)
    private void onAiStep(CallbackInfo ci) {
        try {
            // 获取实体实例
            ZombifiedPiglin zombifiedPiglin = (ZombifiedPiglin)(Object)this;
            int entityId = zombifiedPiglin.getId();
            boolean currentState = YBSOwnSettings.mobPiglinNeutralizer;
            Boolean lastState = lastMobPiglinNeutralizerState.getOrDefault(entityId, false);
            
            // 更新状态跟踪
            lastMobPiglinNeutralizerState.put(entityId, currentState);
            
            if (currentState) {
                // 规则开启：立即停止所有移动
                zombifiedPiglin.getNavigation().stop();
                zombifiedPiglin.setNoActionTime(1000); // 延长无动作时间
                zombifiedPiglin.setDeltaMovement(0, 0, 0); // 立即停止移动
                handleTurtleEggNavigation();
                disableAllMovement();
            } else if (lastState) {
                // 规则从开启变为关闭：恢复移动能力
                restoreAllMovement();
                // 清除该实体的状态跟踪，避免内存泄漏
                lastMobPiglinNeutralizerState.remove(entityId);
            }
        } catch (Exception e) {
            LOGGER.warning("Error in ZombifiedPiglinMixin.onAiStep: " + e.getMessage());
        }
    }

    // 控制僵尸猪灵对海龟蛋(turtle_egg)的感知
    private void handleTurtleEggNavigation() {
        try {
            // 直接检查具体功能开关，不再依赖总控开关
            if (!YBSOwnSettings.mobPiglinNeutralizer) {
                return;
            }
            // 在Mixin中安全地获取实体实例
            ZombifiedPiglin zombifiedPiglin = (ZombifiedPiglin)(Object)this;
            
            // 完全禁用任何形式的导航行为，包括对海龟蛋的导航
            if (zombifiedPiglin.getNavigation().isInProgress()) {
                // 清除所有目标和导航
                zombifiedPiglin.setTarget(null);
                zombifiedPiglin.getNavigation().stop();
                zombifiedPiglin.getMoveControl().strafe(0, 0);
                zombifiedPiglin.setDeltaMovement(0, 0, 0);
                zombifiedPiglin.setNoActionTime(1000); // 延长无动作时间
            }
            
            // 获取当前目标
            Object target = zombifiedPiglin.getTarget();
            
            // 如果有任何目标，立即清除
            if (target != null) {
                zombifiedPiglin.setTarget(null);
                zombifiedPiglin.getNavigation().stop();
                zombifiedPiglin.getMoveControl().strafe(0, 0);
                zombifiedPiglin.setDeltaMovement(0, 0, 0);
                zombifiedPiglin.setNoActionTime(1000); // 延长无动作时间
            }
        } catch (Exception e) {
            LOGGER.warning("Error in ZombifiedPiglinMixin.handleTurtleEggNavigation: " + e.getMessage());
        }
    }
    
    // 彻底禁用僵尸猪灵的所有移动能力（增强版）
    private void disableAllMovement() {
        try {
            // 在Mixin中安全地获取实体实例
            ZombifiedPiglin zombifiedPiglin = (ZombifiedPiglin)(Object)this;
            
            // 增强版移动控制，确保立即彻底禁用移动
            // 阻止AI更新
            zombifiedPiglin.setNoAi(true);
            // 立即停止所有移动
            zombifiedPiglin.setDeltaMovement(0, 0, 0);
            // 禁用导航系统
            zombifiedPiglin.getNavigation().stop();
            // 禁用移动控制
            zombifiedPiglin.getMoveControl().strafe(0, 0);
            // 设置较长的无动作时间，使僵尸猪灵长时间保持静止
            zombifiedPiglin.setNoActionTime(2000); // 增加至100秒
            // 重置所有AI目标
            zombifiedPiglin.setAggressive(false);
            // 清除所有可能的寻路目标
            zombifiedPiglin.setTarget(null);
            // 禁用僵尸猪灵的物品寻找行为
            zombifiedPiglin.setCanPickUpLoot(false);
            // 额外的安全措施
            zombifiedPiglin.getNavigation().stop(); // 再次停止导航以确保效果
        } catch (Exception e) {
            LOGGER.warning("Error in ZombifiedPiglinMixin.disableAllMovement: " + e.getMessage());
        }
    }
    
    // 恢复僵尸猪灵的所有移动能力
    private void restoreAllMovement() {
        try {
            // 在Mixin中安全地获取实体实例
            ZombifiedPiglin zombifiedPiglin = (ZombifiedPiglin)(Object)this;
            
            // 恢复AI控制
            zombifiedPiglin.setNoAi(false);
            // 重置无动作时间，允许立即开始移动
            zombifiedPiglin.setNoActionTime(0);
            // 恢复物品寻找行为
            zombifiedPiglin.setCanPickUpLoot(true);
            // 无需重置目标和导航，让AI自然恢复
        } catch (Exception e) {
            LOGGER.warning("Error in ZombifiedPiglinMixin.restoreAllMovement: " + e.getMessage());
        }
    }

    // 禁用僵尸猪灵的装备行为并控制移动
    @Inject(method = "populateDefaultEquipmentSlots", at = @At("HEAD"), cancellable = true, remap = true)
    private void onPopulateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, net.minecraft.world.DifficultyInstance difficulty, CallbackInfo ci) {
        try {
            // 直接检查具体功能开关，不再依赖总控开关
            if (YBSOwnSettings.mobPiglinNeutralizer) {
                // 取消装备生成
                ci.cancel();
                // 控制移动行为
                handleTurtleEggNavigation();
            }
        } catch (Exception e) {
            LOGGER.warning("Error in ZombifiedPiglinMixin.onPopulateDefaultEquipmentSlots: " + e.getMessage());
        }
    }
}