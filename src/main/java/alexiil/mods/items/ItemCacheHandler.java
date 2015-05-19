package alexiil.mods.items;

import java.util.Deque;
import java.util.Map;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;

public class ItemCacheHandler {
    private static final Map<World, Deque<EntityItem>> cachedItems = Maps.newHashMap();

    private static Deque<EntityItem> getCachedItems(World world) {
        if (cachedItems.containsKey(world))
            return cachedItems.get(world);
        Deque<EntityItem> items = Queues.newArrayDeque();
        cachedItems.put(world, items);
        return items;
    }

    private static int getNumberOfItems(World world) {
        return world.getEntities(EntityItem.class, Predicates.alwaysTrue()).size();
    }

    public static void worldLoaded(WorldEvent.Load event) {
        WorldServer world = (WorldServer) event.world;
        ItemWorldSaveHandler items;
        WorldSavedData data = world.getPerWorldStorage().loadData(ItemWorldSaveHandler.class, ItemWorldSaveHandler.NAME);
        if (data == null) {
            items = new ItemWorldSaveHandler(ItemWorldSaveHandler.NAME);
            world.getPerWorldStorage().setData(ItemWorldSaveHandler.NAME, items);
        }
        else {
            items = (ItemWorldSaveHandler) data;
        }

        cachedItems.put(world, items.getItems());
    }

    public static void itemAdded(EntityJoinWorldEvent event) {
        World world = event.world;
        EntityItem item = (EntityItem) event.entity;
        int itemsInWorld = getNumberOfItems(world);
        Deque<EntityItem> items = getCachedItems(world);
        if (itemsInWorld >= EternalItems.getMaxItems()) {
            // Don't add the item to the world, instead add it to the cached queue
            event.setCanceled(true);
            items.add(item);
        }
    }

    public static void itemExpired(ItemExpireEvent event) {
        EntityItem item = event.entityItem;
        World world = item.worldObj;
        int itemsInWorld = getNumberOfItems(world);
        Deque<EntityItem> items = getCachedItems(world);

        if (itemsInWorld < EternalItems.getMaxItems() && items.isEmpty()) {
            event.extraLife = 6000;
            event.setCanceled(true);
        }
        else {
            items.add(item);
        }
    }

    public static void worldTick(WorldTickEvent worldTickEvent) {
        if (!worldTickEvent.phase.equals(Phase.END))
            return;
        World world = worldTickEvent.world;
        tryAddItems(world, getCachedItems(world));
    }

    private static void tryAddItems(World world, Deque<EntityItem> items) {
        while (addItem(world, items));
    }

    /** @return <code>True</code> if an item from the queue was added to the world without going over the item cap */
    private static boolean addItem(World world, Deque<EntityItem> items) {
        if (items.isEmpty())
            return false;

        int itemsInWorld = getNumberOfItems(world);
        if (itemsInWorld >= EternalItems.getMaxItems())
            return false;

        EntityItem entity = items.remove();
        entity.worldObj = world;// If this has been loaded, then it could be null, so just set it anyway
        world.spawnEntityInWorld(entity);
        return true;
    }
}
