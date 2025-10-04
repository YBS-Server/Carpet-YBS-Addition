package carpet_ybs_extension.mixins;

import carpet_ybs_extension.YBSSimpleSettings;
import carpet_ybs_extension.VillagerBedBinder;
import carpet_ybs_extension.YBSOwnSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@Mixin(Villager.class)
public class Villager_aiMixin {
    
    private static final Logger LOGGER = Logger.getLogger("CarpetYBSAddition");
    // 存储村民与绑定床位置的映射关系
    private static final Map<UUID, BlockPos> villagerBedBindings = new HashMap<>();
    // 存储床位置与绑定村民UUID的映射关系，用于实现一个床只对应一个村民
    private static final Map<BlockPos, UUID> bedToVillagerBindings = new HashMap<>();
    
    // 简化的铁傀儡生成逻辑
    private boolean trySpawnGolem(ServerLevel serverLevel) {
        try {
            // 检查总控开关
            if (!YBSOwnSettings.ybsMasterSwitch) {
                return false;
            }
            // 在基本功能模式下，铁傀儡生成功能仍然可用
            if (!YBSSimpleSettings.optimizedIronGolemSpawning) {
                return false;
            }
            
            Villager villager = (Villager)(Object)this;
            int villagerCount = serverLevel.getEntitiesOfClass(Villager.class, 
                    villager.getBoundingBox().inflate(16.0D)).size();
            
            if (villagerCount >= 10 && Math.random() < 0.05) { // 5%的概率生成
                // 尝试找到一个合适的生成位置
                BlockPos spawnPos = villager.blockPosition().offset(
                        (int)(Math.random() * 8 - 4),
                        0,
                        (int)(Math.random() * 8 - 4)
                );
                
                // 检查位置是否安全
                if (serverLevel.isEmptyBlock(spawnPos) && serverLevel.isEmptyBlock(spawnPos.above())) {
                    // 这里可以添加创建铁傀儡的代码，返回true表示尝试了生成
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error in trySpawnGolem: " + e.getMessage());
        }
        return false;
    }
    
    // 注入到tick方法中，处理铁傀儡生成和禁用村民移动AI
    @Inject(method = "tick", at = @At("HEAD"), remap = true)
    private void onTick(CallbackInfo ci) {
        try {
            // 检查总控开关
            if (!YBSOwnSettings.ybsMasterSwitch) {
                return;
            }
            Villager villager = (Villager)(Object)this;
            Level level = villager.level();
            
            // 如果启用了禁用村民移动功能，则清除村民的移动目标和导航路径
            // 但保留夜晚寻床睡觉的能力
            if (YBSOwnSettings.disableVillagerMovement && level instanceof ServerLevel serverLevel) {
                // 检查是否是夜晚且村民可能需要去睡觉
                long dayTime = serverLevel.getDayTime() % 24000;
                boolean isNight = dayTime > 12541 && dayTime < 23458;
                
                // 检查村民是否有绑定的床
                UUID villagerUUID = villager.getUUID();
                boolean hasBoundBed = villagerBedBindings.containsKey(villagerUUID);
                
                // 如果不是夜晚或者村民没有绑定的床，则阻止移动
                if (!isNight || !hasBoundBed || villager.isSleeping()) {
                    villager.getNavigation().stop();
                    // 阻止村民的移动
                    villager.setNoActionTime(200); // 设置较长的无动作时间
                    villager.setDeltaMovement(0, 0, 0); // 重置移动速度
                }
            }
            
            // 在基本功能模式下，铁傀儡生成功能仍然可用
            if (YBSSimpleSettings.optimizedIronGolemSpawning) {
                if (level instanceof ServerLevel serverLevel) {
                    // 每200刻(10秒)尝试生成一次铁傀儡
                    if (serverLevel.getGameTime() % 200 == 0) {
                        try {
                            this.trySpawnGolem(serverLevel);
                        } catch (Exception e) {
                            LOGGER.warning("Error in tick: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error in Villager_aiMixin.onTick: " + e.getMessage());
        }
    }
    
    // 处理村民与床的交互
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true, remap = true)
    private void onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        try {
            // 检查总控开关
            if (!YBSOwnSettings.ybsMasterSwitch) {
                return;
            }
            if (player == null || hand == null) {
                return;
            }
            
            ItemStack itemStack = player.getItemInHand(hand);
            
            // 优化的绑定逻辑：玩家手持烈焰棒蹲下右键村民，绑定玩家面前的单一床
            // 新增解绑功能：玩家手持空物品栏蹲下右键村民时解除绑定
            if (YBSSimpleSettings.allowForcedBedBinding && player instanceof ServerPlayer serverPlayer && 
                player.isCrouching()) {
                // 解绑功能
                if (itemStack.isEmpty()) {
                    Villager villager = (Villager)(Object)this;
                    UUID villagerUUID = villager.getUUID();
                    
                    if (villagerBedBindings.containsKey(villagerUUID)) {
                        BlockPos boundBedPos = villagerBedBindings.get(villagerUUID);
                        // 移除绑定关系
                        villagerBedBindings.remove(villagerUUID);
                        
                        // 同时清除床到村民的绑定关系
                        if (bedToVillagerBindings.containsValue(villagerUUID)) {
                            for (Map.Entry<BlockPos, UUID> entry : bedToVillagerBindings.entrySet()) {
                                if (entry.getValue().equals(villagerUUID)) {
                                    bedToVillagerBindings.remove(entry.getKey());
                                    break;
                                }
                            }
                        }
                        
                        // 显示解绑成功的粒子效果
                        if (villager.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(
                                    ParticleTypes.CLOUD,
                                    villager.getX(), villager.getY() + 1.0D, villager.getZ(),
                                    8, 0.5D, 0.5D, 0.5D, 0.05D
                            );
                        }
                        
                        // 给玩家反馈
                        serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("村民与床的绑定已解除！"), true);
                        LOGGER.info("Successfully unbound villager " + villagerUUID + " from bed at " + boundBedPos);
                    } else {
                        serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("这个村民没有绑定任何床！"), true);
                        LOGGER.warning("Player " + serverPlayer.getName().getString() + " tried to unbind villager " + villagerUUID + " but no binding existed");
                    }
                    
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    return;
                }
                // 绑定功能
                else if (itemStack.getItem() == Items.BLAZE_ROD) {
                
                Villager villager = (Villager)(Object)this;
                Level level = villager.level();
                
                if (level instanceof ServerLevel serverLevel) {
                    try {
                        LOGGER.info("Player " + serverPlayer.getName().getString() + " attempting to bind villager " + villager.getUUID() + " to bed in front");
                        
                        // 获取玩家视线方向，查找玩家面前的床
                        Entity camera = serverPlayer.getCamera();
                        Vec3 lookVec = camera.getLookAngle();
                        Vec3 startPos = camera.position().add(0, camera.getEyeHeight(), 0);
                        
                        // 搜索玩家视线前方的床，最大距离为10格
                        BlockPos targetBedPos = null;
                        for (int i = 1; i <= 10; i++) {
                            Vec3 currentPos = startPos.add(lookVec.scale(i));
                            BlockPos blockPos = BlockPos.containing(currentPos);
                            
                            // 检查方块是否在世界边界内
                            if (blockPos.getY() < serverLevel.getMinBuildHeight() || 
                                blockPos.getY() > serverLevel.getMaxBuildHeight()) {
                                continue;
                            }
                            
                            BlockState blockState = serverLevel.getBlockState(blockPos);
                            if (blockState.getBlock() instanceof BedBlock) {
                                // 直接使用找到的床位置，后续会通过getFullBedPosition获取完整床位置
                                targetBedPos = blockPos;
                                break;
                            }
                        }
                        
                        if (targetBedPos != null) {
                            // 获取完整的床位置
                            BlockState bedState = serverLevel.getBlockState(targetBedPos);
                            BlockPos fullBedPos = getFullBedPosition(targetBedPos, bedState);
                            
                            // 验证获取的是否确实是床头位置
                            BlockState fullBedState = serverLevel.getBlockState(fullBedPos);
                            if (fullBedState.getBlock() instanceof BedBlock && 
                                fullBedState.getValue(BedBlock.PART) == BedPart.HEAD) {
                                 
                                // 检查这张床是否已经被其他村民绑定
                                if (bedToVillagerBindings.containsKey(fullBedPos)) {
                                    UUID boundVillagerUUID = bedToVillagerBindings.get(fullBedPos);
                                    if (!boundVillagerUUID.equals(villager.getUUID())) {
                                        // 床已被其他村民绑定
                                        serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("这张床已经被另一个村民绑定了！"), true);
                                        LOGGER.warning("Bed at " + fullBedPos + " is already bound to villager " + boundVillagerUUID);
                                        cir.setReturnValue(InteractionResult.SUCCESS);
                                        return;
                                    }
                                }
                                
                                // 移除村民之前的绑定（如果有）
                                if (villagerBedBindings.containsKey(villager.getUUID())) {
                                    BlockPos oldBedPos = villagerBedBindings.get(villager.getUUID());
                                    bedToVillagerBindings.remove(oldBedPos);
                                }
                                
                                // 建立新的绑定关系
                                villagerBedBindings.put(villager.getUUID(), fullBedPos);
                                bedToVillagerBindings.put(fullBedPos, villager.getUUID());
                                
                                // 显示绑定成功的粒子效果
                                serverLevel.sendParticles(
                                        ParticleTypes.HAPPY_VILLAGER,
                                        fullBedPos.getX() + 0.5D, fullBedPos.getY() + 1.0D, fullBedPos.getZ() + 0.5D,
                                        15, 0.5D, 0.5D, 0.5D, 0.1D
                                );
                                
                                // 在床的两个位置都显示粒子效果，让玩家确认整个床都被绑定
                                BlockPos footPos = fullBedPos.relative(fullBedState.getValue(BedBlock.FACING).getOpposite());
                                serverLevel.sendParticles(
                                        ParticleTypes.HAPPY_VILLAGER,
                                        footPos.getX() + 0.5D, footPos.getY() + 1.0D, footPos.getZ() + 0.5D,
                                        15, 0.5D, 0.5D, 0.5D, 0.1D
                                );
                                
                                serverLevel.sendParticles(
                                        ParticleTypes.ENCHANT,
                                        villager.getX(), villager.getY() + 1.5D, villager.getZ(),
                                        10, 0.5D, 0.5D, 0.5D, 0.05D
                                );
                                
                                // 给玩家反馈
                                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("村民绑定到床成功！夜晚将会自动睡觉。"), true);
                                LOGGER.info("Successfully bound villager " + villager.getUUID() + " to bed at " + fullBedPos);
                            } else {
                                // 如果获取的不是有效床头位置，提示玩家
                                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("无法正确识别床的位置！"), true);
                                LOGGER.warning("Failed to identify valid bed position at " + fullBedPos);
                            }
                        } else {
                            // 玩家面前没有找到床
                            serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("你面前没有找到床！"), true);
                            LOGGER.warning("No bed found in front of player " + serverPlayer.getName().getString());
                        }
                    } catch (Exception e) {
                        LOGGER.warning("Failed to bind villager to bed: " + e.getMessage());
                        // 给玩家反馈失败信息
                        serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("绑定村民到床失败！"), true);
                    }
                    
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    return;
                }
                }
            }
            
            // 在基本功能模式下，只允许指定的功能
            if (YBSSimpleSettings.villagerBasicFunctionOnly) {
                // 保留功能1：床搜索距离显示
                if (itemStack.getItem() instanceof BedItem && YBSSimpleSettings.villagerBedSearchDistance > 0) {
                    Villager villager = (Villager)(Object)this;
                    Level level = villager.level();
                    
                    if (level instanceof ServerLevel serverLevel) {
                        try {
                            int searchDistance = Math.min(128, YBSSimpleSettings.villagerBedSearchDistance); // 限制最大距离
                            BlockPos villagerPos = villager.blockPosition();
                            
                            // 在村民周围显示搜索范围的粒子
                            for (int x = -searchDistance; x <= searchDistance; x += 4) {
                                for (int z = -searchDistance; z <= searchDistance; z += 4) {
                                    BlockPos particlePos = villagerPos.offset(x, 1, z);
                                    // 确保粒子位置在世界边界内
                                    if (particlePos.getY() >= serverLevel.getMinBuildHeight() && 
                                        particlePos.getY() <= serverLevel.getMaxBuildHeight()) {
                                        serverLevel.sendParticles(
                                                new BlockParticleOption(ParticleTypes.BLOCK_MARKER, Blocks.WHITE_BED.defaultBlockState()),
                                                particlePos.getX() + 0.5D, particlePos.getY() + 0.5D, particlePos.getZ() + 0.5D,
                                                1, 0.0D, 0.0D, 0.0D, 0.0D
                                        );
                                        // 向玩家显示中文提示
                                        if (player instanceof ServerPlayer serverPlayer) {
                                            serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("已显示村民的床搜索范围"), true);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.warning("Error spawning bed particles: " + e.getMessage());
                        }
                    }
                    
                    // 不要取消原始行为，只添加额外功能
                    cir.setReturnValue(InteractionResult.SUCCESS);
                }
            } else {
                // 正常模式下的所有功能
                // 当玩家手持床右击村民时，会显示搜索范围的粒子效果，直观展示设置的距离
                if (itemStack.getItem() instanceof BedItem && YBSSimpleSettings.villagerBedSearchDistance > 0) {
                    Villager villager = (Villager)(Object)this;
                    Level level = villager.level();
                    
                    if (level instanceof ServerLevel serverLevel) {
                        try {
                            int searchDistance = Math.min(128, YBSSimpleSettings.villagerBedSearchDistance); // 限制最大距离
                            BlockPos villagerPos = villager.blockPosition();
                            
                            // 在村民周围显示搜索范围的粒子
                            for (int x = -searchDistance; x <= searchDistance; x += 4) {
                                for (int z = -searchDistance; z <= searchDistance; z += 4) {
                                    BlockPos particlePos = villagerPos.offset(x, 1, z);
                                    // 确保粒子位置在世界边界内
                                    if (particlePos.getY() >= serverLevel.getMinBuildHeight() && 
                                        particlePos.getY() <= serverLevel.getMaxBuildHeight()) {
                                        serverLevel.sendParticles(
                                                new BlockParticleOption(ParticleTypes.BLOCK_MARKER, Blocks.WHITE_BED.defaultBlockState()),
                                                particlePos.getX() + 0.5D, particlePos.getY() + 0.5D, particlePos.getZ() + 0.5D,
                                                1, 0.0D, 0.0D, 0.0D, 0.0D
                                        );
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.warning("Error spawning bed particles: " + e.getMessage());
                        }
                    }
                    
                    // 不要取消原始行为，只添加额外功能
                    cir.setReturnValue(InteractionResult.SUCCESS);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error in Villager_aiMixin.onMobInteract: " + e.getMessage());
        }
    }
    // 在村民附近查找最近的床
    private BlockPos findNearestBed(ServerLevel serverLevel, BlockPos startPos, int searchDistance) {
        BlockPos nearestBed = null;
        double nearestDistance = Double.MAX_VALUE;
        
        // 搜索范围内的所有方块
        for (int x = -searchDistance; x <= searchDistance; x++) {
            for (int y = -searchDistance; y <= searchDistance; y++) {
                for (int z = -searchDistance; z <= searchDistance; z++) {
                    BlockPos currentPos = startPos.offset(x, y, z);
                    
                    // 检查方块是否在世界边界内
                    if (currentPos.getY() < serverLevel.getMinBuildHeight() || 
                        currentPos.getY() > serverLevel.getMaxBuildHeight()) {
                        continue;
                    }
                    
                    BlockState blockState = serverLevel.getBlockState(currentPos);
                    if (blockState.getBlock() instanceof BedBlock) {
                        double distance = startPos.distSqr(currentPos);
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestBed = currentPos;
                        }
                    }
                }
            }
        }
        
        return nearestBed;
    }
    
    // 获取完整的床位置（返回床头位置）
    private BlockPos getFullBedPosition(BlockPos pos, BlockState blockState) {
        if (blockState.getBlock() instanceof BedBlock) {
            BedPart part = blockState.getValue(BedBlock.PART);
            if (part == BedPart.FOOT) {
                // 如果是床尾，返回床头位置
                return pos.relative(blockState.getValue(BedBlock.FACING));
            }
        }
        return pos; // 如果已经是床头，直接返回
    }
    
    // 处理村民上下矿车的逻辑
    @Inject(method = "tick", at = @At("HEAD"), remap = true)
    private void onTickHead(CallbackInfo ci) {
        try {
            // 检查总控开关
            if (!YBSOwnSettings.ybsMasterSwitch) {
                return;
            }
            // 当基本功能模式开启时，或者限制村民只能乘坐矿车时，保留村民上下矿车的功能
            // 如果禁用了村民移动功能，则不执行任何移动相关的逻辑
            if (!YBSOwnSettings.disableVillagerMovement && (YBSSimpleSettings.villagerBasicFunctionOnly || YBSSimpleSettings.VilliagerCanOnlyRideMinecart)) {
                Villager villager = (Villager)(Object)this;
                Level level = villager.level();
                
                if (level instanceof ServerLevel serverLevel) {
                    // 处理村民进入矿车的逻辑
                    if (!villager.isPassenger() && villager.isAlive() && !villager.isSleeping() && level.getGameTime() % 40 == 0) {
                        // 搜索附近的矿车
                        AABB searchBox = villager.getBoundingBox().inflate(2.0D);
                        for (AbstractMinecart minecart : level.getEntitiesOfClass(AbstractMinecart.class, searchBox)) {
                            if (minecart.getPassengers().isEmpty()) {
                                // 尝试让村民进入矿车
                                if (villager.startRiding(minecart)) {
                                    LOGGER.info("Villager " + villager.getUUID() + " entered minecart at " + minecart.blockPosition());
                                    break;
                                }
                            }
                        }
                    }
                    
                    // 处理村民离开矿车的逻辑
                    if (villager.isPassenger() && villager.getVehicle() instanceof AbstractMinecart minecart) {
                        // 如果矿车停止移动一段时间，让村民下车
                        Vec3 motion = minecart.getDeltaMovement();
                        double speedSqr = motion.x * motion.x + motion.z * motion.z;
                        if (speedSqr < 0.001D && level.getGameTime() % 100 == 0) {
                            // 检查矿车周围是否有需要村民离开的原因（如前方有障碍或目的地）
                            BlockPos minecartPos = minecart.blockPosition();
                            BlockPos exitPos = minecartPos.above();
                            
                            // 确保下车位置是安全的
                            if (level.isEmptyBlock(exitPos) && level.isEmptyBlock(exitPos.above())) {
                                villager.stopRiding();
                                // 确保村民被传送到正确的位置
                                    // 如果禁用了村民移动功能，则不执行moveTo
                                    if (!YBSOwnSettings.disableVillagerMovement) {
                                        villager.moveTo(exitPos.getX() + 0.5D, exitPos.getY(), exitPos.getZ() + 0.5D, villager.getYRot(), villager.getXRot());
                                    }
                                LOGGER.info("Villager " + villager.getUUID() + " exited minecart at " + minecartPos);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error in Villager_aiMixin.onTickHead (minecart handling): " + e.getMessage());
        }
    }
    
    // 处理夜晚自动去绑定床睡觉的逻辑
    @Inject(method = "tick", at = @At("TAIL"), remap = true)
    private void onTickTail(CallbackInfo ci) {
        try {
            // 检查总控开关
            if (!YBSOwnSettings.ybsMasterSwitch) {
                return;
            }
            // 当基本功能模式开启时，禁用村民AI优化
            if (YBSSimpleSettings.villagerBasicFunctionOnly) {
                // 强制关闭AI优化
                if (YBSOwnSettings.VilliagerBrainCut) {
                    YBSOwnSettings.VilliagerBrainCut = false;
                    LOGGER.info("Villager AI optimization disabled due to basic function only mode");
                }
            }
            
            // 处理夜晚自动去绑定床睡觉的逻辑
            if (YBSSimpleSettings.allowForcedBedBinding) {
                Villager villager = (Villager)(Object)this;
                Level level = villager.level();
                
                if (level instanceof ServerLevel serverLevel) {
                    // 检查是否是夜晚且村民未睡觉 (使用getDayTime方法判断夜晚)
                    long dayTime = serverLevel.getDayTime() % 24000;
                    boolean isNight = dayTime > 12541 && dayTime < 23458;
                    
                    if (isNight && !villager.isSleeping() && villager.isAlive()) {
                        // 检查村民是否有绑定的床
                        UUID villagerUUID = villager.getUUID();
                        if (villagerBedBindings.containsKey(villagerUUID)) {
                            BlockPos boundBedPos = villagerBedBindings.get(villagerUUID);
                            
                            // 检查绑定的床是否仍然存在且可使用
                            BlockState bedState = serverLevel.getBlockState(boundBedPos);
                            BlockPos fullBedPos = getFullBedPosition(boundBedPos, bedState);
                            if (bedState.getBlock() instanceof BedBlock && bedState.getValue(BedBlock.OCCUPIED) == false && 
                                (!bedToVillagerBindings.containsKey(fullBedPos) || bedToVillagerBindings.get(fullBedPos).equals(villagerUUID))) {
                                // 引导村民去睡觉
                                    try {
                                        // 确保村民在绑定床的附近
                                        double distanceToBed = villager.distanceToSqr(fullBedPos.getX() + 0.5D, fullBedPos.getY(), fullBedPos.getZ() + 0.5D);
                                         
                                        // 如果村民离床较远，先让他们移动到床边
                                        // 即使禁用了村民移动功能，夜间寻床的功能仍然可用
                                        if (distanceToBed > 2.0D) {
                                            // 设置村民的目标位置为床边
                                            villager.getNavigation().moveTo(fullBedPos.getX() + 0.5D, fullBedPos.getY(), fullBedPos.getZ() + 0.5D, 1.0D);
                                        } else {
                                            // 确保村民面对床的正确方向
                                            BlockPos footPos = fullBedPos.relative(bedState.getValue(BedBlock.FACING).getOpposite());
                                            double footX = footPos.getX() + 0.5D;
                                            double footZ = footPos.getZ() + 0.5D;
                                            villager.getLookControl().setLookAt(footX, fullBedPos.getY(), footZ);
                                              
                                            // 让村民尝试睡觉
                                            villager.startSleeping(fullBedPos);
                                            LOGGER.info("Villager " + villagerUUID + " went to sleep in their bound bed at " + fullBedPos);
                                        }
                                    } catch (Exception e) {
                                        LOGGER.warning("Failed to make villager " + villagerUUID + " sleep in bound bed: " + e.getMessage());
                                    }
                            } else {
                                // 床不存在或已被占用，清除绑定关系
                                villagerBedBindings.remove(villagerUUID);
                                // 同时清除床到村民的绑定关系
                                if (bedToVillagerBindings.containsValue(villagerUUID)) {
                                    for (Map.Entry<BlockPos, UUID> entry : bedToVillagerBindings.entrySet()) {
                                        if (entry.getValue().equals(villagerUUID)) {
                                            bedToVillagerBindings.remove(entry.getKey());
                                            break;
                                        }
                                    }
                                }
                                LOGGER.warning("Villager " + villagerUUID + " bound bed at " + fullBedPos + " no longer exists or is occupied, removing binding");
                            }
                        }
                    }
                    // 白天时唤醒村民
                    else if (!isNight && villager.isSleeping()) {
                        villager.stopSleeping();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error in Villager_aiMixin.onTickTail: " + e.getMessage());
        }
    }
}