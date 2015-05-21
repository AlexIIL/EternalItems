package alexiil.mods.items;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;

import alexiil.mods.lib.AlexIILMod;

@Mod(modid = Lib.Mod.ID, guiFactory = "alexiil.mods.items.ConfigGuiFactory", dependencies = "required-after:alexiillib",
        acceptableRemoteVersions = "*")
public class EternalItems extends AlexIILMod {
    @Instance(Lib.Mod.ID)
    public static EternalItems INSTANCE;
    private static int maxItems;
    private static boolean hardCap;

    @Override
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
        new ItemCacheHandler();

        String comment = "The maximum number of items allowed per world before new ones start to be cached.";
        maxItems = cfg.cfg().getInt("maxItems", Configuration.CATEGORY_GENERAL, 200, 20, 6000, comment);

        comment =
            "True if you want the items added to the world to be hard capped to the number "
                + "(but the items that expire in the world will always be added to the cache if there is not enough room).";
        hardCap = cfg.cfg().getBoolean("hardCap", Configuration.CATEGORY_GENERAL, true, comment);

        cfg.saveAll();
    }

    public static int getMaxItems() {
        return maxItems;
    }

    @Override
    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandEternalItems());
    }

    @Override
    public String getCommitHash() {
        return Lib.Mod.COMMIT_HASH;
    }

    @Override
    public int getBuildType() {
        return Lib.Mod.buildType();
    }

    @Override
    public String getUser() {
        return "AlexIIL";
    }

    @Override
    public String getRepo() {
        return "EternalItems";
    }

    // Mod related events
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void itemExpireEvent(ItemExpireEvent event) {
        if (hardCap && event.entity.worldObj instanceof WorldServer)
            ItemCacheHandler.itemExpired(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void entityAddedEvent(EntityJoinWorldEvent event) {
        if (event.entity instanceof EntityItem && event.world instanceof WorldServer) {
            ItemCacheHandler.itemAdded(event);
        }
    }

    @SubscribeEvent
    public void worldLoadEvent(WorldEvent.Load event) {
        if (event.world instanceof WorldServer)
            ItemCacheHandler.worldLoaded(event);
    }

    @SubscribeEvent
    public void worldTickEvent(WorldTickEvent event) {
        if (event.world instanceof WorldServer)
            ItemCacheHandler.worldTick(event);
    }
}
