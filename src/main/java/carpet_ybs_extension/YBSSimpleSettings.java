package carpet_ybs_extension;

import carpet.settings.Rule;


/**
 * 村民相关的设置类
 * 这些设置将被/carpet命令处理
 */
public class YBSSimpleSettings {
    @Rule(
        desc = "村民识别床的距离",
        category = {"mobs", "ybs"}
    )
    public static int villagerBedSearchDistance = 48;
    
    @Rule(
        desc = "限制村民只能乘坐矿车",
        category = {"mobs", "ybs"}
    )
    public static boolean VilliagerCanOnlyRideMinecart = false;
    
    @Rule(
        desc = "优化村民被恐吓后铁傀儡生成的条件",
        category = {"mobs", "ybs"}
    )
    public static boolean optimizedIronGolemSpawning = false;
    
    @Rule(
        desc = "允许玩家手持烈焰棒蹲下右键村民时，将村民绑定到玩家面前的单一床，且一个床只对应一个村民，绑定后其他村民无法在该床上睡眠。手持空物品栏蹲下右键村民可解除绑定。",
        category = {"mobs", "ybs"}
    )
    public static boolean allowForcedBedBinding = false;
    
    /**
     * 只保留基本村民功能模式 - 该设置已设为默认开启且无法关闭
     * 仅保留本模组提到的功能：上下矿车、优化铁傀儡生成、识别床距离、村民随机游走功能、烈焰棒绑定村民与床
     */
    public static final boolean villagerBasicFunctionOnly = true;
    
    @Rule(
        desc = "控制猪灵及僵尸猪灵的移动，当开启时，猪灵及僵尸猪灵将被移除移动功能；当关闭时，猪灵及僵尸猪灵可以正常移动",
        category = {"mobs", "ybs"}
    )
    public static boolean mobPiglinNeutralizer = false;
}