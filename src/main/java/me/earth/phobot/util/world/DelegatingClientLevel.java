package me.earth.phobot.util.world;

import com.google.gson.internal.UnsafeAllocator;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.mixins.level.IClientLevel;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.TickPriority;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Slf4j
@SuppressWarnings({"deprecation", "LombokGetterMayBeUsed"}) // TODO: due to the super constructor this is a bit unsafe. We could add null checks to everything?
public abstract class DelegatingClientLevel extends ClientLevel implements LevelAccessor {
    // GUESS WHAT?! Lomboks @Delegate does not work since it tries to override a final method...
    // https://github.com/projectlombok/lombok/issues/941
    @Getter
    protected final ClientLevel level;

    public DelegatingClientLevel(ClientLevel level) {
        this(level, getClientPacketListener(level));
    }

    @SuppressWarnings("DataFlowIssue") // passing LevelRenderer as null is not a problem!
    private DelegatingClientLevel(ClientLevel level, ClientPacketListener connection) {
        super(connection, level.getLevelData(), level.dimension(), level.dimensionTypeRegistration(), 0, level.getServerSimulationDistance(), () -> InactiveProfiler.INSTANCE, null, level.isDebug(), 0);
        this.level = level;
    }

    // these will be called in the constructor and cannot delegate, or we crash.

    @Override
    public int getHeight() {
        return this.dimensionType().height();
    }

    @Override
    public int getMinBuildHeight() {
        return this.dimensionType().minY();
    }

    @Override
    public int getMaxBuildHeight() {
        return this.getMinBuildHeight() + this.getHeight();
    }

    @Override
    public int getSectionsCount() {
        return this.getMaxSection() - this.getMinSection();
    }

    @Override
    public int getMinSection() {
        return SectionPos.blockToSectionCoord(this.getMinBuildHeight());
    }

    @Override
    public int getMaxSection() {
        return SectionPos.blockToSectionCoord(this.getMaxBuildHeight() - 1) + 1;
    }

    @Override
    public ProfilerFiller getProfiler() {
        return InactiveProfiler.INSTANCE;
    }

    @Override
    public void updateSkyBrightness() {
        // called from super constructor
        //noinspection ConstantValue
        if (level != null) {
            level.updateSkyBrightness();
        }
    }

    @Override
    public void setDefaultSpawnPos(BlockPos blockPos, float f) {
        // this will be called from the super constructor so we have to check!
        //noinspection ConstantValue
        if (level != null) {
            level.setDefaultSpawnPos(blockPos, f);
        }
    }

    @Override
    public void handleBlockChangedAck(int i) {
        level.handleBlockChangedAck(i);
    }

    @Override
    public void setServerVerifiedBlockState(BlockPos blockPos, net.minecraft.world.level.block.state.BlockState blockState, int i) {
        level.setServerVerifiedBlockState(blockPos, blockState, i);
    }

    @Override
    public void syncBlockState(BlockPos blockPos, net.minecraft.world.level.block.state.BlockState blockState, Vec3 vec3) {
        level.syncBlockState(blockPos, blockState, vec3);
    }

    @Override
    public boolean setBlock(BlockPos blockPos, net.minecraft.world.level.block.state.BlockState blockState, int i, int j) {
        return level.setBlock(blockPos, blockState, i, j);
    }

    @Override
    public void queueLightUpdate(Runnable runnable) {
        level.queueLightUpdate(runnable);
    }

    @Override
    public void pollLightUpdates() {
        level.pollLightUpdates();
    }

    @Override
    public boolean isLightUpdateQueueEmpty() {
        return level.isLightUpdateQueueEmpty();
    }

    @Override
    public DimensionSpecialEffects effects() {
        return level.effects();
    }

    @Override
    public void tick(BooleanSupplier booleanSupplier) {
        level.tick(booleanSupplier);
    }

    @Override
    public void setGameTime(long l) {
        level.setGameTime(l);
    }

    @Override
    public void setDayTime(long l) {
        level.setDayTime(l);
    }

    @Override
    public Iterable<Entity> entitiesForRendering() {
        return level.entitiesForRendering();
    }

    @Override
    public void tickEntities() {
        level.tickEntities();
    }

    @Override
    public boolean shouldTickDeath(Entity entity) {
        return level.shouldTickDeath(entity);
    }

    @Override
    public void tickNonPassenger(Entity entity) {
        level.tickNonPassenger(entity);
    }

    @Override
    public void unload(LevelChunk levelChunk) {
        level.unload(levelChunk);
    }

    @Override
    public void onChunkLoaded(ChunkPos chunkPos) {
        level.onChunkLoaded(chunkPos);
    }

    @Override
    public void clearTintCaches() {
        level.clearTintCaches();
    }

    @Override
    public boolean hasChunk(int i, int j) {
        return level.hasChunk(i, j);
    }

    @Override
    public int getEntityCount() {
        return level.getEntityCount();
    }

    @Override
    public void addEntity(Entity entity) {
        level.addEntity(entity);
    }

    @Override
    public void playLocalSound(Entity entity, SoundEvent soundEvent, SoundSource soundSource, float f, float g) {
        level.playLocalSound(entity, soundEvent, soundSource, f, g);
    }

