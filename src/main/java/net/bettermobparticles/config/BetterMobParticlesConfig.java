package net.bettermobparticles.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.bettermobparticles.BetterMobParticlesMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Read once at startup and never reloaded: this is consulted on every hit, so re-reading a file
 * there is out of the question.
 *
 * <p>Spawned server-side, so every nearby player sees the same particles - this config is read by
 * the SERVER for gameplay-visible things (whether particles spawn at all, how many) and by the
 * CLIENT only for how the droplet itself looks and behaves once spawned. In singleplayer both come
 * from the same file.
 */
public final class BetterMobParticlesConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = BetterMobParticlesMod.MOD_ID + ".json";

	private static BetterMobParticlesConfig instance = new BetterMobParticlesConfig();

	public boolean enabled = true;
	/**
	 * Mobs that should not bleed (skeletons, golems, slimes, ...) - and what they throw off
	 * instead (sparks for a blaze, sulfur for a sulfur cube, and so on) - are chosen by entity type
	 * tags under {@code bettermobparticles:}, which a datapack can override.
	 */
	public int particleCount = 12;
	/** Client-side. 55 ticks is a shade under 3 seconds. */
	public int lifetimeTicks = 55;
	/**
	 * How hard droplets are thrown out of the wound, in blocks per tick. Low: blood should well
	 * out and drop, not fire across the room.
	 */
	public float force = 0.30F;
	public Melee melee = new Melee();

	public static BetterMobParticlesConfig get() {
		return instance;
	}

	/** Particles from a weapon swing, not just from an arrow or trident. */
	public static final class Melee {
		public boolean enabled = true;
		/**
		 * Chance of drawing blood with something that is not a tool or weapon - a bare fist, a
		 * block, a torch. Not zero, because a punch that opens nothing at all feels inert, but far
		 * from certain, because a fist is not a blade.
		 */
		public float barehandChance = 0.33F;
	}

	/**
	 * Loads the config, writing a fully-populated file the first time. Any failure leaves the
	 * defaults in place rather than stopping the mod: a bad config file should cost you your
	 * customisation, not your game.
	 */
	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		try {
			if (Files.exists(path)) {
				try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
					BetterMobParticlesConfig loaded = GSON.fromJson(reader, BetterMobParticlesConfig.class);
					if (loaded != null) {
						loaded.fillMissingSections();
						loaded.clampToUsableRanges();
						instance = loaded;
					}
				}
			}
			save(path);
		} catch (Exception e) {
			// Includes a malformed file (JsonSyntaxException) and an unwritable config directory.
			BetterMobParticlesMod.logOnce("Failed to load " + FILE_NAME + ", using defaults", e);
		}
	}

	/** Writes the current values back out - used by the settings screen after an edit. */
	public static void save() {
		try {
			save(FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME));
		} catch (Exception e) {
			BetterMobParticlesMod.logOnce("Failed to save " + FILE_NAME, e);
		}
	}

	private static void save(Path path) throws IOException {
		Files.createDirectories(path.getParent());
		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			GSON.toJson(instance, writer);
		}
	}

	/**
	 * Gson leaves a field null when its key is absent, so a config written by an older version -
	 * or one a user trimmed by hand - would otherwise hand out a null section and NPE on the first
	 * hit.
	 */
	private void fillMissingSections() {
		if (this.melee == null) {
			this.melee = new Melee();
		}
	}

	/**
	 * Hand-edited values are not to be trusted. A negative particle count or a force so high
	 * droplets fire across the room would each turn a typo into a broken-looking game.
	 */
	private void clampToUsableRanges() {
		this.particleCount = clamp(this.particleCount, 0, 64);
		this.lifetimeTicks = clamp(this.lifetimeTicks, 1, 600);
		this.force = clamp(this.force, 0.0F, 1.0F);
		this.melee.barehandChance = clamp(this.melee.barehandChance, 0.0F, 1.0F);
	}

	private static float clamp(float value, float min, float max) {
		if (!Float.isFinite(value)) {
			return min;
		}
		return value < min ? min : Math.min(value, max);
	}

	private static int clamp(int value, int min, int max) {
		return value < min ? min : Math.min(value, max);
	}
}
