package net.jhabit.qlogic.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.jhabit.qlogic.QuiteLogical;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.levelgen.Heightmap;

public class ModEntities {

    public static final EntityType<LeaderZombie> ZOMBIE_LEADER = register("zombie_leader",
            EntityType.Builder.of(LeaderZombie::new, MobCategory.MONSTER).sized(0.66f, 2.145f));

    public static final EntityType<JungleZombie> JUNGLE_ZOMBIE = register("jungle_zombie",
            EntityType.Builder.of(JungleZombie::new, MobCategory.MONSTER).sized(0.6f, 1.85f));

    public static final EntityType<Frostbite> FROSTBITE = register("frostbite",
            EntityType.Builder.of(Frostbite::new, MobCategory.MONSTER).sized(0.6f, 1.85f));

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        Identifier id = Identifier.fromNamespaceAndPath(QuiteLogical.MOD_ID, name);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id,
                builder.build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
    }

    public static void initialize() {
        // 속성 등록
        FabricDefaultAttributeRegistry.register(ZOMBIE_LEADER, Zombie.createAttributes()
                .add(Attributes.SCALE, 1.05)
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.ARMOR_TOUGHNESS, 2)
        );

        FabricDefaultAttributeRegistry.register(JUNGLE_ZOMBIE, Zombie.createAttributes()
                .add(Attributes.SCALE, 0.95)
                .add(Attributes.MAX_HEALTH, 15.0)
                .add(Attributes.MOVEMENT_SPEED, 0.29)
        );

        FabricDefaultAttributeRegistry.register(FROSTBITE, Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
        );

        // 스폰 규칙 등록
        SpawnPlacements.register(JUNGLE_ZOMBIE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
        SpawnPlacements.register(FROSTBITE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
    }
}