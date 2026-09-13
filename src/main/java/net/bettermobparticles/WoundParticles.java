package net.bettermobparticles;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.bettermobparticles.config.BetterMobParticlesConfig;
import org.jspecify.annotations.Nullable;

/**
 * Picks and spawns the "wound" particles for a hit on a living entity, from any source of damage.
 *
 * <p>Not everything bleeds red, and spraying blood off a skeleton looks worse than spraying nothing.
 * What a given mob throws off is chosen by entity type tags rather than a hardcoded list, so a
 * datapack can correct this mod's guesses and cover modded mobs without a code change. Everything
 * except actual blood reuses a vanilla particle or a vanilla block's own texture, so no extra
 * textures are needed beyond the blood droplet itself.
 */
public final class WoundParticles {
	private WoundParticles() {
	}

	/**
	 * @param scatter width of the cone, as a FRACTION of the throw force - an absolute value makes
	 *                a hard throw come out as a thin line and a soft one as a shapeless blob
	 * @param countScale how much of the configured droplet count this effect uses
	 * @param forceScale multiplies the configured throw force for this effect specifically, so one
	 *                    mob can be tuned weaker or stronger without touching the global slider
	 * @param risesUp ignores the caller's wound direction and always throws straight up - for
	 *                 things that are not "bleeding" from a wound at all (a breeze's air burst)
	 */
	private record Effect(ParticleOptions particle, double scatter, float countScale, float forceScale, boolean risesUp) {
		Effect(ParticleOptions particle, double scatter, float countScale) {
			this(particle, scatter, countScale, 1.0F, false);
		}
	}

