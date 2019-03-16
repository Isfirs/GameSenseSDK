package com.sse3.gamesense.config;

import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;

public class GameSenseModConfig
{
    public static BooleanValue modEnabled;
    public static BooleanValue CheckForUpdates;

    public static void init(Builder CLIENT_BUILDER)
    {
        CLIENT_BUILDER
                .comment("GameSenseMod for Minecraft 1.13.2",
                        "by JayJay1989BE",
                        "General Gamesense configuration")
                .push("gamesense");

        modEnabled = CLIENT_BUILDER
                .comment("Mod Enabled [false/true|default:true]")
                .define("modEnabled", true);
        CheckForUpdates = CLIENT_BUILDER
                .comment("Check For Updates [false/true|default:true]")
                .define("checkForUpdates", true);
    }
}
