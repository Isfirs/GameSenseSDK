package com.sse3.gamesense.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.sse3.gamesense.GameSenseMod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import java.nio.file.Path;

import static net.minecraftforge.fml.Logging.CORE;

@Mod.EventBusSubscriber
public class Config
{

    private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec CLIENT_CONFIG;

    static
    {
        GameSenseModConfig.init(CLIENT_BUILDER);
        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }


    public static void loadConfig(ForgeConfigSpec spec, Path path)
    {
        GameSenseMod.logger.debug("Loading config file {}", path);

        final CommentedFileConfig configData = CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build();

        GameSenseMod.logger.debug("Built TOML config for {}", path.toString());
        configData.load();
        GameSenseMod.logger.debug("Loaded TOML config file {}", path.toString());
        spec.setConfig(configData);
    }

    @SubscribeEvent
    public static void onLoad(final ModConfig.Loading configEvent)
    {
        GameSenseMod.logger.debug("Loaded {} config file {}", GameSenseMod.MODID, configEvent.getConfig().getFileName());

    }

    @SubscribeEvent
    public static void onFileChange(final ModConfig.ConfigReloading configEvent)
    {
        GameSenseMod.logger.fatal(CORE, "{} config just got changed on the file system!", GameSenseMod.MODID);
    }

}