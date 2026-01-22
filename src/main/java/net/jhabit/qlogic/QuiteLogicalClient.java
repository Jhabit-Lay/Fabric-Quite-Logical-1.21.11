package net.jhabit.qlogic;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.jhabit.qlogic.entity.ModEntities;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

public class QuiteLogicalClient implements ClientModInitializer {

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

        QuiteLogical.LOGGER.info("Quite Logical 클라이언트: getTexture 메서드 적용 완료");
    }
}