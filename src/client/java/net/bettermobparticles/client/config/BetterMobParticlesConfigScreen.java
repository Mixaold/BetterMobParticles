package net.bettermobparticles.client.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.bettermobparticles.config.BetterMobParticlesConfig;

/**
 * The settings screen, reached from Mod Menu.
 *
 * <p>Values are written straight back into the live config object, so a change takes effect the
 * moment it is saved. Particles are spawned server-side (so every nearby player sees the same
 * thing), so on a dedicated server it is the SERVER's copy of this file that decides whether they
 * spawn at all - editing this screen on a client only changes that client's own rendering details.
 */
public final class BetterMobParticlesConfigScreen {
	private BetterMobParticlesConfigScreen() {
	}

	public static Screen create(Screen parent) {
		BetterMobParticlesConfig config = BetterMobParticlesConfig.get();

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(title("title"))
				.setSavingRunnable(BetterMobParticlesConfig::save);
		ConfigEntryBuilder entry = builder.entryBuilder();

		ConfigCategory main = builder.getOrCreateCategory(title("category.main"));
		main.addEntry(entry.startBooleanToggle(title("enabled"), config.enabled)
				.setDefaultValue(true)
				.setTooltip(tooltip("enabled"))
				.setSaveConsumer(v -> config.enabled = v)
				.build());
		main.addEntry(entry.startIntSlider(title("particleCount"), config.particleCount, 0, 32)
				.setDefaultValue(12)
				.setSaveConsumer(v -> config.particleCount = v)
				.build());
		main.addEntry(entry.startIntSlider(title("lifetimeTicks"), config.lifetimeTicks, 5, 200)
				.setDefaultValue(55)
				.setTooltip(tooltip("lifetimeTicks"))
				.setSaveConsumer(v -> config.lifetimeTicks = v)
				.build());
		main.addEntry(entry.startIntSlider(title("force"), Math.round(config.force * 100.0F), 0, 100)
				.setDefaultValue(30)
				.setTooltip(tooltip("force"))
				.setSaveConsumer(v -> config.force = v / 100.0F)
				.build());

		ConfigCategory melee = builder.getOrCreateCategory(title("category.melee"));
		melee.addEntry(entry.startBooleanToggle(title("meleeEnabled"), config.melee.enabled)
				.setDefaultValue(true)
				.setTooltip(tooltip("meleeEnabled"))
				.setSaveConsumer(v -> config.melee.enabled = v)
				.build());
		melee.addEntry(entry.startIntSlider(title("barehandChance"), Math.round(config.melee.barehandChance * 100.0F), 0, 100)
				.setDefaultValue(33)
				.setTooltip(tooltip("barehandChance"))
				.setSaveConsumer(v -> config.melee.barehandChance = v / 100.0F)
				.build());

		return builder.build();
	}

	private static Component title(String key) {
		return Component.translatable("bettermobparticles.config." + key);
	}

	private static Component[] tooltip(String key) {
		return new Component[] { Component.translatable("bettermobparticles.config." + key + ".tooltip") };
	}
}
