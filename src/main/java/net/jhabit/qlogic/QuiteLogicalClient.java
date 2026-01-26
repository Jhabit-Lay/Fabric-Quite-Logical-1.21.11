package net.jhabit.qlogic;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.jhabit.qlogic.entity.ModEntities;
import net.jhabit.qlogic.mixin.CopperGolemAccessor;
import net.jhabit.qlogic.network.CrawlPayload;
import net.jhabit.qlogic.network.PingPayload;
import net.jhabit.qlogic.network.RemovePingPayload;
import net.jhabit.qlogic.render.WorldMarkerRenderer;
import net.jhabit.qlogic.util.CompassManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;

public class QuiteLogicalClient implements ClientModInitializer {

    // --- [1] 키바인딩 및 설정 변수 / Keybindings and Settings ---
    public static KeyMapping pingKey;
    public static KeyMapping crawlKey;
    public static boolean isCrawlingToggled = false;

    // [KR] 엎드리기 토글 옵션: '유지' 또는 '토글' 모드 설정
    // [EN] Crawl toggle option: Set to 'Hold' or 'Toggle' mode
    public static final OptionInstance<Boolean> crawlToggleOption = OptionInstance.createBoolean(
            "options.qlogic.crawl_toggle",
            OptionInstance.noTooltip(),
            false,
            (value) -> isCrawlingToggled = false
    );

    // --- [2] 텍스처 경로 정의 / Texture Identifiers ---
    public static final Identifier LEADER_ZOMBIE_TEXTURE =
            Identifier.fromNamespaceAndPath(QuiteLogical.MOD_ID, "textures/entity/zombie/leader_zombie.png");
    public static final Identifier JUNGLE_ZOMBIE_TEXTURE =
            Identifier.fromNamespaceAndPath(QuiteLogical.MOD_ID, "textures/entity/zombie/jungle_zombie.png");
    public static final Identifier FROSTBITE_TEXTURE =
            Identifier.fromNamespaceAndPath(QuiteLogical.MOD_ID, "textures/entity/zombie/frostbite.png");

    @Override
    public void onInitializeClient() {

        final boolean[] lastCrawlState = {false};

        // --- [3] 키바인드 등록 / Keybinding Registration ---
        pingKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.qlogic.ping",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KeyMapping.Category.MISC
        ));

        crawlKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.qlogic.crawl",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                KeyMapping.Category.MOVEMENT
        ));

        // --- [4] 엎드리기 로직 틱 이벤트 / Crawl Logic Tick Event ---
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (crawlToggleOption.get()) {
                while (crawlKey.consumeClick()) {
                    isCrawlingToggled = !isCrawlingToggled;
                }
            } else {
                isCrawlingToggled = crawlKey.isDown();
            }

            // [핵심] 상태가 바뀌었을 때만 서버로 패킷 전송
            // [EN] Send packet only when state changes
            if (isCrawlingToggled != lastCrawlState[0]) {
                ClientPlayNetworking.send(new CrawlPayload(isCrawlingToggled));
                lastCrawlState[0] = isCrawlingToggled;
            }
        });

        // --- [5] 밀랍 탐지기 로직 / Wax Detector Visuals ---
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null || client.isPaused()) return;

            // [KR] 벌집이나 레진을 들고 있을 때만 주변 스캔
            // [EN] Scan surroundings only when holding Honeycomb or Resin Clump
            boolean isHoldingIndicator = client.player.isHolding(Items.HONEYCOMB) || client.player.isHolding(Items.RESIN_CLUMP);

            if (isHoldingIndicator) {
                BlockPos pPos = client.player.blockPosition();

                // [KR] 주변 블록 스캔: 이미 밀랍칠된 블록에서 입자 생성
                // [EN] Block Scan: Spawn particles on blocks that are already waxed
                for (BlockPos pos : BlockPos.betweenClosed(pPos.offset(-6, -2, -6), pPos.offset(6, 4, 6))) {
                    BlockState state = client.level.getBlockState(pos);
                    if (HoneycombItem.WAX_OFF_BY_BLOCK.get().containsKey(state.getBlock())) {
                        if (client.level.random.nextFloat() < 0.05f) { // [KR] 반짝임 빈도 조절 / [EN] Adjust sparkle frequency
                            client.level.addParticle(ParticleTypes.WAX_ON,
                                    pos.getX() + client.level.random.nextDouble(),
                                    pos.getY() + client.level.random.nextDouble(),
                                    pos.getZ() + client.level.random.nextDouble(), 0, 0, 0);
                        }
                    }
                }

                // [KR] 주변 골렘 스캔: 밀랍칠된 골렘(-2L 상태) 탐색
                // [EN] Golem Scan: Detect waxed Copper Golems (state -2L)
                for (Entity entity : client.level.entitiesForRendering()) {
                    if (entity instanceof CopperGolem golem && entity.distanceTo(client.player) < 8) {
                        if (((CopperGolemAccessor)golem).getNextWeatheringTick() == -2L) {
                            if (client.level.random.nextFloat() < 0.1f) {
                                client.level.addParticle(ParticleTypes.WAX_ON,
                                        golem.getRandomX(0.5D), golem.getRandomY(), golem.getRandomZ(0.5D), 0, 0, 0);
                            }
                        }
                    }
                }
            }
        });

        // --- [6] 엔티티 렌더러 등록 / Entity Renderer Registration ---
        qlogic$registerEntityRenderers();

        // --- [7] 네트워킹 수신기 등록 / Networking Receivers ---
        // [KR] 핑 추가 수신: 다른 플레이어의 핑을 나침반 바에 표시
        // [EN] Ping Add: Display other players' pings on the locator bar
        ClientPlayNetworking.registerGlobalReceiver(PingPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                GlobalPos gPos = GlobalPos.of(context.client().level.dimension(), payload.pos());
                CompassManager.targetMap.put(gPos, new net.jhabit.qlogic.util.CompassData(
                        Component.literal(payload.senderName()), 1, System.currentTimeMillis() + 10000L));
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(RemovePingPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                GlobalPos gPos = GlobalPos.of(context.client().level.dimension(), payload.pos());
                CompassManager.targetMap.remove(gPos);
            });
        });

        WorldMarkerRenderer.register();
        QuiteLogical.LOGGER.info("Quite Logical 클라이언트 로드 완료");
    }

    /**
     * [KR] 커스텀 몹 및 투사체 렌더러 등록 전용 메서드
     * [EN] Method for registering custom mob and projectile renderers
     */
    private void qlogic$registerEntityRenderers() {
        EntityRendererRegistry.register(ModEntities.ZOMBIE_LEADER, (context) ->
                new ZombieRenderer(context) {
                    @Override public Identifier getTextureLocation(ZombieRenderState state) { return LEADER_ZOMBIE_TEXTURE; }
                }
        );

        EntityRendererRegistry.register(ModEntities.JUNGLE_ZOMBIE, (context) ->
                new ZombieRenderer(context) {
                    @Override public Identifier getTextureLocation(ZombieRenderState state) { return JUNGLE_ZOMBIE_TEXTURE; }
                }
        );

        EntityRendererRegistry.register(ModEntities.FROSTBITE, (context) ->
                new ZombieRenderer(context) {
                    @Override public Identifier getTextureLocation(ZombieRenderState state) { return FROSTBITE_TEXTURE; }
                }
        );

        EntityRendererRegistry.register(ModEntities.THROWN_POISONOUS_POTATO, ThrownItemRenderer::new);
    }
}