	private static final Effect BLOOD = new Effect(BetterMobParticlesMod.BLOOD, 0.55, 1.0F);
	// Blaze: noticeably toned down (forceScale + smaller cone + fewer particles) - a full-strength
	// blood-sized burst read as far too aggressive for what should be a small puff of embers.
	private static final Effect FIRE = new Effect(ParticleTypes.FLAME, 0.40, 0.35F, 0.5F, false);
	private static final Effect EMBER = new Effect(ParticleTypes.LAVA, 0.30, 0.20F, 0.5F, false);
	private static final Effect BONE = new Effect(
			new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()), 0.50, 0.8F);
	private static final Effect SLIME = new Effect(ParticleTypes.ITEM_SLIME, 0.60, 1.0F);
	// Iron golem: bigger and wider than the other "block crumb" effects - a mob that size deserves
	// a more noticeable clang, not the same handful of specks a zombie's helmet would shed.
	private static final Effect METAL = new Effect(
			new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()), 0.85, 1.0F, 1.15F, false);
	private static final Effect METAL_SPARK = new Effect(ParticleTypes.CRIT, 0.9, 0.4F, 1.15F, false);
	private static final Effect SNOW = new Effect(ParticleTypes.SNOWFLAKE, 0.75, 1.0F);
	private static final Effect COPPER = new Effect(
			new BlockParticleOption(ParticleTypes.BLOCK, Blocks.RAW_COPPER_BLOCK.defaultBlockState()), 0.65, 0.7F);
	// Ghast / Happy Ghast: a gas-and-cloud creature, not flesh - white ash instead of red droplets.
	private static final Effect WHITE = new Effect(ParticleTypes.WHITE_ASH, 0.60, 1.0F);
	// Breeze: not a wound at all - a puff of air, always thrown straight up regardless of which
	// way the hit came from. Deliberately SMALL_GUST, not the plain GUST vanilla uses for a wind
	// charge's actual attack - that one is sized and paced for a dramatic single burst, and looks
	// oversized and out of place fired repeatedly as wound debris.
	private static final Effect AIR = new Effect(ParticleTypes.SMALL_GUST, 0.6, 0.6F, 0.8F, true);
	private static final Effect SULFUR = new Effect(
			new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SULFUR.defaultBlockState()), 0.55, 1.0F);
	// Creaking: a walking, grey-barked construct - its own heart block's texture reads as the
	// closest vanilla match for "wood, but the color of this specific mob".
	private static final Effect WOOD = new Effect(
			new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CREAKING_HEART.defaultBlockState()), 0.55, 0.8F);

	/**
	 * @param spray direction the debris should be thrown in, normally back out along whatever hit
	 *              the entity; length is ignored - see {@link BetterMobParticlesConfig#force}
	 */
	public static void spawn(ServerLevel level, LivingEntity target, Vec3 at, Vec3 spray) {
		spawn(level, target, at, spray, 1.0F);
	}

	/**
	 * @param intensity scales how much comes out - a bare fist opens far less than a blade
	 */
	public static void spawn(ServerLevel level, LivingEntity target, Vec3 at, Vec3 spray, float intensity) {
		BetterMobParticlesConfig config = BetterMobParticlesConfig.get();
		if (!config.enabled || config.particleCount <= 0) {
			return;
		}
		if (!isFinite(at) || !isFinite(spray)) {
			return;
		}

		Effect effect = effectFor(target);
		if (effect == null) {
			return;
		}

		// Callers pass a direction; how hard it is thrown is one setting, in one place, so tuning
		// the look does not mean hunting down a magic number at every call site.
		Vec3 direction = effect.risesUp()
				? new Vec3(0.0, 1.0, 0.0)
				: (spray.lengthSqr() > 1.0E-8 ? spray.normalize() : new Vec3(0.0, 1.0, 0.0));

		int count = Math.max(1, Math.round(config.particleCount * intensity));
		emit(level, effect, count, at, direction, config.force);
		if (effect == FIRE) {
			// A few heavier embers among the flames so a blaze hit reads as sparks knocked loose
			// rather than as the mob simply being on fire.
			emit(level, EMBER, count, at, direction, config.force);
		} else if (effect == METAL) {
			// A couple of bright sparks alongside the crumbs, the same "two effects at once" trick
			// used for fire above - iron-on-iron should flash, not just shed dust.
			emit(level, METAL_SPARK, count, at, direction, config.force);
		}
	}

	/**
	 * Emits droplet by droplet with a count of ZERO, which is what makes the spray directional.
	 *
	 * <p>This is the one genuinely counter-intuitive bit of the vanilla particle packet: with a
	 * count above zero the three "offset" numbers are read as a POSITION spread and each particle
	 * is given a random gaussian velocity in every direction, so the wound direction is thrown away
	 * and everything flies apart at full speed. With a count of zero those same three numbers are
	 * the velocity itself. Sending one packet per droplet is the price of blood that actually comes
	 * out of the wound the way the blade went in.
	 */
	private static void emit(ServerLevel level, Effect effect, int baseCount, Vec3 at, Vec3 direction, float baseForce) {
		int count = Math.max(1, Math.round(baseCount * effect.countScale()));
		RandomSource random = level.getRandom();
		Vec3 spray = direction.scale(baseForce * effect.forceScale());
		// Cone width scales with the throw, so turning the force up widens the spray instead of
		// stretching it into a thin line.
		double cone = spray.length() * effect.scatter();
		for (int i = 0; i < count; i++) {
			// Each droplet also gets its own share of the force - a real spurt has fast leaders
			// and slow stragglers, not one uniform speed.
			Vec3 velocity = spray.scale(0.55 + random.nextDouble() * 0.9).add(
					(random.nextDouble() - 0.5) * cone,
					(random.nextDouble() - 0.5) * cone,
					(random.nextDouble() - 0.5) * cone);
			level.sendParticles(
					effect.particle(),
					at.x + (random.nextDouble() - 0.5) * 0.1,
					at.y + (random.nextDouble() - 0.5) * 0.1,
					at.z + (random.nextDouble() - 0.5) * 0.1,
					0,
					velocity.x, velocity.y, velocity.z,
					1.0);
		}
	}

	private static @Nullable Effect effectFor(LivingEntity target) {
		if (target.is(BetterMobParticlesMod.DOES_NOT_BLEED)) {
			return null;
		}
		if (target.is(BetterMobParticlesMod.BLEEDS_FIRE)) {
			return FIRE;
		}
		if (target.is(BetterMobParticlesMod.BLEEDS_BONE)) {
			return BONE;
		}
		if (target.is(BetterMobParticlesMod.BLEEDS_SLIME)) {
			return SLIME;
		}
		if (target.is(BetterMobParticlesMod.BLEEDS_METAL)) {
			return METAL;
		}
		if (target.is(BetterMobParticlesMod.BLEEDS_COPPER)) {
			return COPPER;
		}
		if (target.is(BetterMobParticlesMod.BLEEDS_SNOW)) {
			return SNOW;
		}
		if (target.is(BetterMobParticlesMod.BLEEDS_WHITE)) {
			return WHITE;
		}
		if (target.is(BetterMobParticlesMod.BLEEDS_AIR)) {
			return AIR;
		}
		if (target.is(BetterMobParticlesMod.BLEEDS_SULFUR)) {
			return SULFUR;
		}
		if (target.is(BetterMobParticlesMod.BLEEDS_WOOD)) {
			return WOOD;
		}
		return BLOOD;
	}

	private static boolean isFinite(Vec3 v) {
		return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
	}
}
