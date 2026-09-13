package net.bettermobparticles.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.bettermobparticles.config.BetterMobParticlesConfig;

/**
 * A droplet of blood thrown off by a wound. Falls under gravity, collides with the world so it
 * comes to rest on whatever is below, and fades out rather than blinking away.
 *
 * <p>This is an ordinary particle with the mod's own texture, which is the supported way to do
 * this: a mod's {@code textures/particle} entries are added to the particle atlas automatically.
 * The thing that genuinely cannot be done is sampling an ENTITY's texture for the particle -
 * entity textures never reach that atlas, and {@code ParticleRenderType} is a closed set.
 */
public class BloodParticle extends SingleQuadParticle {
	/** Fraction of the lifetime spent fading out at the end. */
	private static final float FADE_FRACTION = 0.35F;

	private BloodParticle(ClientLevel level, double x, double y, double z, double xa, double ya, double za, SpriteSet sprites) {
		super(level, x, y, z, sprites.get(level.getRandom()));

		// Velocity is taken exactly as given: the server aimed this spray along the wound's
		// direction, and Particle's own velocity constructor would randomise that away.
		this.xd = xa;
		this.yd = ya;
		this.zd = za;

		this.gravity = 1.0F;
		// Only slightly below the 0.98 default. Anything much lower (0.82 was tried) kills the
		// throw within about ten ticks, so the spray never travels and just dribbles downward.
		this.friction = 0.95F;
		this.hasPhysics = true;
		// Smaller than the 0.2 default so a droplet settles against the surface it lands on
		// rather than floating half a block clear of it - but not as tiny as first tried (0.04):
		// that let droplets sink into partial-height blocks (snow layers especially) instead of
		// resting on their visible top, see the top-surface snap in tick() for the belt-and-braces
		// fix that guarantees visibility regardless of collision-box size.
		this.setSize(0.08F, 0.08F);
		this.quadSize *= 0.55F;

		int configured = BetterMobParticlesConfig.get().lifetimeTicks;
		// Spread the lifetimes a little so a spray does not vanish all at once.
		this.lifetime = configured + this.random.nextInt(Math.max(1, configured / 4));
	}

	@Override
	public SingleQuadParticle.Layer getLayer() {
		return SingleQuadParticle.Layer.TRANSLUCENT;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.removed) {
			return;
		}
		if (this.onGround) {
			snapOntoSurface();
		}
		if (this.lifetime <= 0) {
			return;
		}
		float progress = (float) this.age / this.lifetime;
		float fadeStart = 1.0F - FADE_FRACTION;
		if (progress > fadeStart) {
			this.setAlpha(Math.max(0.0F, (1.0F - progress) / FADE_FRACTION));
		}
	}

	/**
	 * Snaps the droplet up to the block's VISUAL top, deliberately not its collision shape.
	 *
	 * <p>A single layer of fresh snow is the case that forced this: vanilla gives {@code layers=1}
	 * an almost-zero-height COLLISION shape on purpose (that thinnest layer is meant to be walked
	 * over like bare ground), while its drawn shape is a real 1/8-block-tall slab. Our particle's
	 * physics faithfully follows that collision shape and settles on the dirt underneath - which
	 * is correct physics and a wrong picture, since the droplet then renders visibly buried under
	 * the snow everyone can see. Re-deriving the position from the render shape instead of the
	 * physics shape fixes exactly that mismatch, and costs nothing for the vast majority of blocks
	 * where the two shapes already agree.
	 */
	private void snapOntoSurface() {
		BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
		VoxelShape shape = this.level.getBlockState(pos).getShape(this.level, pos);
		if (shape.isEmpty()) {
			return;
		}
		double top = pos.getY() + shape.max(Direction.Axis.Y);
		if (this.y < top) {
			this.setPos(this.x, top, this.z);
		}
	}

	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(
				SimpleParticleType options, ClientLevel level,
				double x, double y, double z,
				double xa, double ya, double za,
				RandomSource random) {
			return new BloodParticle(level, x, y, z, xa, ya, za, this.sprites);
		}
	}
}
