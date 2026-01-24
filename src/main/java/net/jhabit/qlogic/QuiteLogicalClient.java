package net.jhabit.qlogic;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.impl.client.rendering.EntityRendererRegistryImpl;
import net.jhabit.qlogic.entity.ModEntities;
import net.jhabit.qlogic.network.PingPayload;
import net.jhabit.qlogic.render.WorldMarkerRenderer;
import net.jhabit.qlogic.util.CompassManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class QuiteLogicalClient implements ClientModInitializer {

    public static KeyMapping pingKey;

    public static final Identifier LEADER_ZOMBIE_TEXTURE =
            Identifier.fromNamespaceAndPath(QuiteLogical.MOD_ID, "textures/entity/zombie/leader_zombie.png");

    public static final Identifier JUNGLE_ZOMBIE_TEXTURE =
            Identifier.fromNamespaceAndPath(QuiteLogical.MOD_ID, "textures/entity/zombie/jungle_zombie.png");

    public static final Identifier FROSTBITE_TEXTURE =
            Identifier.fromNamespaceAndPath(QuiteLogical.MOD_ID, "textures/entity/zombie/frostbite.png");

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.ZOMBIE_LEADER, (context) ->
                new ZombieRenderer(context) {
                    @Override
                    // [2] 이제 super 대신 우리가 만든 텍스처를 반환합니다.
                    public Identifier getTextureLocation(ZombieRenderState state) {
                        return LEADER_ZOMBIE_TEXTURE;
                    }
                }
        );

        EntityRendererRegistry.register(ModEntities.JUNGLE_ZOMBIE, (context) ->
                new ZombieRenderer(context) {
                    @Override
                    public Identifier getTextureLocation(ZombieRenderState state) {
                        // 정글 좀비 전용 텍스처를 반환합니다.
                        return JUNGLE_ZOMBIE_TEXTURE;
                    }
                }
        );

        EntityRendererRegistry.register(ModEntities.FROSTBITE, (context) ->
                new ZombieRenderer(context) {
                    @Override
                    public Identifier getTextureLocation(ZombieRenderState state) {
                        // 정글 좀비 전용 텍스처를 반환합니다.
                        return FROSTBITE_TEXTURE;
                    }
                }
        );

        // [중요] 클라이언트에서도 S2C(서버->클라이언트) 패킷 구조를 등록해야 합니다.
        PayloadTypeRegistry.playC2S().register(PingPayload.ID, PingPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PingPayload.ID, PingPayload.CODEC);

        // 수신 시 CompassManager에 데이터 추가
        ClientPlayNetworking.registerGlobalReceiver(PingPayload.ID, (payload, context) -> {
            context.client().execute(() -> CompassManager.addPing(payload.pos()));
        });

        WorldMarkerRenderer.register();

        pingKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.qlogic.ping",           // 번역 키 (Description)
                InputConstants.Type.KEYSYM,  // 입력 방식
                GLFW.GLFW_KEY_V,             // 기본 키값
                KeyMapping.Category.MISC     // 오류 해결 포인트: String 대신 Category 상수를 사용합니다.
        ));

        EntityRendererRegistryImpl.register(ModEntities.THROWN_POISONOUS_POTATO, ThrownItemRenderer::new);

        QuiteLogical.LOGGER.info("Quite Logical 클라이언트: getTexture 메서드 적용 완료");
    }
}