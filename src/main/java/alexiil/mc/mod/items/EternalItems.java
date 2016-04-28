package alexiil.mc.mod.items;

import org.apache.logging.log4j.Logger;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.world.WorldServer;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;

@Mod(modid = Lib.Mod.ID, guiFactory = "alexiil.mc.mod.items.ConfigGuiFactory", acceptableRemoteVersions = "*")
public class EternalItems {
    @Instance(Lib.Mod.ID)
    public static EternalItems INSTANCE;
    public static Logger log;
    public static Configuration cfg;
    private static int maxItemsPerWorld, maxItemsPerChunk;
    private static boolean hardCap;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        log = event.getModLog();
        cfg = new Configuration(event.getSuggestedConfigurationFile());

        MinecraftForge.EVENT_BUS.register(this);
        ItemCacheHandler.preInit();

        cfg.load();

        String comment = "The maximum number of items allowed per world before new ones start to be cached.";
        maxItemsPerWorld = cfg.getInt("maxItems", Configuration.CATEGORY_GENERAL, 600, 20, 100000, comment);

        comment = "The maximum number of items allowed per chunk before new ones are added to the queue of that chunk";
        maxItemsPerChunk = cfg.getInt("maxItemsPerChunk", Configuration.CATEGORY_GENERAL, 30, 5, 1000, comment);

        comment = "True if you want the items added to the world to be hard capped to the number ";
        comment += "(but the items that expire in the world will always be added to the cache if there is not enough room).";
        hardCap = cfg.getBoolean("hardCap", Configuration.CATEGORY_GENERAL, true, comment);

        cfg.save();
    }

    public static int getMaxItems() {
        return maxItemsPerWorld;
    }

    public static int getMaxItemsChunk() {
        return maxItemsPerChunk;
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandEternalItems());
    }

    // Mod related events
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void itemExpireEvent(ItemExpireEvent event) {
        if (hardCap && event.getEntity().worldObj instanceof WorldServer) ItemCacheHandler.itemExpired(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void entityAddedEvent(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityItem && event.getWorld() instanceof WorldServer) {
            ItemCacheHandler.itemAdded(event);
        }
    }

    @SubscribeEvent
    public void worldLoadEvent(WorldEvent.Load event) {
        if (event.getWorld() instanceof WorldServer) ItemCacheHandler.worldLoaded(event);
    }

    @SubscribeEvent
    public void worldTickEvent(WorldTickEvent event) {
        if (event.world instanceof WorldServer) ItemCacheHandler.worldTick(event);
    }
}