    @Override
    public void playSound(@Nullable Player player, double d, double e, double f, SoundEvent soundEvent, SoundSource soundSource) {
        level.playSound(player, d, e, f, soundEvent, soundSource);
    }

    @Override
    public Explosion explode(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator explosionDamageCalculator, double d, double e, double f, float g, boolean bl, ExplosionInteraction explosionInteraction, ParticleOptions particleOptions, ParticleOptions particleOptions2, SoundEvent soundEvent) {
        return level.explode(entity, damageSource, explosionDamageCalculator, d, e, f, g, bl, explosionInteraction, particleOptions, particleOptions2, soundEvent);
    }

    @Override
    public Explosion explode(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator explosionDamageCalculator, double d, double e, double f, float g, boolean bl, ExplosionInteraction explosionInteraction, boolean bl2, ParticleOptions particleOptions, ParticleOptions particleOptions2, SoundEvent soundEvent) {
        return level.explode(entity, damageSource, explosionDamageCalculator, d, e, f, g, bl, explosionInteraction, bl2, particleOptions, particleOptions2, soundEvent);
    }

    // We already copy the correct DimensionType from the given Level
    // public DimensionType dimensionType()

    // We do not override registryAccess as we already pass a valid registry access up
    // public RegistryAccess registryAccess()

    @Override
    public TickRateManager tickRateManager() {
        return level.tickRateManager();
    }

    @Override
    public void removeEntity(int i, Entity.RemovalReason removalReason) {
        level.removeEntity(i, removalReason);
    }

    @Nullable
    @Override
    public Entity getEntity(int i) {
        return level.getEntity(i);
    }

    @Override
    public void disconnect() {
        level.disconnect();
    }

    @Override
    public void animateTick(int i, int j, int k) {
        level.animateTick(i, j, k);
    }

    @Override
    public void doAnimateTick(int i, int j, int k, int l, RandomSource randomSource, @Nullable Block block, BlockPos.MutableBlockPos mutableBlockPos) {
        level.doAnimateTick(i, j, k, l, randomSource, block, mutableBlockPos);
    }

    @Override
    public CrashReportCategory fillReportDetails(CrashReport crashReport) {
        return level.fillReportDetails(crashReport);
    }

    @Override
    public void playSeededSound(@Nullable Player player, double d, double e, double f, Holder<SoundEvent> holder, SoundSource soundSource, float g, float h, long l) {
        level.playSeededSound(player, d, e, f, holder, soundSource, g, h, l);
    }

    @Override
    public void playSeededSound(@Nullable Player player, Entity entity, Holder<SoundEvent> holder, SoundSource soundSource, float f, float g, long l) {
        level.playSeededSound(player, entity, holder, soundSource, f, g, l);
    }

    @Override
    public void playLocalSound(double d, double e, double f, SoundEvent soundEvent, SoundSource soundSource, float g, float h, boolean bl) {
        level.playLocalSound(d, e, f, soundEvent, soundSource, g, h, bl);
    }

    @Override
    public void createFireworks(double d, double e, double f, double g, double h, double i, @Nullable CompoundTag compoundTag) {
        level.createFireworks(d, e, f, g, h, i, compoundTag);
    }

    @Override
    public void sendPacketToServer(Packet<?> packet) {
        level.sendPacketToServer(packet);
    }

    @Override
    public RecipeManager getRecipeManager() {
        return level.getRecipeManager();
    }

    @Override
    public void setScoreboard(Scoreboard scoreboard) {
        level.setScoreboard(scoreboard);
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return level.getBlockTicks();
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return level.getFluidTicks();
    }

    @Override
    public ClientChunkCache getChunkSource() {
        return level.getChunkSource();
    }

    @Nullable
    @Override
    public MapItemSavedData getMapData(String string) {
        return level.getMapData(string);
    }

    @Override
    public void overrideMapData(String string, MapItemSavedData mapItemSavedData) {
        level.overrideMapData(string, mapItemSavedData);
    }

    @Override
    public void setMapData(String string, MapItemSavedData mapItemSavedData) {
        level.setMapData(string, mapItemSavedData);
    }

    @Override
    public int getFreeMapId() {
        return level.getFreeMapId();
    }

    @Override
    public Scoreboard getScoreboard() {
        return level.getScoreboard();
    }

    @Override
    public void sendBlockUpdated(BlockPos blockPos, net.minecraft.world.level.block.state.BlockState blockState, net.minecraft.world.level.block.state.BlockState blockState2, int i) {
        level.sendBlockUpdated(blockPos, blockState, blockState2, i);
    }

    @Override
    public void setBlocksDirty(BlockPos blockPos, net.minecraft.world.level.block.state.BlockState blockState, net.minecraft.world.level.block.state.BlockState blockState2) {
        level.setBlocksDirty(blockPos, blockState, blockState2);
    }

    @Override
    public void setSectionDirtyWithNeighbors(int i, int j, int k) {
        level.setSectionDirtyWithNeighbors(i, j, k);
    }

    @Override
    public void destroyBlockProgress(int i, BlockPos blockPos, int j) {
        level.destroyBlockProgress(i, blockPos, j);
    }

    @Override
    public void globalLevelEvent(int i, BlockPos blockPos, int j) {
        level.globalLevelEvent(i, blockPos, j);
    }

