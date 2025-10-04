package carpet_ybs_extension.mixins;

import carpet_ybs_extension.YBSOwnSettings;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Unique;

import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;

@Mixin(Piglin.class)
public class PiglinMixin {
    private static final Logger LOGGER = Logger.getLogger("CarpetYBSAddition");
    
    // 用于跟踪每个猪灵实体的上一次规则状态
    @Unique
    private static final Map<Integer, Boolean> lastMobPiglinNeutralizerState = new HashMap<>();

    // 完全禁用猪灵的物品拾取行为
    @Inject(method = "wantsToPickUp", at = @At("HEAD"), cancellable = true, remap = true)
    private void onWantsToPickUp(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        try {
            // 直接检查具体功能开关，不再依赖总控开关
            if (YBSOwnSettings.mobPiglinNeutralizer) {
                // 完全禁用物品拾取
                cir.setReturnValue(false);
                // 立即禁用所有移动
                clearTarget();
            }
        } catch (Exception e) {
            LOGGER.warning("Error in PiglinMixin.onWantsToPickUp: " + e.getMessage());
        }
    }

    // 添加对aiStep方法的注入，确保mobPiglinNeutralizer规则在所有维度都能持续生效
    @Inject(method = "aiStep", at = @At("HEAD"), remap = true)
    private void onAiStep(CallbackInfo ci) {
        try {
            // 获取实体实例
            Piglin piglin = (Piglin)(Object)this;
            int entityId = piglin.getId();
            boolean currentState = YBSOwnSettings.mobPiglinNeutralizer;
            Boolean lastState = lastMobPiglinNeutralizerState.getOrDefault(entityId, false);
            
            // 更新状态跟踪
            lastMobPiglinNeutralizerState.put(entityId, currentState);
            
            if (currentState) {
                // 规则开启：立即停止所有移动
                piglin.getNavigation().stop();
                piglin.setNoActionTime(1000); // 延长无动作时间
                piglin.setDeltaMovement(0, 0, 0); // 立即停止移动
                disableAllMovement();
            } else if (lastState) {
                // 规则从开启变为关闭：恢复移动能力
                restoreAllMovement();
                // 清除该实体的状态跟踪，避免内存泄漏
                lastMobPiglinNeutralizerState.remove(entityId);
            }
        } catch (Exception e) {
            LOGGER.warning("Error in PiglinMixin.onAiStep: " + e.getMessage());
        }
    }

    // 控制猪灵对海龟蛋(turtle_egg)的感知
    private void handleTurtleEggNavigation() {
        try {
            // 直接检查具体功能开关，不再依赖总控开关
            if (!YBSOwnSettings.mobPiglinNeutralizer) {
                return;
            }
            // 在Mixin中安全地获取实体实例
            Piglin piglin = (Piglin)(Object)this;
            
            // 完全禁用任何形式的导航行为，包括对海龟蛋的导航
            if (piglin.getNavigation().isInProgress()) {
                // 清除所有目标和导航
                piglin.setTarget(null);
                piglin.getNavigation().stop();
                piglin.getMoveControl().strafe(0, 0);
                piglin.setDeltaMovement(0, 0, 0);
                piglin.setNoActionTime(1000); // 延长无动作时间
            }
            
            // 获取当前目标
            Object target = piglin.getTarget();
            
            // 如果有任何目标，立即清除
            if (target != null) {
                piglin.setTarget(null);
                piglin.getNavigation().stop();
                piglin.getMoveControl().strafe(0, 0);
                piglin.setDeltaMovement(0, 0, 0);
                piglin.setNoActionTime(1000); // 延长无动作时间
            }
        } catch (Exception e) {
            LOGGER.warning("Error in PiglinMixin.handleTurtleEggNavigation: " + e.getMessage());
        }
    }

    // 清除猪灵的所有目标和路径（禁用移动）
    private void clearTarget() {
        try {
            // 直接检查具体功能开关，不再依赖总控开关
            if (!YBSOwnSettings.mobPiglinNeutralizer) {
                return;
            }
            Piglin piglin = (Piglin)(Object)this;
            // 清除猪灵的所有目标和路径
            piglin.setTarget(null);
            piglin.getNavigation().stop();
            // 禁用随机移动
            piglin.getMoveControl().strafe(0, 0);
            // 重置移动速度
            piglin.setDeltaMovement(0, 0, 0);
            // 设置较长的无动作时间
            piglin.setNoActionTime(1000); // 延长无动作时间
            // 添加额外的控制措施来彻底禁用移动
            disableAllMovement();
        } catch (Exception e) {
            LOGGER.warning("Error in PiglinMixin.clearTarget: " + e.getMessage());
        }
    }
    
    // 彻底禁用猪灵的所有移动能力（增强版）
    private void disableAllMovement() {
        try {
            // 在Mixin中安全地获取实体实例
            Piglin piglin = (Piglin)(Object)this;
            
            // 增强版移动控制，确保立即彻底禁用移动
            // 阻止AI更新
            piglin.setNoAi(true);
            // 立即停止所有移动
            piglin.setDeltaMovement(0, 0, 0);
            // 禁用导航系统
            piglin.getNavigation().stop();
            // 禁用移动控制
            piglin.getMoveControl().strafe(0, 0);
            // 设置较长的无动作时间，使猪灵长时间保持静止
            piglin.setNoActionTime(2000); // 增加至100秒
            // 重置所有AI目标
            piglin.setAggressive(false);
            // 清除所有可能的寻路目标
            piglin.setTarget(null);
            // 禁用猪灵的物品寻找行为
            piglin.setCanPickUpLoot(false);
            // 额外的安全措施
            piglin.getNavigation().stop(); // 再次停止导航以确保效果
        } catch (Exception e) {
            LOGGER.warning("Error in PiglinMixin.disableAllMovement: " + e.getMessage());
        }
    }
    
    // 恢复猪灵的所有移动能力
    private void restoreAllMovement() {
        try {
            // 在Mixin中安全地获取实体实例
            Piglin piglin = (Piglin)(Object)this;
            
            // 恢复AI控制
            piglin.setNoAi(false);
            // 重置无动作时间，允许立即开始移动
            piglin.setNoActionTime(0);
            // 恢复物品寻找行为
            piglin.setCanPickUpLoot(true);
            // 无需重置目标和导航，让AI自然恢复
        } catch (Exception e) {
            LOGGER.warning("Error in PiglinMixin.restoreAllMovement: " + e.getMessage());
        }
    }
}