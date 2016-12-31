package alexiil.mc.mod.items;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.world.WorldServer;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
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

    private static Property propMaxItems, propMaxItemsPerChunk, propHardCap;

    private static int maxItemsPerWorld, maxItemsPerChunk;
    private static boolean hardCap;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        log = event.getModLog();
        cfg = new Configuration(event.getSuggestedConfigurationFile());

        MinecraftForge.EVENT_BUS.register(this);
        ItemCacheHandler.preInit();

        cfg.load();

        propMaxItems = cfg.get(Configuration.CATEGORY_GENERAL, "maxItems", 10_000).setMinValue(20).setMaxValue(1_000_000);
        propMaxItems.setComment("The maximum number of items allowed per world before new ones start to be cached.");

        propMaxItemsPerChunk = cfg.get(Configuration.CATEGORY_GENERAL, "maxItemsPerChunk", 100)
                                  .setMinValue(5)
                                  .setMaxValue(1_000);
        propMaxItemsPerChunk.setComment("The maximum number of items allowed per chunk before new ones are added to the queue of that chunk");

        propHardCap = cfg.get(Configuration.CATEGORY_GENERAL, "hardCap", true);
        propHardCap.setComment("True if you want the items added to the world to be "
            + "hard capped to the number (but the items that expire in the world will "
            + "always be added to the cache if there is not enough room).");

        loadFromConfig();

        cfg.save();
    }

    private static void loadFromConfig() {
        maxItemsPerWorld = propMaxItems.getInt();
        maxItemsPerChunk = propMaxItemsPerChunk.getInt();
        hardCap = propHardCap.getBoolean();
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

    @SubscribeEvent
    public void onConfigChange(ConfigChangedEvent cce) {
        if (Lib.Mod.ID.equals(cce.getModID())) {
            loadFromConfig();
        }
    }

    public static List<IConfigElement> getConfigElements() {
        List<IConfigElement> elements = new ArrayList<>();
        elements.add(new ConfigElement(propMaxItems));
        elements.add(new ConfigElement(propMaxItemsPerChunk));
        elements.add(new ConfigElement(propHardCap));
        return elements;
    }
}