    @Override
    public void levelEvent(@Nullable Player player, int i, BlockPos blockPos, int j) {
        level.levelEvent(player, i, blockPos, j);
    }

    @Override
    public void addParticle(ParticleOptions particleOptions, double d, double e, double f, double g, double h, double i) {
        level.addParticle(particleOptions, d, e, f, g, h, i);
    }

    @Override
    public void addParticle(ParticleOptions particleOptions, boolean bl, double d, double e, double f, double g, double h, double i) {
        level.addParticle(particleOptions, bl, d, e, f, g, h, i);
    }

    @Override
    public void addAlwaysVisibleParticle(ParticleOptions particleOptions, double d, double e, double f, double g, double h, double i) {
        level.addAlwaysVisibleParticle(particleOptions, d, e, f, g, h, i);
    }

    @Override
    public void addAlwaysVisibleParticle(ParticleOptions particleOptions, boolean bl, double d, double e, double f, double g, double h, double i) {
        level.addAlwaysVisibleParticle(particleOptions, bl, d, e, f, g, h, i);
    }

    @Override
    public List<AbstractClientPlayer> players() {
        return level.players();
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int i, int j, int k) {
        return level.getUncachedNoiseBiome(i, j, k);
    }

    @Override
    public float getSkyDarken(float f) {
        return level.getSkyDarken(f);
    }

    @Override
    public Vec3 getSkyColor(Vec3 vec3, float f) {
        return level.getSkyColor(vec3, f);
    }

    @Override
    public Vec3 getCloudColor(float f) {
        return level.getCloudColor(f);
    }

    @Override
    public float getStarBrightness(float f) {
        return level.getStarBrightness(f);
    }

    @Override
    public int getSkyFlashTime() {
        return level.getSkyFlashTime();
    }

    @Override
    public void setSkyFlashTime(int i) {
        level.setSkyFlashTime(i);
    }

    @Override
    public float getShade(Direction direction, boolean bl) {
        return level.getShade(direction, bl);
    }

