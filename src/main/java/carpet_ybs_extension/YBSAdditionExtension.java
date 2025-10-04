package carpet_ybs_extension;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.settings.SettingsManager;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class YBSAdditionExtension implements CarpetExtension, ModInitializer {
    public static void noop() { }
    private static SettingsManager mySettingManager;
    
    static {
        mySettingManager = new SettingsManager("1.0.0", "ybs", "Carpet YBS Addition");
    }
    
    @Override
    public void onInitialize() {
        // 初始化模组时注册扩展
        CarpetServer.manageExtension(this);
    }

    @Override
    public void onGameStarted() {
        // 让/carpet命令处理我们的设置
        CarpetServer.settingsManager.parseSettingsClass(YBSSimpleSettings.class);
        // 我们自己的设置类独立于carpet.conf
        mySettingManager.parseSettingsClass(YBSOwnSettings.class);
    }

    @Override
    public void onServerLoaded(MinecraftServer server) {
        // /carpet设置的重载由carpet处理
        // 自己的设置的重载作为扩展处理
    }

    @Override
    public void onTick(MinecraftServer server) {
        // 不需要添加这个
    }

    @Override
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        YBSAdditionCommand.register(dispatcher);
    }

    @Override
    public SettingsManager customSettingsManager() {
        // 这将确保我们的设置在世界加载时正确加载
        return mySettingManager;
    }

    @Override
    public void onPlayerLoggedIn(ServerPlayer player) {
        // 可以在这里添加玩家登录逻辑
    }

    @Override
    public void onPlayerLoggedOut(ServerPlayer player) {
        // 可以在这里添加玩家登出逻辑
    }
}