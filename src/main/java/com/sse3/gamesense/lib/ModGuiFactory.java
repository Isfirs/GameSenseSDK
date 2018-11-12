package com.sse3.gamesense.lib;

import com.sse3.gamesense.GameSenseMod;
import com.sse3.gamesense.config.ModConfig;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.DefaultGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.List;

public class ModGuiFactory extends DefaultGuiFactory
{

    public ModGuiFactory()
    {
        super(GameSenseMod.MODID, GameSenseMod.MODNAME);
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen)
    {
        return new GuiConfig(parentScreen, getConfigElements(), GameSenseMod.MODID, false, false, this.title);
    }

    private static List<IConfigElement> getConfigElements()
    {
        Configuration config = ModConfig.getConfig();
        List<IConfigElement> list = new ConfigElement(config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements();
        return list;
    }

}