    @Override
    public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
        return level.getBlockTint(blockPos, colorResolver);
    }

    @Override
    public int calculateBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
        return level.calculateBlockTint(blockPos, colorResolver);
    }

    @Override
    public String toString() {
        return level.toString();
    }

    @Override
    public ClientLevelData getLevelData() {
        return level.getLevelData();
    }

    @Override
    public void gameEvent(GameEvent gameEvent, Vec3 vec3, GameEvent.Context context) {
        level.gameEvent(gameEvent, vec3, context);
    }

    @Override
    public String gatherChunkSourceStats() {
        return level.gatherChunkSourceStats();
    }

    @Override
    public void addDestroyBlockEffect(BlockPos blockPos, net.minecraft.world.level.block.state.BlockState blockState) {
        level.addDestroyBlockEffect(blockPos, blockState);
    }

    @Override
    public void setServerSimulationDistance(int i) {
        level.setServerSimulationDistance(i);
    }

    @Override
    public int getServerSimulationDistance() {
        return level.getServerSimulationDistance();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return level.enabledFeatures();
    }

    @Override
    public boolean isClientSide() {
        return level.isClientSide();
    }

    @Nullable
    @Override
    public MinecraftServer getServer() {
        return level.getServer();
    }

    @Override
    public boolean isInWorldBounds(BlockPos blockPos) {
        return level.isInWorldBounds(blockPos);
    }

    @Override
    public LevelChunk getChunkAt(BlockPos blockPos) {
        return level.getChunkAt(blockPos);
    }

    @Override
    public LevelChunk getChunk(int i, int j) {
        return level.getChunk(i, j);
    }

    @Nullable
    @Override
    public ChunkAccess getChunk(int i, int j, ChunkStatus chunkStatus, boolean bl) {
        return level.getChunk(i, j, chunkStatus, bl);
    }

    @Override
    public boolean setBlock(BlockPos blockPos, net.minecraft.world.level.block.state.BlockState blockState, int i) {
        return level.setBlock(blockPos, blockState, i);
    }

    @Override
    public void onBlockStateChange(BlockPos blockPos, net.minecraft.world.level.block.state.BlockState blockState, net.minecraft.world.level.block.state.BlockState blockState2) {
        level.onBlockStateChange(blockPos, blockState, blockState2);
    }

    @Override
    public boolean removeBlock(BlockPos blockPos, boolean bl) {
        return level.removeBlock(blockPos, bl);
    }

    @Override
    public boolean destroyBlock(BlockPos blockPos, boolean bl, @Nullable Entity entity, int i) {
        return level.destroyBlock(blockPos, bl, entity, i);
    }

    @Override
    public boolean setBlockAndUpdate(BlockPos blockPos, net.minecraft.world.level.block.state.BlockState blockState) {
        return level.setBlockAndUpdate(blockPos, blockState);
    }

    @Override
    public void updateNeighborsAt(BlockPos blockPos, Block block) {
        level.updateNeighborsAt(blockPos, block);
    }

    @Override
    public void updateNeighborsAtExceptFromFacing(BlockPos blockPos, Block block, Direction direction) {
        level.updateNeighborsAtExceptFromFacing(blockPos, block, direction);
    }

    @Override
    public void neighborChanged(BlockPos blockPos, Block block, BlockPos blockPos2) {
        level.neighborChanged(blockPos, block, blockPos2);
    }

    @Override
    public void neighborChanged(net.minecraft.world.level.block.state.BlockState blockState, BlockPos blockPos, Block block, BlockPos blockPos2, boolean bl) {
        level.neighborChanged(blockState, blockPos, block, blockPos2, bl);
    }

    @Override
    public void neighborShapeChanged(Direction direction, net.minecraft.world.level.block.state.BlockState blockState, BlockPos blockPos, BlockPos blockPos2, int i, int j) {
        level.neighborShapeChanged(direction, blockState, blockPos, blockPos2, i, j);
    }

    @Override
    public int getHeight(Heightmap.Types types, int i, int j) {
        return level.getHeight(types, i, j);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return level.getLightEngine();
    }

    @Override
    public net.minecraft.world.level.block.state.BlockState getBlockState(BlockPos blockPos) {
        return level.getBlockState(blockPos);
    }

    @Override
    public FluidState getFluidState(BlockPos blockPos) {
        return level.getFluidState(blockPos);
    }

    @Override
    public boolean isDay() {
        return level.isDay();
    }

    @Override
    public boolean isNight() {
        return level.isNight();
    }

    @Override
    public void playSound(@Nullable Entity entity, BlockPos blockPos, SoundEvent soundEvent, SoundSource soundSource, float f, float g) {
        level.playSound(entity, blockPos, soundEvent, soundSource, f, g);
    }

    @Override
    public void playSound(@Nullable Player player, BlockPos blockPos, SoundEvent soundEvent, SoundSource soundSource, float f, float g) {
        level.playSound(player, blockPos, soundEvent, soundSource, f, g);
    }

    @Override
    public void playSeededSound(@Nullable Player player, double d, double e, double f, SoundEvent soundEvent, SoundSource soundSource, float g, float h, long l) {
        level.playSeededSound(player, d, e, f, soundEvent, soundSource, g, h, l);
    }

    @Override
    public void playSound(@Nullable Player player, double d, double e, double f, SoundEvent soundEvent, SoundSource soundSource, float g, float h) {
        level.playSound(player, d, e, f, soundEvent, soundSource, g, h);
    }

    @Override
    public void playSound(@Nullable Player player, Entity entity, SoundEvent soundEvent, SoundSource soundSource, float f, float g) {
        level.playSound(player, entity, soundEvent, soundSource, f, g);
    }

    @Override
    public void playLocalSound(BlockPos blockPos, SoundEvent soundEvent, SoundSource soundSource, float f, float g, boolean bl) {
        level.playLocalSound(blockPos, soundEvent, soundSource, f, g, bl);
    }

    @Override
    public float getSunAngle(float f) {
        return level.getSunAngle(f);
    }

    @Override
    public void addBlockEntityTicker(TickingBlockEntity tickingBlockEntity) {
        level.addBlockEntityTicker(tickingBlockEntity);
    }

    @Override
    public <T extends Entity> void guardEntityTick(Consumer<T> consumer, T entity) {
        level.guardEntityTick(consumer, entity);
    }

    @Override
    public boolean shouldTickBlocksAt(long l) {
        return level.shouldTickBlocksAt(l);
    }

    @Override
    public boolean shouldTickBlocksAt(BlockPos blockPos) {
        return level.shouldTickBlocksAt(blockPos);
    }

    @Override
    public Explosion explode(@Nullable Entity entity, double d, double e, double f, float g, ExplosionInteraction explosionInteraction) {
        return level.explode(entity, d, e, f, g, explosionInteraction);
    }

    @Override
    public Explosion explode(@Nullable Entity entity, double d, double e, double f, float g, boolean bl, ExplosionInteraction explosionInteraction) {
        return level.explode(entity, d, e, f, g, bl, explosionInteraction);
    }

    @Override
    public Explosion explode(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator explosionDamageCalculator, Vec3 vec3, float f, boolean bl, ExplosionInteraction explosionInteraction) {
        return level.explode(entity, damageSource, explosionDamageCalculator, vec3, f, bl, explosionInteraction);
    }

    @Override
    public Explosion explode(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator explosionDamageCalculator, double d, double e, double f, float g, boolean bl, ExplosionInteraction explosionInteraction) {
        return level.explode(entity, damageSource, explosionDamageCalculator, d, e, f, g, bl, explosionInteraction);
    }

    @Override
    public boolean noBlockCollision(@Nullable Entity entity, AABB aABB) {
        return level.noBlockCollision(entity, aABB);
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos blockPos) {
        return level.getBlockEntity(blockPos);
    }

    @Override
    public void setBlockEntity(BlockEntity blockEntity) {
        level.setBlockEntity(blockEntity);
    }

    @Override
    public void removeBlockEntity(BlockPos blockPos) {
        level.removeBlockEntity(blockPos);
    }

    @Override
    public boolean isLoaded(BlockPos blockPos) {
        return level.isLoaded(blockPos);
    }

    @Override
    public boolean loadedAndEntityCanStandOnFace(BlockPos blockPos, Entity entity, Direction direction) {
        return level.loadedAndEntityCanStandOnFace(blockPos, entity, direction);
    }

    @Override
    public boolean loadedAndEntityCanStandOn(BlockPos blockPos, Entity entity) {
        return level.loadedAndEntityCanStandOn(blockPos, entity);
    }

    @Override
    public void setSpawnSettings(boolean bl, boolean bl2) {
        level.setSpawnSettings(bl, bl2);
    }

    @Override
    public BlockPos getSharedSpawnPos() {
        return level.getSharedSpawnPos();
    }

    @Override
    public float getSharedSpawnAngle() {
        return level.getSharedSpawnAngle();
    }

    @Override
    public void close() throws IOException {
        level.close();
    }

    @Nullable
    @Override
    public BlockGetter getChunkForCollisions(int i, int j) {
        return level.getChunkForCollisions(i, j);
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity entity, AABB aABB, Predicate<? super Entity> predicate) {
        return level.getEntities(entity, aABB, predicate);
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB aABB, Predicate<? super T> predicate) {
        return level.getEntities(entityTypeTest, aABB, predicate);
    }

    @Override
    public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB aABB, Predicate<? super T> predicate, List<? super T> list) {
        level.getEntities(entityTypeTest, aABB, predicate, list);
    }

    @Override
    public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB aABB, Predicate<? super T> predicate, List<? super T> list, int i) {
        level.getEntities(entityTypeTest, aABB, predicate, list, i);
    }

    @Override
    public void blockEntityChanged(BlockPos blockPos) {
        level.blockEntityChanged(blockPos);
    }

    @Override
    public int getSeaLevel() {
        return level.getSeaLevel();
    }

    @Override
    public long getGameTime() {
        return level.getGameTime();
    }

    @Override
    public long getDayTime() {
        return level.getDayTime();
    }

    @Override
    public boolean mayInteract(Player player, BlockPos blockPos) {
        return level.mayInteract(player, blockPos);
    }

    @Override
    public void broadcastEntityEvent(Entity entity, byte b) {
        level.broadcastEntityEvent(entity, b);
    }

    @Override
    public void broadcastDamageEvent(Entity entity, DamageSource damageSource) {
        level.broadcastDamageEvent(entity, damageSource);
    }

    @Override
    public void blockEvent(BlockPos blockPos, Block block, int i, int j) {
        level.blockEvent(blockPos, block, i, j);
    }

    @Override
    public GameRules getGameRules() {
        return level.getGameRules();
    }

    @Override
    public float getThunderLevel(float f) {
        return level.getThunderLevel(f);
    }

    @Override
    public void setThunderLevel(float f) {
        level.setThunderLevel(f);
    }

    @Override
    public float getRainLevel(float f) {
        return level.getRainLevel(f);
    }

    @Override
    public void setRainLevel(float f) {
        level.setRainLevel(f);
    }

    @Override
    public boolean isThundering() {
        return level.isThundering();
    }

    @Override
    public boolean isRaining() {
        return level.isRaining();
    }

    @Override
    public boolean isRainingAt(BlockPos blockPos) {
        return level.isRainingAt(blockPos);
    }

    @Override
    public void updateNeighbourForOutputSignal(BlockPos blockPos, Block block) {
        level.updateNeighbourForOutputSignal(blockPos, block);
    }

    @Override
    public DifficultyInstance getCurrentDifficultyAt(BlockPos blockPos) {
        return level.getCurrentDifficultyAt(blockPos);
    }

    @Override
    public int getSkyDarken() {
        return level.getSkyDarken();
    }

    @Override
    public WorldBorder getWorldBorder() {
        return level.getWorldBorder();
    }

    @Override
    public ResourceKey<DimensionType> dimensionTypeId() {
        return level.dimensionTypeId();
    }

    @Override
    public Holder<DimensionType> dimensionTypeRegistration() {
        return level.dimensionTypeRegistration();
    }

    @Override
    public ResourceKey<Level> dimension() {
        return level.dimension();
    }

    @Override
    public RandomSource getRandom() {
        return level.getRandom();
    }

    @Override
    public boolean isStateAtPosition(BlockPos blockPos, Predicate<net.minecraft.world.level.block.state.BlockState> predicate) {
        return level.isStateAtPosition(blockPos, predicate);
    }

    @Override
    public boolean isFluidAtPosition(BlockPos blockPos, Predicate<FluidState> predicate) {
        return level.isFluidAtPosition(blockPos, predicate);
    }

    @Override
    public BlockPos getBlockRandomPos(int i, int j, int k, int l) {
        return level.getBlockRandomPos(i, j, k, l);
    }

    @Override
    public boolean noSave() {
        return level.noSave();
    }

    @Override
    public Supplier<ProfilerFiller> getProfilerSupplier() {
        return level.getProfilerSupplier();
    }

    @Override
    public BiomeManager getBiomeManager() {
        return level.getBiomeManager();
    }

    @Override
    public long nextSubTickCount() {
        return level.nextSubTickCount();
    }

    @Override
    public DamageSources damageSources() {
        return level.damageSources();
    }

    @Override
    public long dayTime() {
        return level.dayTime();
    }

    @Override
    public void scheduleTick(BlockPos blockPos, Block block, int i, TickPriority tickPriority) {
        level.scheduleTick(blockPos, block, i, tickPriority);
    }

    @Override
    public void scheduleTick(BlockPos blockPos, Block block, int i) {
        level.scheduleTick(blockPos, block, i);
    }

    @Override
    public void scheduleTick(BlockPos blockPos, Fluid fluid, int i, TickPriority tickPriority) {
        level.scheduleTick(blockPos, fluid, i, tickPriority);
    }

    @Override
    public void scheduleTick(BlockPos blockPos, Fluid fluid, int i) {
        level.scheduleTick(blockPos, fluid, i);
    }

    @Override
    public Difficulty getDifficulty() {
        return level.getDifficulty();
    }

    @Override
    public void blockUpdated(BlockPos blockPos, Block block) {
        level.blockUpdated(blockPos, block);
    }

    @Override
    public void playSound(@Nullable Player player, BlockPos blockPos, SoundEvent soundEvent, SoundSource soundSource) {
        level.playSound(player, blockPos, soundEvent, soundSource);
    }

    @Override
    public void levelEvent(int i, BlockPos blockPos, int j) {
        level.levelEvent(i, blockPos, j);
    }

    @Override
    public void gameEvent(@Nullable Entity entity, GameEvent gameEvent, Vec3 vec3) {
        level.gameEvent(entity, gameEvent, vec3);
    }

    @Override
    public void gameEvent(@Nullable Entity entity, GameEvent gameEvent, BlockPos blockPos) {
        level.gameEvent(entity, gameEvent, blockPos);
    }

    @Override
    public void gameEvent(GameEvent gameEvent, BlockPos blockPos, GameEvent.Context context) {
        level.gameEvent(gameEvent, blockPos, context);
    }

    @Override
    public <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos blockPos, BlockEntityType<T> blockEntityType) {
        return level.getBlockEntity(blockPos, blockEntityType);
    }

    @Override
    public List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB aABB) {
        return level.getEntityCollisions(entity, aABB);
    }

    @Override
    public boolean isUnobstructed(@Nullable Entity entity, VoxelShape voxelShape) {
        return level.isUnobstructed(entity, voxelShape);
    }

    @Override
    public BlockPos getHeightmapPos(Heightmap.Types types, BlockPos blockPos) {
        return level.getHeightmapPos(types, blockPos);
    }

    @Override
    public int getBrightness(LightLayer lightLayer, BlockPos blockPos) {
        return level.getBrightness(lightLayer, blockPos);
    }

    @Override
    public int getRawBrightness(BlockPos blockPos, int i) {
        return level.getRawBrightness(blockPos, i);
    }

    @Override
    public boolean canSeeSky(BlockPos blockPos) {
        return level.canSeeSky(blockPos);
    }

    @Override
    public boolean isUnobstructed(net.minecraft.world.level.block.state.BlockState blockState, BlockPos blockPos, CollisionContext collisionContext) {
        return level.isUnobstructed(blockState, blockPos, collisionContext);
    }

    @Override
    public boolean isUnobstructed(Entity entity) {
        return level.isUnobstructed(entity);
    }

    @Override
    public boolean noCollision(AABB aABB) {
        return level.noCollision(aABB);
    }

    @Override
    public boolean noCollision(Entity entity) {
        return level.noCollision(entity);
    }

    @Override
    public boolean noCollision(@Nullable Entity entity, AABB aABB) {
        return level.noCollision(entity, aABB);
    }

    @Override
    public Iterable<VoxelShape> getCollisions(@Nullable Entity entity, AABB aABB) {
        return level.getCollisions(entity, aABB);
    }

    @Override
    public Iterable<VoxelShape> getBlockCollisions(@Nullable Entity entity, AABB aABB) {
        return level.getBlockCollisions(entity, aABB);
    }

    @Override
    public boolean collidesWithSuffocatingBlock(@Nullable Entity entity, AABB aABB) {
        return level.collidesWithSuffocatingBlock(entity, aABB);
    }

    @Override
    public Optional<BlockPos> findSupportingBlock(Entity entity, AABB aABB) {
        return level.findSupportingBlock(entity, aABB);
    }

    @Override
    public Optional<Vec3> findFreePosition(@Nullable Entity entity, VoxelShape voxelShape, Vec3 vec3, double d, double e, double f) {
        return level.findFreePosition(entity, voxelShape, vec3, d, e, f);
    }

    @Override
    public int getLightEmission(BlockPos blockPos) {
        return level.getLightEmission(blockPos);
    }

    @Override
    public int getMaxLightLevel() {
        return level.getMaxLightLevel();
    }

    @Override
    public Stream<net.minecraft.world.level.block.state.BlockState> getBlockStates(AABB aABB) {
        return level.getBlockStates(aABB);
    }

    @Override
    public BlockHitResult isBlockInLine(ClipBlockStateContext clipBlockStateContext) {
        return level.isBlockInLine(clipBlockStateContext);
    }

    @Override
    public BlockHitResult clip(ClipContext clipContext) {
        return level.clip(clipContext);
    }

    @Nullable
    @Override
    public BlockHitResult clipWithInteractionOverride(Vec3 vec3, Vec3 vec32, BlockPos blockPos, VoxelShape voxelShape, net.minecraft.world.level.block.state.BlockState blockState) {
        return level.clipWithInteractionOverride(vec3, vec32, blockPos, voxelShape, blockState);
    }

    @Override
    public double getBlockFloorHeight(VoxelShape voxelShape, Supplier<VoxelShape> supplier) {
        return level.getBlockFloorHeight(voxelShape, supplier);
    }

    @Override
    public double getBlockFloorHeight(BlockPos blockPos) {
        return level.getBlockFloorHeight(blockPos);
    }

    @Override
    public <T extends Entity> List<T> getEntitiesOfClass(Class<T> class_, AABB aABB, Predicate<? super T> predicate) {
        return level.getEntitiesOfClass(class_, aABB, predicate);
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity entity, AABB aABB) {
        return level.getEntities(entity, aABB);
    }

    @Override
    public <T extends Entity> List<T> getEntitiesOfClass(Class<T> class_, AABB aABB) {
        return level.getEntitiesOfClass(class_, aABB);
    }

    @Nullable
    @Override
    public Player getNearestPlayer(double d, double e, double f, double g, @Nullable Predicate<Entity> predicate) {
        return level.getNearestPlayer(d, e, f, g, predicate);
    }

    @Nullable
    @Override
    public Player getNearestPlayer(Entity entity, double d) {
        return level.getNearestPlayer(entity, d);
    }

    @Nullable
    @Override
    public Player getNearestPlayer(double d, double e, double f, double g, boolean bl) {
        return level.getNearestPlayer(d, e, f, g, bl);
    }

    @Override
    public boolean hasNearbyAlivePlayer(double d, double e, double f, double g) {
        return level.hasNearbyAlivePlayer(d, e, f, g);
    }

    @Nullable
    @Override
    public Player getNearestPlayer(TargetingConditions targetingConditions, LivingEntity livingEntity) {
        return level.getNearestPlayer(targetingConditions, livingEntity);
    }

    @Nullable
    @Override
    public Player getNearestPlayer(TargetingConditions targetingConditions, LivingEntity livingEntity, double d, double e, double f) {
        return level.getNearestPlayer(targetingConditions, livingEntity, d, e, f);
    }

    @Nullable
    @Override
    public Player getNearestPlayer(TargetingConditions targetingConditions, double d, double e, double f) {
        return level.getNearestPlayer(targetingConditions, d, e, f);
    }

    @Nullable
    @Override
    public <T extends LivingEntity> T getNearestEntity(Class<? extends T> class_, TargetingConditions targetingConditions, @Nullable LivingEntity livingEntity, double d, double e, double f, AABB aABB) {
        return level.getNearestEntity(class_, targetingConditions, livingEntity, d, e, f, aABB);
    }

    @Nullable
    @Override
    public <T extends LivingEntity> T getNearestEntity(List<? extends T> list, TargetingConditions targetingConditions, @Nullable LivingEntity livingEntity, double d, double e, double f) {
        return level.getNearestEntity(list, targetingConditions, livingEntity, d, e, f);
    }

    @Override
    public List<Player> getNearbyPlayers(TargetingConditions targetingConditions, LivingEntity livingEntity, AABB aABB) {
        return level.getNearbyPlayers(targetingConditions, livingEntity, aABB);
    }

    @Override
    public <T extends LivingEntity> List<T> getNearbyEntities(Class<T> class_, TargetingConditions targetingConditions, LivingEntity livingEntity, AABB aABB) {
        return level.getNearbyEntities(class_, targetingConditions, livingEntity, aABB);
    }

    @Nullable
    @Override
    public Player getPlayerByUUID(UUID uUID) {
        return level.getPlayerByUUID(uUID);
    }

    @Override
    public float getMoonBrightness() {
        return level.getMoonBrightness();
    }

    @Override
    public float getTimeOfDay(float f) {
        return level.getTimeOfDay(f);
    }

    @Override
    public int getMoonPhase() {
        return level.getMoonPhase();
    }

    @Override
    public Holder<Biome> getBiome(BlockPos blockPos) {
        return level.getBiome(blockPos);
    }

    @Override
    public Stream<net.minecraft.world.level.block.state.BlockState> getBlockStatesIfLoaded(AABB aABB) {
        return level.getBlockStatesIfLoaded(aABB);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int i, int j, int k) {
        return level.getNoiseBiome(i, j, k);
    }

    @Override
    public boolean isEmptyBlock(BlockPos blockPos) {
        //
        return this.getBlockState(blockPos).isAir();
    }

    @Override
    public boolean canSeeSkyFromBelowWater(BlockPos blockPos) {
        return level.canSeeSkyFromBelowWater(blockPos);
    }

    @Override
    public float getPathfindingCostFromLightLevels(BlockPos blockPos) {
        return level.getPathfindingCostFromLightLevels(blockPos);
    }

    @Override
    public float getLightLevelDependentMagicValue(BlockPos blockPos) {
        return level.getLightLevelDependentMagicValue(blockPos);
    }

    @Override
    public ChunkAccess getChunk(BlockPos blockPos) {
        return level.getChunk(blockPos);
    }

    @Override
    public ChunkAccess getChunk(int i, int j, ChunkStatus chunkStatus) {
        return level.getChunk(i, j, chunkStatus);
    }

    @Override
    public boolean isWaterAt(BlockPos blockPos) {
        return level.isWaterAt(blockPos);
    }

    @Override
    public boolean containsAnyLiquid(AABB aABB) {
        return level.containsAnyLiquid(aABB);
    }

    @Override
    public int getMaxLocalRawBrightness(BlockPos blockPos) {
        return level.getMaxLocalRawBrightness(blockPos);
    }

    @Override
    public int getMaxLocalRawBrightness(BlockPos blockPos, int i) {
        return level.getMaxLocalRawBrightness(blockPos, i);
    }

    @Override
    public boolean hasChunkAt(int i, int j) {
        return level.hasChunkAt(i, j);
    }

    @Override
    public boolean hasChunkAt(BlockPos blockPos) {
        return level.hasChunkAt(blockPos);
    }

    @Override
    public boolean hasChunksAt(BlockPos blockPos, BlockPos blockPos2) {
        return level.hasChunksAt(blockPos, blockPos2);
    }

    @Override
    public boolean hasChunksAt(int i, int j, int k, int l, int m, int n) {
        return level.hasChunksAt(i, j, k, l, m, n);
    }

    @Override
    public boolean hasChunksAt(int i, int j, int k, int l) {
        return level.hasChunksAt(i, j, k, l);
    }

    @Override
    public <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<? extends T>> resourceKey) {
        return level.holderLookup(resourceKey);
    }

    @Override
    public boolean destroyBlock(BlockPos blockPos, boolean bl) {
        return level.destroyBlock(blockPos, bl);
    }

    @Override
    public boolean destroyBlock(BlockPos blockPos, boolean bl, @Nullable Entity entity) {
        return level.destroyBlock(blockPos, bl, entity);
    }

    @Override
    public boolean addFreshEntity(Entity entity) {
        return level.addFreshEntity(entity);
    }

    @Override
    public int getDirectSignal(BlockPos blockPos, Direction direction) {
        return level.getDirectSignal(blockPos, direction);
    }

    @Override
    public int getDirectSignalTo(BlockPos blockPos) {
        return level.getDirectSignalTo(blockPos);
    }

    @Override
    public int getControlInputSignal(BlockPos blockPos, Direction direction, boolean bl) {
        return level.getControlInputSignal(blockPos, direction, bl);
    }

    @Override
    public boolean hasSignal(BlockPos blockPos, Direction direction) {
        return level.hasSignal(blockPos, direction);
    }

    @Override
    public int getSignal(BlockPos blockPos, Direction direction) {
        return level.getSignal(blockPos, direction);
    }

    @Override
    public boolean hasNeighborSignal(BlockPos blockPos) {
        return level.hasNeighborSignal(blockPos);
    }

    @Override
    public int getBestNeighborSignal(BlockPos blockPos) {
        return level.getBestNeighborSignal(blockPos);
    }

    @Override
    public boolean isOutsideBuildHeight(BlockPos blockPos) {
        return level.isOutsideBuildHeight(blockPos);
    }

    @Override
    public boolean isOutsideBuildHeight(int i) {
        return level.isOutsideBuildHeight(i);
    }

    @Override
    public int getSectionIndex(int i) {
        return level.getSectionIndex(i);
    }

    @Override
    public int getSectionIndexFromSectionY(int i) {
        return level.getSectionIndexFromSectionY(i);
    }

    @Override
    public int getSectionYFromSectionIndex(int i) {
        return level.getSectionYFromSectionIndex(i);
    }

    private static ClientPacketListener getClientPacketListener(ClientLevel level) {
        if (level instanceof IClientLevel iClientLevel) {
            return iClientLevel.getConnection();
        }

        log.error("IClientLevel duck not applied to " + level + ", if in a Unit Test ignore this message.");
        return DummyClientPacketListener.create(level.registryAccess());
    }

    @VisibleForTesting
    public static final class DummyClientPacketListener extends ClientPacketListener {
        private RegistryAccess.Frozen registryAccess;

        @Override
        public RegistryAccess.Frozen registryAccess() {
            return registryAccess;
        }

        @SneakyThrows
        public static DummyClientPacketListener create(RegistryAccess registryAccess) {
            RegistryAccess.Frozen frozen = registryAccess instanceof RegistryAccess.Frozen alreadyFrozen
                    ? alreadyFrozen
                    : new RegistryAccess.Frozen() {
                        @Override
                        public <E> Optional<Registry<E>> registry(ResourceKey<? extends Registry<? extends E>> resourceKey) {
                            return registryAccess.registry(resourceKey);
                        }

                        @Override
                        public Stream<RegistryEntry<?>> registries() {
                            return registryAccess.registries();
                        }
                    };
            //noinspection ConstantValue
            if (Minecraft.getInstance() == null) { // can happen in tests, there we need Unsafe
                DummyClientPacketListener dummyClientPacketListener = UnsafeAllocator.INSTANCE.newInstance(DummyClientPacketListener.class);
                dummyClientPacketListener.registryAccess = frozen;
                return dummyClientPacketListener;
            }

            return new DummyClientPacketListener(frozen);
        }

        private DummyClientPacketListener(RegistryAccess.Frozen registryAccess) {
            super(Minecraft.getInstance(), new Connection(PacketFlow.SERVERBOUND), new CommonListenerCookie(null, null, null, null, null, null, null));
            this.registryAccess = registryAccess;
        }
    }

}
