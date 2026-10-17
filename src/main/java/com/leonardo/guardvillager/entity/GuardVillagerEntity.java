package com.leonardo.guardvillager.entity;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import java.util.List;

public class GuardVillagerEntity extends PathfinderMob implements RangedAttackMob {

    private int attackCooldown = 0;
    private int jumpAttackCooldown = 0;
    private BlockPos spawnPosition;

    public GuardVillagerEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.spawnPosition = this.blockPosition();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 45.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new UsePotionEmergencyGoal(this));
        
        // MECÁNICA STICK WAR: LEGACY - Salto con Daño Masivo Crítico
        this.goalSelector.addGoal(3, new StickWarJumpAttackGoal(this));
        
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.4D, true));
        this.goalSelector.addGoal(5, new RangedBowAttackGoal<>(this, 1.1D, 12, 45.0F));
        this.goalSelector.addGoal(6, new HealIronGolemGoal(this));
        this.goalSelector.addGoal(7, new ReturnToPostGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true, (entity) -> entity instanceof Monster));
    }

    // SOLUCIÓN HUEVO DE INVOCACIÓN: Nace siempre equipado con armas listas (Evita el bug de manos vacías)
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnGroupData) {
        spawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.spawnPosition = this.blockPosition();
        
        // 50% de probabilidad de aparecer con Espada/Escudo o Ballesta Francotirador
        if (this.random.nextBoolean()) {
            this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
            this.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        } else {
            this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CROSSBOW));
        }
        return spawnGroupData;
    }

    // DISPARO AL TOQUE FRANCOTIRADOR PERFECTO
    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        ItemStack weapon = this.getItemInHand(InteractionHand.MAIN_HAND);
        Arrow arrow = new Arrow(this.level(), this, new ItemStack(Items.ARROW), weapon);
        double d0 = target.getX() - this.getX();
        double d1 = target.getY(0.3333333333333333D) - arrow.getY();
        double d2 = target.getZ() - this.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        
        arrow.shoot(d0, d1 + d3 * 0.18D, d2, 2.2F, 0.0F); // Velocidad de flecha aumentada para dar a bichos voladores
        this.playSound(net.minecraft.sounds.SoundEvents.ARROW_SHOOT, 1.0F, 1.0F);
        this.level().addFreshEntity(arrow);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.attackCooldown > 0) this.attackCooldown--;
        if (this.jumpAttackCooldown > 0) this.jumpAttackCooldown--;
    }

    // --- LÓGICA DEL ATAQUE CRÍTICO CON SALTO (STICK WAR LEGACY) ---
    private static class StickWarJumpAttackGoal extends Goal {
        private final GuardVillagerEntity guard;

        public StickWarJumpAttackGoal(GuardVillagerEntity guard) {
            this.guard = guard;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.guard.getTarget();
            return target != null && target.isAlive() && this.guard.jumpAttackCooldown <= 0 && this.guard.distanceToSqr(target) > 9.0D && this.guard.distanceToSqr(target) < 36.0D && this.guard.onGround();
        }

        @Override
        public void start() {
            LivingEntity target = this.guard.getTarget();
            if (target != null) {
                // Impulso físico de salto hacia el enemigo
                double d0 = target.getX() - this.guard.getX();
                double d1 = target.getZ() - this.guard.getZ();
                double d2 = Math.sqrt(d0 * d0 + d1 * d1);
                this.guard.setDeltaMovement(d0 / d2 * 0.8D, 0.5D, d1 / d2 * 0.8D); // Salto parabólico
                this.guard.jumpAttackCooldown = 100; // Cooldown de 5 segundos para que no abuse del salto
            }
        }

        @Override
        public void tick() {
            LivingEntity target = this.guard.getTarget();
            if (target != null && !this.guard.onGround() && this.guard.getBoundingBox().inflate(1.0D).intersects(target.getBoundingBox())) {
                // DAÑO MASIVO CRÍTICO EN PLENO AIRE (Multiplica por 2 el daño base)
                target.hurt(this.guard.damageSources().mobAttack(this.guard), 12.0F);
                this.guard.playSound(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT, 1.5F, 1.0F);
            }
        }
    }

    // --- SUB-SISTEMAS EXTRAS ---
    private static class ReturnToPostGoal extends Goal {
        private final GuardVillagerEntity guard;
        private final double speed;
        public ReturnToPostGoal(GuardVillagerEntity guard, double speed) { this.guard = guard; this.speed = speed; }
        @Override public boolean canUse() { return this.guard.getTarget() == null && this.guard.blockPosition().distSqr(this.guard.spawnPosition) > 256; }
        @Override public void start() { this.guard.getNavigation().moveTo(this.guard.spawnPosition.getX(), this.guard.spawnPosition.getY(), this.guard.spawnPosition.getZ(), this.speed); }
    }

    private static class UsePotionEmergencyGoal extends Goal {
        private final GuardVillagerEntity guard;
        public UsePotionEmergencyGoal(GuardVillagerEntity guard) { this.guard = guard; }
        @Override public boolean canUse() { return this.guard.getHealth() < 8.0F && this.guard.getTarget() == null; }
        @Override public void start() { this.guard.setHealth(this.guard.getHealth() + 4.0F); this.guard.playSound(net.minecraft.sounds.SoundEvents.GENERIC_DRINK, 1.0F, 1.0F); }
    }

    private static class HealIronGolemGoal extends Goal {
        private final GuardVillagerEntity guard;
        public HealIronGolemGoal(GuardVillagerEntity guard) { this.guard = guard; }
        @Override public boolean canUse() {
            if (this.guard.getTarget() != null) return false;
            List<IronGolem> golems = this.guard.level().getEntitiesOfClass(IronGolem.class, this.guard.getBoundingBox().inflate(8.0D));
            for (IronGolem golem : golems) { if (golem.getHealth() < golem.getMaxHealth()) return true; }
            return false;
        }
        @Override public void start() {
            List<IronGolem> golems = this.guard.level().getEntitiesOfClass(IronGolem.class, this.guard.getBoundingBox().inflate(8.0D));
            if (!golems.isEmpty()) {
                IronGolem golem = golems.get(0);
                this.guard.getNavigation().moveTo(golem, 1.0D);
                if (this.guard.distanceToSqr(golem) < 4.0D) { golem.setHealth(golem.getHealth() + 10.0F); this.guard.playSound(net.minecraft.sounds.SoundEvents.IRON_GOLEM_REPAIR, 1.0F, 1.0F); }
            }
        }
    }
}
