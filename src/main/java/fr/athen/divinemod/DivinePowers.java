package fr.athen.divinemod;

import fr.athen.divinemod.item.ModItems;
import fr.athen.divinemod.network.DivineActionPayload;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DivinePowers {
    private static final Identifier LEGS_SPEED = DivineMod.id("legs_speed");
    private static final Identifier LEGS_STEP = DivineMod.id("legs_step");
    private static final String CLOUD_TAG = "divinemod_cloud";
    private static final float[] CLOUD_SPEEDS = {0.02f, 0.035f, 0.05f, 0.07f, 0.09f, 0.11f, 0.14f, 0.17f, 0.21f, 0.26f};

    private static final Map<UUID, Integer> CLOUD_LEVEL = new HashMap<>();
    private static final Map<UUID, Long> CLONE_EXPIRY = new HashMap<>();
    private static final Map<UUID, Long> CLONE_COOLDOWN = new HashMap<>();
    private static final List<Scheduled> SCHEDULED = new ArrayList<>();

    private record Scheduled(long at, Runnable task) {}

    private DivinePowers() {}

    public static void schedule(MinecraftServer server, int delayTicks, Runnable task) {
        SCHEDULED.add(new Scheduled(server.getTicks() + delayTicks, task));
    }

    // =====================================================================
    // Tick serveur
    // =====================================================================
    public static void tick(MinecraftServer server) {
        long t = server.getTicks();

        if (!SCHEDULED.isEmpty()) {
            List<Scheduled> due = new ArrayList<>();
            SCHEDULED.removeIf(s -> {
                if (s.at() <= t) { due.add(s); return true; }
                return false;
            });
            for (Scheduled s : due) s.task().run();
        }

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) tickPlayer(p, t);

        if (t % 5 == 0) {
            for (ServerWorld w : server.getWorlds()) tickAllies(w, t);
        }
    }

    private static void tickPlayer(ServerPlayerEntity p, long t) {
        boolean helmet = p.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.DIVINE_HELMET);
        boolean chest = p.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.DIVINE_CHESTPLATE);
        boolean legs = p.getEquippedStack(EquipmentSlot.LEGS).isOf(ModItems.DIVINE_LEGGINGS);
        boolean boots = p.getEquippedStack(EquipmentSlot.FEET).isOf(ModItems.DIVINE_BOOTS);

        if (t % 20 == 0) {
            if (helmet) {
                effect(p, StatusEffects.NIGHT_VISION, 0, 400);
                effect(p, StatusEffects.WATER_BREATHING, 0, 260);
            }
            if (chest) {
                effect(p, StatusEffects.STRENGTH, 1, 260);
                effect(p, StatusEffects.RESISTANCE, 1, 260);
                effect(p, StatusEffects.FIRE_RESISTANCE, 0, 260);
                effect(p, StatusEffects.REGENERATION, 0, 260);
            }
        }

        // Jambières : vitesse x2 + on monte les blocs tout seul
        toggleModifier(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, LEGS_SPEED, 1.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, legs);
        toggleModifier(p, EntityAttributes.GENERIC_STEP_HEIGHT, LEGS_STEP, 0.65, EntityAttributeModifier.Operation.ADD_VALUE, legs);

        // Bottes : le nuage magique (vol façon Sangoku)
        PlayerAbilities ab = p.getAbilities();
        boolean vanillaFlight = p.isCreative() || p.isSpectator();
        if (boots && !vanillaFlight) {
            boolean changed = false;
            if (!ab.allowFlying) { ab.allowFlying = true; changed = true; }
            if (!p.getCommandTags().contains(CLOUD_TAG)) p.addCommandTag(CLOUD_TAG);
            float speed = CLOUD_SPEEDS[cloudLevel(p) - 1];
            if (ab.getFlySpeed() != speed) { ab.setFlySpeed(speed); changed = true; }
            if (changed) p.sendAbilitiesUpdate();
            if (ab.flying && t % 2 == 0) {
                p.getServerWorld().spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY() - 0.2, p.getZ(), 6, 0.45, 0.05, 0.45, 0.0);
            }
        } else if (p.getCommandTags().contains(CLOUD_TAG)) {
            p.removeCommandTag(CLOUD_TAG);
            if (!vanillaFlight) { ab.allowFlying = false; ab.flying = false; }
            ab.setFlySpeed(0.05f);
            p.sendAbilitiesUpdate();
        }
    }

    private static int cloudLevel(PlayerEntity p) {
        return CLOUD_LEVEL.getOrDefault(p.getUuid(), 3);
    }

    private static void effect(PlayerEntity p, RegistryEntry<StatusEffect> e, int amp, int duration) {
        p.addStatusEffect(new StatusEffectInstance(e, duration, amp, true, false, true));
    }

    private static void toggleModifier(PlayerEntity p, RegistryEntry<EntityAttribute> attr, Identifier id, double value,
                                       EntityAttributeModifier.Operation op, boolean on) {
        EntityAttributeInstance inst = p.getAttributeInstance(attr);
        if (inst == null) return;
        boolean has = inst.hasModifier(id);
        if (on && !has) inst.addTemporaryModifier(new EntityAttributeModifier(id, value, op));
        else if (!on && has) inst.removeModifier(id);
    }

    // =====================================================================
    // Alliés : mobs soumis par le Regard Autoritaire + clones
    // =====================================================================
    private static void tickAllies(ServerWorld w, long t) {
        List<MobEntity> allies = new ArrayList<>();
        for (Entity e : w.iterateEntities()) {
            if (e instanceof MobEntity m && DivineUtil.isAlly(m)) allies.add(m);
        }
        for (MobEntity m : allies) {
            if (m.getCommandTags().contains(DivineUtil.CLONE_TAG)) {
                Long exp = CLONE_EXPIRY.get(m.getUuid());
                if (exp == null || t > exp) {
                    w.spawnParticles(ParticleTypes.POOF, m.getX(), m.getY() + 1, m.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
                    CLONE_EXPIRY.remove(m.getUuid());
                    m.discard();
                    continue;
                }
            }

            LivingEntity target = m.getTarget();
            if (target == null || !DivineUtil.isEvil(target)) {
                LivingEntity enemy = w.getEntitiesByClass(LivingEntity.class, m.getBoundingBox().expand(16), DivineUtil::isEvil)
                        .stream().min(Comparator.comparingDouble(e -> e.squaredDistanceTo(m))).orElse(null);
                m.setTarget(enemy);
                target = enemy;
            }

            if (target == null) {
                if (m instanceof CreeperEntity c) c.setFuseSpeed(-1);
                UUID ownerId = DivineUtil.getOwner(m);
                ServerPlayerEntity owner = ownerId == null ? null : w.getServer().getPlayerManager().getPlayer(ownerId);
                if (owner != null && owner.getWorld() == w) {
                    double d = m.squaredDistanceTo(owner);
                    if (d > 40 * 40) m.requestTeleport(owner.getX(), owner.getY(), owner.getZ());
                    else if (d > 36) m.getNavigation().startMovingTo(owner, 1.25);
                }
            }
        }
    }

    // =====================================================================
    // Touches (envoyées par le client)
    // =====================================================================
    public static void handleAction(ServerPlayerEntity p, int action) {
        switch (action) {
            case DivineActionPayload.GAZE -> gaze(p);
            case DivineActionPayload.CLONES -> clones(p);
            case DivineActionPayload.SPEED_UP -> cloudSpeed(p, 1);
            case DivineActionPayload.SPEED_DOWN -> cloudSpeed(p, -1);
            default -> { }
        }
    }

    /** Casque Divin : Regard Autoritaire. Le mob visé se soumet à toi et se bat pour toi. */
    private static void gaze(ServerPlayerEntity p) {
        if (!p.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.DIVINE_HELMET)) {
            DivineUtil.actionBar(p, "Il te faut le Casque Divin sur la tête.", Formatting.RED);
            return;
        }
        EntityHitResult hit = DivineUtil.raycastEntity(p, 48, e -> e instanceof MobEntity && !DivineUtil.isAlly(e));
        if (hit == null || !(hit.getEntity() instanceof MobEntity m)) {
            DivineUtil.actionBar(p, "Regarde un mob dans les yeux...", Formatting.YELLOW);
            return;
        }
        ServerWorld w = p.getServerWorld();
        if (m instanceof WardenEntity || m instanceof EnderDragonEntity || m instanceof WitherEntity) {
            DivineUtil.actionBar(p, "Cette créature est trop puissante pour se soumettre !", Formatting.DARK_RED);
            DivineUtil.sound(w, m, SoundEvents.ENTITY_WARDEN_ROAR, 1f, 1f);
            return;
        }
        m.addCommandTag(DivineUtil.ALLY_TAG);
        m.addCommandTag(DivineUtil.ownerTag(p.getUuid()));
        m.setPersistent();
        m.setTarget(null);
        if (m instanceof CreeperEntity c) c.setFuseSpeed(-1);
        DivineUtil.particleLine(w, ParticleTypes.ENCHANT, p.getEyePos(), m.getEyePos(), 0.4);
        w.spawnParticles(ParticleTypes.HEART, m.getX(), m.getEyeY() + 0.5, m.getZ(), 8, 0.4, 0.3, 0.4, 0.0);
        DivineUtil.sound(w, m, SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1f, 1.2f);
        p.sendMessage(Text.literal("◉ ").append(m.getDisplayName()).append(" se soumet à ton pouvoir.").formatted(Formatting.GOLD), true);
    }

    /** Jambières Divines : Multi-Clonage. 4 clones de toi qui chassent les monstres pendant 30 s. */
    private static void clones(ServerPlayerEntity p) {
        if (!p.getEquippedStack(EquipmentSlot.LEGS).isOf(ModItems.DIVINE_LEGGINGS)) {
            DivineUtil.actionBar(p, "Il te faut les Jambières Divines.", Formatting.RED);
            return;
        }
        ServerWorld w = p.getServerWorld();
        long t = w.getServer().getTicks();
        Long cd = CLONE_COOLDOWN.get(p.getUuid());
        if (cd != null && t < cd) {
            DivineUtil.actionBar(p, "Multi-Clonage rechargé dans " + ((cd - t) / 20 + 1) + " s", Formatting.YELLOW);
            return;
        }
        CLONE_COOLDOWN.put(p.getUuid(), t + 20 * 20);

        for (int i = 0; i < 4; i++) {
            ZombieEntity z = EntityType.ZOMBIE.create(w);
            if (z == null) continue;
            double a = i * Math.PI / 2;
            z.refreshPositionAndAngles(p.getX() + Math.cos(a) * 2, p.getY(), p.getZ() + Math.sin(a) * 2, p.getYaw(), 0);
            z.setBaby(false);
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponentTypes.PROFILE, new ProfileComponent(p.getGameProfile()));
            z.equipStack(EquipmentSlot.HEAD, head);
            z.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            z.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
            for (EquipmentSlot s : EquipmentSlot.values()) z.setEquipmentDropChance(s, 0f);
            z.setCanPickUpLoot(false);
            z.setCustomName(p.getName().copy().formatted(Formatting.GOLD));
            z.setCustomNameVisible(true);
            z.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 20 * 40, 0, false, false));
            z.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 40, 1, false, false));
            z.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 40, 2, false, false));
            z.addCommandTag(DivineUtil.ALLY_TAG);
            z.addCommandTag(DivineUtil.CLONE_TAG);
            z.addCommandTag(DivineUtil.ownerTag(p.getUuid()));
            w.spawnEntity(z);
            CLONE_EXPIRY.put(z.getUuid(), t + 20 * 30);
            w.spawnParticles(ParticleTypes.POOF, z.getX(), z.getY() + 1, z.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
        }
        DivineUtil.sound(w, p, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 1f);
        DivineUtil.actionBar(p, "✦ Multi-Clonage !", Formatting.GOLD);
    }

    /** Bottes Divines : réglage de la vitesse du nuage. */
    private static void cloudSpeed(ServerPlayerEntity p, int delta) {
        if (!p.getEquippedStack(EquipmentSlot.FEET).isOf(ModItems.DIVINE_BOOTS)) return;
        int lvl = Math.max(1, Math.min(CLOUD_SPEEDS.length, cloudLevel(p) + delta));
        CLOUD_LEVEL.put(p.getUuid(), lvl);
        DivineUtil.actionBar(p, "☁ Vitesse du nuage : " + lvl + "/" + CLOUD_SPEEDS.length, Formatting.AQUA);
    }

    // =====================================================================
    // Dégâts / mort
    // =====================================================================
    public static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
        if (victim instanceof PlayerEntity p && p.getEquippedStack(EquipmentSlot.FEET).isOf(ModItems.DIVINE_BOOTS)
                && (source.isIn(DamageTypeTags.IS_FALL) || source.isOf(DamageTypes.FLY_INTO_WALL))) {
            return false;
        }
        Entity attacker = source.getAttacker();
        if (attacker != null && DivineUtil.isAlly(attacker)) {
            UUID owner = DivineUtil.getOwner(attacker);
            if (owner != null && (victim.getUuid().equals(owner) || owner.equals(DivineUtil.getOwner(victim)))) {
                return false;
            }
        }
        return true;
    }

    /** Pomme Divine : Bénédiction du Phénix, tu renais avec deux barres de cœurs. */
    public static boolean allowDeath(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayerEntity p) || !p.getCommandTags().contains(DivineUtil.PHOENIX_TAG)) return true;
        p.removeCommandTag(DivineUtil.PHOENIX_TAG);
        p.clearStatusEffects();
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 20 * 60 * 5, 4));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 10, 2));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 20 * 60 * 2, 0));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 20 * 60, 3));
        p.setHealth(p.getMaxHealth());

        ServerWorld w = p.getServerWorld();
        w.sendEntityStatus(p, (byte) 35);
        DivineUtil.lightning(w, p.getPos());
        w.spawnParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 150, 1.2, 1.5, 1.2, 0.08);
        w.spawnParticles(ParticleTypes.LAVA, p.getX(), p.getY() + 1, p.getZ(), 30, 1.0, 1.0, 1.0, 0.0);
        for (int i = 0; i < 20; i++) {
            double s = i * 0.25;
            w.spawnParticles(ParticleTypes.FLAME, p.getX() + s, p.getY() + 1.5 + s * 0.4, p.getZ(), 2, 0.05, 0.05, 0.05, 0.0);
            w.spawnParticles(ParticleTypes.FLAME, p.getX() - s, p.getY() + 1.5 + s * 0.4, p.getZ(), 2, 0.05, 0.05, 0.05, 0.0);
        }
        DivineUtil.sound(w, p, SoundEvents.ENTITY_BLAZE_SHOOT, 1.5f, 0.6f);
        p.sendMessage(Text.literal("✹ Le Phénix te ramène à la vie !").formatted(Formatting.GOLD, Formatting.BOLD), false);
        return false;
    }
}
