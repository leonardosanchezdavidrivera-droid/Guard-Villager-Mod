package com.leonardo.guardvillager;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import com.leonardo.guardvillager.entity.GuardVillagerEntity;

public class GuardVillager implements ModInitializer {
    public static final String MOD_ID = "guard_villager";

    // REGISTRO DE ENTIDAD SEGURO: Evita el bug de crasheos por IDs duplicados al arrancar
    public static final EntityType<GuardVillagerEntity> GUARD_VILLAGER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "guard_villager"),
            EntityType.Builder.of(GuardVillagerEntity::new, MobCategory.CREATURE)
                    .dimensions(0.6F, 1.95F) // Mismo tamaño físico de un aldeano normal
                    .build("guard_villager")
    );

    @Override
    public void onInitialize() {
        // ENLACE DE ATRIBUTOS: Carga la vida y daño base de la entidad sin fallas
        FabricDefaultAttributeRegistry.register(GUARD_VILLAGER, GuardVillagerEntity.createAttributes());
    }
}
