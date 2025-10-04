package carpet_ybs_extension;

import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.literal;

public class YBSAdditionCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("ybs").
                then(literal("settings").
                        executes( (c)-> listSettings(c.getSource()))));
    }

    private static int listSettings(CommandSourceStack source) {
        Messenger.m(source, "w YBS Addition 设置:");
        Messenger.m(source, "r 总控开关: " + YBSOwnSettings.ybsMasterSwitch + " (关闭状态下所有子功能均无效)");
        Messenger.m(source, "w 通过/carpet命令管理的设置:");
        Messenger.m(source, "w  - 村民识别床距离: " + YBSSimpleSettings.villagerBedSearchDistance);
        Messenger.m(source, "w  - 村民只能乘坐矿车: " + YBSSimpleSettings.VilliagerCanOnlyRideMinecart);
        Messenger.m(source, "w  - 优化铁傀儡生成: " + YBSSimpleSettings.optimizedIronGolemSpawning);
        Messenger.m(source, "w  - 只保留基本村民功能: " + YBSSimpleSettings.villagerBasicFunctionOnly);
        Messenger.m(source, "w 独立管理的设置:");
        Messenger.m(source, "w  - 村民AI优化: " + YBSOwnSettings.VilliagerBrainCut);
        Messenger.m(source, "w  - 禁用村民移动功能: " + YBSOwnSettings.disableVillagerMovement);
        Messenger.m(source, "w  - 猪灵(piglin)及僵尸猪灵(zombified_piglin)特性阉割: " + YBSOwnSettings.mobPiglinNeutralizer);
        Messenger.m(source, "w  - 调试模式: " + YBSOwnSettings.debugMode);
        
        // 添加中文提示和注意事项
        Messenger.m(source, "g 当前村民AI优化特性已" + (YBSOwnSettings.VilliagerBrainCut ? "开启" : "关闭"));
        if (YBSOwnSettings.VilliagerBrainCut) {
            Messenger.m(source, "g 已修改的特性：优化村民寻路算法、简化村民决策逻辑、减少村民行为计算消耗");
            Messenger.m(source, "r 注意：开启此特性后，村民交易功能和袭击相关机制将无法使用");
        } else {
            Messenger.m(source, "g 当前特性：村民保持原版行为，可正常进行交易和参与袭击事件");
        };
        
        // 基本村民功能模式提示
        if (YBSSimpleSettings.villagerBasicFunctionOnly) {
            Messenger.m(source, "y 除上下矿车、优化铁傀儡生成、识别床距离、村民随机游走功能保留外，其余功能将暂时不可用（除用烈焰棒绑定村民与床的规则）");
        } else {
            Messenger.m(source, "g 村民功能未受限，所有功能均可正常使用");
        };
        
        // 猪灵(piglin)及僵尸猪灵(zombified_piglin)特性阉割模式提示
        if (YBSOwnSettings.mobPiglinNeutralizer) {
            Messenger.m(source, "g 猪灵及僵尸猪灵特性已阉割");
            Messenger.m(source, "g 已修改的特性：移除对玩家的敌意、禁用攻击行为、移除愤怒机制、禁用交易行为");
            Messenger.m(source, "g 仅保留：猪灵对海龟蛋(turtle_egg)的寻路功能");
        } else {
            Messenger.m(source, "g 猪灵及僵尸猪灵保持原版行为");
        };
        return 1;
    }
}