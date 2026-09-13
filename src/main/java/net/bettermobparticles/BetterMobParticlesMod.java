package net.bettermobparticles;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.bettermobparticles.config.BetterMobParticlesConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spawns fitting particles whenever a living entity takes damage - from any source. This mod does
 * not know or care what dealt the damage: an arrow, a sword, a modded weapon, another mob's claws.
 * Everything is derived from vanilla's own {@code DamageSource}, so it works standalone with no
 * other mod installed, including on plain vanilla arrows.
 *
 * <p>The trade-off for that independence: a projectile's exact impact point is gone by the time
 * {@code AFTER_DAMAGE} fires (vanilla discards it right after the hit, and this mod does not hook
 * the projectile's own code the way a dedicated arrow mod could), so the wound position for a
 * ranged hit is an approximation from the projectile's own lingering position, not the precise
 * point the tip touched.
 */
public class BetterMobParticlesMod implements ModInitializer {
	public static final String MOD_ID = "bettermobparticles";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * Registered as an ordinary particle type with the mod's own texture: a mod's
	 * {@code textures/particle} entries join the particle atlas automatically. (Drawing particles
	 * from an ENTITY's own texture, rather than a fixed sprite, is the thing that is not possible -
	 * entity textures are not on that atlas, and {@code ParticleRenderType} is a closed set of four
	 * values.)
	 */
	public static final SimpleParticleType BLOOD = Registry.register(
			BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "blood"), FabricParticleTypes.simple());

	/**
	 * What a mob throws off when it is wounded. Everything not in one of these tags bleeds red.
	 * Tags rather than a hardcoded list so datapacks (and other mods' mobs) can be corrected
	 * without touching code. See {@link WoundParticles}.
	 */
	public static final TagKey<EntityType<?>> DOES_NOT_BLEED = entityTag("does_not_bleed");
	public static final TagKey<EntityType<?>> BLEEDS_FIRE = entityTag("bleeds_fire");
	public static final TagKey<EntityType<?>> BLEEDS_BONE = entityTag("bleeds_bone");
	public static final TagKey<EntityType<?>> BLEEDS_SLIME = entityTag("bleeds_slime");
	public static final TagKey<EntityType<?>> BLEEDS_METAL = entityTag("bleeds_metal");
	public static final TagKey<EntityType<?>> BLEEDS_SNOW = entityTag("bleeds_snow");
	public static final TagKey<EntityType<?>> BLEEDS_COPPER = entityTag("bleeds_copper");
	public static final TagKey<EntityType<?>> BLEEDS_WHITE = entityTag("bleeds_white");
	public static final TagKey<EntityType<?>> BLEEDS_AIR = entityTag("bleeds_air");
	public static final TagKey<EntityType<?>> BLEEDS_SULFUR = entityTag("bleeds_sulfur");
	public static final TagKey<EntityType<?>> BLEEDS_WOOD = entityTag("bleeds_wood");

	/** Weapons and tools that draw blood outright; anything else only sometimes does. */
	public static final TagKey<Item> DRAWS_BLOOD = TagKey.create(
			Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "draws_blood"));

	private static TagKey<EntityType<?>> entityTag(String name) {
		return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, name));
	}

	private static final Set<String> LOGGED_ONCE = ConcurrentHashMap.newKeySet();

	/**
	 * Logs a failure the first time it is seen and stays silent afterwards. This code runs on
	 * every hit in the game, so an unguarded log of a recurring failure would itself become the
	 * problem it is reporting.
	 */
	public static void logOnce(String message, Throwable error) {
		if (LOGGED_ONCE.add(message)) {
			LOGGER.error("[{}] {} (further occurrences suppressed)", MOD_ID, message, error);
		}
	}

	@Override
	public void onInitialize() {
		BetterMobParticlesConfig.load();

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			try {
				spawnWound(entity, source, damageTaken, blocked);
			} catch (Exception e) {
				logOnce("Failed to spawn wound particles", e);
			}
		});
		// AFTER_DAMAGE is guarded by "if (!isDeadOrDying())" inside Fabric itself, so the killing
		// blow - the one hit that most deserves particles - never reaches it. AFTER_DEATH is the
		// other half of the same story.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			try {
				spawnWound(entity, source, 1.0F, false);
			} catch (Exception e) {
				logOnce("Failed to spawn wound particles on a killing blow", e);
			}
		});

		LOGGER.info("Better Mob Particles initialized");
	}

	private static void spawnWound(LivingEntity target, DamageSource source, float damageTaken, boolean blocked) {
		if (blocked || damageTaken <= 0.0F || !(target.level() instanceof ServerLevel level)) {
			return;
		}
		BetterMobParticlesConfig config = BetterMobParticlesConfig.get();
		if (!config.enabled) {
			return;
		}

		// Fire, fall damage, drowning, magic and the like have no entity to derive a wound
		// direction from - only a projectile or a direct attacker gives a sensible one.
		Entity direct = source.getDirectEntity();
		if (direct instanceof Projectile projectile) {
			spawnFromProjectile(level, target, projectile);
		} else if (direct instanceof LivingEntity attacker && attacker != target) {
			spawnFromMelee(level, target, attacker, config);
		}
	}

	/**
	 * An arrow, a trident, or any modded projectile. A weapon is always "sharp" - no barehand
	 * chance applies here.
	 */
	private static void spawnFromProjectile(ServerLevel level, LivingEntity target, Projectile projectile) {
		// The exact impact point is gone by the time this event fires - vanilla discards it right
		// after the hit, and this mod deliberately does not hook the projectile's own code, so it
		// keeps working on plain vanilla arrows with nothing else installed. The projectile's own
		// position, which lingers here for a tick after landing, is the closest approximation
		// available without that hook.
		Vec3 velocity = projectile.getDeltaMovement();
		// Thrown back along the flight path rather than forward: the spray comes out of the
		// wound, not out the far side of the target.
		Vec3 direction = velocity.lengthSqr() > 1.0E-8 ? velocity.normalize().scale(-1.0) : new Vec3(0.0, 1.0, 0.0);
		WoundParticles.spawn(level, target, projectile.position(), direction.add(0.0, 0.05, 0.0));
	}

	private static void spawnFromMelee(ServerLevel level, LivingEntity target, LivingEntity attacker, BetterMobParticlesConfig config) {
		if (!config.melee.enabled) {
			return;
		}
		// A blade or a tool opens a wound every time; a fist, a block or a torch only sometimes,
		// and less of one.
		boolean sharp = attacker.getMainHandItem().is(DRAWS_BLOOD);
		float intensity = 1.0F;
		if (!sharp) {
			if (level.getRandom().nextFloat() >= config.melee.barehandChance) {
				return;
			}
			intensity = 0.4F;
		}

		// Melee has no exact impact point the way a projectile does, so the wound is placed on
		// the side of the target facing the attacker, around chest height.
		Vec3 toTarget = target.position().subtract(attacker.position());
		Vec3 facing = new Vec3(toTarget.x, 0.0, toTarget.z);
		Vec3 outward = facing.lengthSqr() > 1.0E-6 ? facing.normalize() : new Vec3(0.0, 0.0, 1.0);
		Vec3 at = target.position()
				.add(0.0, target.getBbHeight() * 0.6, 0.0)
				.subtract(outward.scale(target.getBbWidth() * 0.4));

		WoundParticles.spawn(level, target, at, outward.scale(-0.12).add(0.0, 0.05, 0.0), intensity);
	}
}
