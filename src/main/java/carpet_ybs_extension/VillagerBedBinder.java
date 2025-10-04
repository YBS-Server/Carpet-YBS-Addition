package carpet_ybs_extension;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 管理村民和床的绑定功能
 * 用于跟踪玩家的绑定状态，实现强制绑定村民到指定床
 */
public class VillagerBedBinder {
    // 存储玩家的当前绑定目标村民
    private static final Map<UUID, UUID> PLAYER_TARGET_VILLAGER = new HashMap<>();
    
    /**
     * 设置玩家的当前绑定目标村民
     * @param player 执行绑定操作的玩家
     * @param villager 目标村民
     */
    public static void setPlayerTargetVillager(ServerPlayer player, Villager villager) {
        if (player != null && villager != null) {
            PLAYER_TARGET_VILLAGER.put(player.getUUID(), villager.getUUID());
        }
    }
    
    /**
     * 获取玩家的当前绑定目标村民
     * @param player 执行绑定操作的玩家
     * @return 目标村民的UUID，如果没有则返回null
     */
    public static UUID getPlayerTargetVillager(ServerPlayer player) {
        if (player != null) {
            return PLAYER_TARGET_VILLAGER.get(player.getUUID());
        }
        return null;
    }
    
    /**
     * 清除玩家的绑定目标村民
     * @param player 执行绑定操作的玩家
     */
    public static void clearPlayerTargetVillager(ServerPlayer player) {
        if (player != null) {
            PLAYER_TARGET_VILLAGER.remove(player.getUUID());
        }
    }
    
    /**
     * 检查是否有活跃的绑定任务
     * @param player 执行绑定操作的玩家
     * @return 如果有活跃的绑定任务则返回true
     */
    public static boolean hasActiveBindingTask(ServerPlayer player) {
        return getPlayerTargetVillager(player) != null;
    }
}