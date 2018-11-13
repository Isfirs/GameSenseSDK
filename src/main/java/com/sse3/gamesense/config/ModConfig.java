package com.sse3.gamesense.config;

import com.sse3.gamesense.GameSenseMod;
import com.sse3.gamesense.lib.ModGuiConfigEntries;

import net.minecraft.client.Minecraft;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModConfig
{

    private static class DefaultValues
    {
        private static final boolean modEnabled = true;
        private static final boolean CheckForUpdates = true;

    }


    private static Configuration config = null;

    public static boolean modEnabled = DefaultValues.modEnabled;
    public static boolean CheckForUpdates = DefaultValues.CheckForUpdates;


        public static void initializeConfiguration()
        {
            File configFile = new File(Loader.instance().getConfigDir(), GameSenseMod.MODID + ".cfg");
            config = new Configuration(configFile);
            config.load();
            syncConfig(true, true);
        }


        public static Configuration getConfig()
        {
            return config;
        }


        public static void syncConfig(boolean loadConfigFromFile, boolean readFieldsFromConfig)
        {
            if (loadConfigFromFile) config.load();

            Property propModEnabled = config.get(Configuration.CATEGORY_GENERAL, "modEnabled", DefaultValues.modEnabled, "");
            propModEnabled.setLanguageKey("gamesense.options.modenabled");
            propModEnabled.setRequiresMcRestart(true);

            Property propCheckForUpdates = config.get(Configuration.CATEGORY_GENERAL, "CheckForUpdates", DefaultValues.CheckForUpdates, "");
            propCheckForUpdates.setLanguageKey("gamesense.options.checkforupdates");
            propCheckForUpdates.setRequiresMcRestart(true);

            try
            {
                propModEnabled.setConfigEntryClass(ModGuiConfigEntries.BooleanEntry.class);
                propCheckForUpdates.setConfigEntryClass(ModGuiConfigEntries.BooleanEntry.class);

                List<String> propOrderGeneral = new ArrayList<>();
                propOrderGeneral.add(propModEnabled.getName());
                propOrderGeneral.add(propCheckForUpdates.getName());
                config.setCategoryPropertyOrder(Configuration.CATEGORY_GENERAL, propOrderGeneral);

            }
            catch (NoClassDefFoundError e)
            {
            }


            if (readFieldsFromConfig)
            {
                modEnabled = propModEnabled.getBoolean();
                CheckForUpdates = propCheckForUpdates.getBoolean();
            }

            propModEnabled.set(modEnabled);
            propCheckForUpdates.set(CheckForUpdates);

            if (config.hasChanged()) config.save();
        }


        @Mod.EventBusSubscriber
        public static class ConfigEventHandler
        {
            @SubscribeEvent
            public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
            {
                if (event.getModID().equals(GameSenseMod.MODID))
                {
                    if (!event.isWorldRunning() || Minecraft.getMinecraft().isSingleplayer())
                    {
                        ModConfig.syncConfig(false, true);
                    }
                }
            }
        }

    }