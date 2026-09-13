package net.bettermobparticles.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.bettermobparticles.BetterMobParticlesMod;
import net.bettermobparticles.client.particle.BloodParticle;

public class BetterMobParticlesModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Sprites come from assets/bettermobparticles/particles/blood.json, loaded onto the
		// particle atlas the same way vanilla's own particles are.
		ParticleProviderRegistry.getInstance().register(BetterMobParticlesMod.BLOOD, BloodParticle.Provider::new);

		BetterMobParticlesMod.LOGGER.info("Better Mob Particles client initialized");
	}
}
