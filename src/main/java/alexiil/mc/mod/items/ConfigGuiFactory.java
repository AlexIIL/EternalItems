package alexiil.mc.mod.items;

import java.util.Collections;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;

public class ConfigGuiFactory implements IModGuiFactory {
    @Override
    public void initialize(Minecraft minecraftInstance) {}

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return GuiConfigEternalItems.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return Collections.emptySet();
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }

    public static class GuiConfigEternalItems extends GuiConfig {
        public GuiConfigEternalItems(GuiScreen parent) {
            super(parent, EternalItems.getConfigElements(), Lib.Mod.ID, false, false, EternalItems.cfg.toString());
        }
    }
}
