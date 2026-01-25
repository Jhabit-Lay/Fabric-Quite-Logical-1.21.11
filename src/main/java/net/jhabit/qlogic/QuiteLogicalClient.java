package net.jhabit.qlogic;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.jhabit.qlogic.entity.ModEntities;
import net.jhabit.qlogic.network.PingPayload;
import net.jhabit.qlogic.render.WorldMarkerRenderer;
import net.jhabit.qlogic.util.CompassManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class QuiteLogicalClient implements ClientModInitializer {

    public static KeyMapping pingKey;
    public static KeyMapping crawlKey;

    // 변수 이름을 하나로 통일했습니다. (isCrawlingToggled)
    public static boolean isCrawlingToggled = false;

    // 엎드리기 토글 옵션 설정
    public static final OptionInstance<Boolean> crawlToggleOption = OptionInstance.createBoolean(
            "options.qlogic.crawl_toggle",
            OptionInstance.noTooltip(),
            false, // 기본값: 유지(Hold)
            (value) -> {
                // 옵션이 바뀔 때 토글 상태 초기화
                isCrawlingToggled = false;
            }
    );

    public static final Identifier LEADER_ZOMBIE_TEXTURE =
            Identifier.fromNamespaceAndPath(QuiteLogical.MOD_ID, "textures/entity/zombie/leader_zombie.png");
    public static final Identifier JUNGLE_ZOMBIE_TEXTURE =
            Identifier.fromNamespaceAndPath(QuiteLogical.MOD_ID, "textures/entity/zombie/jungle_zombie.png");
    public static final Identifier FROSTBITE_TEXTURE =
            Identifier.fromNamespaceAndPath(QuiteLogical.MOD_ID, "textures/entity/zombie/frostbite.png");

    @Override
    public void onInitializeClient() {
        // --- 1. 키바인드 등록 ---
        pingKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.qlogic.ping",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KeyMapping.Category.MISC // 카테고리 상수 수정
        ));

        crawlKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.qlogic.crawl",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                KeyMapping.Category.MOVEMENT // 카테고리 상수 수정
        ));

        // --- 2. 틱 이벤트 등록 (이 로직이 메서드 안에 있어야 합니다) ---
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // 설정이 '토글'일 때만 클릭 이벤트를 소모하여 상태를 반전
            if (crawlToggleOption.get()) {
                while (crawlKey.consumeClick()) {
                    isCrawlingToggled = !isCrawlingToggled;
                }
            } else {
                // '유지' 모드일 때는 토글 상태를 항상 꺼둠
                isCrawlingToggled = false;
            }
        });

        // --- 3. 엔티티 렌더러 등록 ---
        EntityRendererRegistry.register(ModEntities.ZOMBIE_LEADER, (context) ->
                new ZombieRenderer(context) {
                    @Override
                    public Identifier getTextureLocation(ZombieRenderState state) {
                        return LEADER_ZOMBIE_TEXTURE;
                    }
                }
        );

        EntityRendererRegistry.register(ModEntities.JUNGLE_ZOMBIE, (context) ->
                new ZombieRenderer(context) {
                    @Override
                    public Identifier getTextureLocation(ZombieRenderState state) {
                        return JUNGLE_ZOMBIE_TEXTURE;
                    }
                }
        );

        EntityRendererRegistry.register(ModEntities.FROSTBITE, (context) ->
                new ZombieRenderer(context) {
                    @Override
                    public Identifier getTextureLocation(ZombieRenderState state) {
                        return FROSTBITE_TEXTURE;
                    }
                }
        );

        EntityRendererRegistry.register(ModEntities.THROWN_POISONOUS_POTATO, ThrownItemRenderer::new);

        // --- 4. 네트워킹 및 기타 ---
        PayloadTypeRegistry.playC2S().register(PingPayload.ID, PingPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PingPayload.ID, PingPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(PingPayload.ID, (payload, context) -> {
            context.client().execute(() -> CompassManager.addPing(payload.pos()));
        });

        WorldMarkerRenderer.register();

        QuiteLogical.LOGGER.info("Quite Logical 클라이언트 로드 완료");
    }
}