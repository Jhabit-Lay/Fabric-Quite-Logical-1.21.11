package net.jhabit.quitelogical;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

public class QuiteLogicalClient implements ClientModInitializer {

    public static final Identifier LEADER_ZOMBIE_TEXTURE =
            Identifier.fromNamespaceAndPath(QuiteLogical.MOD_ID, "textures/entity/zombie/leader_zombie.png");

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(QuiteLogical.ZOMBIE_LEADER, (context) ->
                new ZombieRenderer(context) {
                    @Override
                    // [2] 이제 super 대신 우리가 만든 텍스처를 반환합니다.
                    public Identifier getTextureLocation(ZombieRenderState state) {
                        return LEADER_ZOMBIE_TEXTURE;
                    }
                }
        );
        QuiteLogical.LOGGER.info("Quite Logical 클라이언트: getTexture 메서드 적용 완료");
    }
}