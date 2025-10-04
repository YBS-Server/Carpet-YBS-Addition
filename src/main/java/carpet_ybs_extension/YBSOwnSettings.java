package carpet_ybs_extension;

import carpet.settings.Rule;

/**
 * 这个类包含的设置独立于carpet.conf管理
 */
public class YBSOwnSettings {
    @Rule(
        desc = "YBS扩展总控开关(开启后可使用各子功能，关闭后所有子功能均无效)",
        category = {"ybs", "core"}
    )
    public static boolean ybsMasterSwitch = false;
    
    @Rule(
        desc = "村民AI优化和行为调整(开启后村民不会随机游走，但会禁用交易功能和袭击机制)",
        category = {"villagers", "ybs"}
    )
    public static boolean VilliagerBrainCut = false;
    
    @Rule(
        desc = "禁用村民所有移动功能(默认关闭，需要通过指令开启，开启后会禁用村民随机游走等移动AI，但不会禁用夜间寻床的功能)",
        category = {"villagers", "ybs"}
    )
    public static boolean disableVillagerMovement = false;
    
    @Rule(
        desc = "猪灵(piglin)及僵尸猪灵(zombified_piglin)特性阉割(开启后只保留针对海龟蛋(turtle_egg)的寻路功能，移除其他所有特性)",
        category = {"mobs", "ybs"}
    )
    public static boolean mobPiglinNeutralizer = false;
    
    @Rule(
        desc = "调试模式",
        category = {"debug", "ybs"}
    )
    public static boolean debugMode = false;
}