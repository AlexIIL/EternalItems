package alexiil.mods.items;

import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;

import org.apache.commons.lang3.mutable.MutableLong;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;

public class ItemCacheHandler {
    private static final Map<World, Deque<EntityItem>> cachedItems = new MapMaker().weakKeys().makeMap();
    private static final Map<World, Map<ChunkCoordIntPair, Integer>> chunkStats = new MapMaker().weakKeys().makeMap();
    private static final Map<World, MutableLong> startProfiling = new MapMaker().weakKeys().makeMap();

    private static Deque<EntityItem> getCachedItems(World world) {
        if (cachedItems.containsKey(world))
            return cachedItems.get(world);
        throw new Error("Tried to get a list of cached items before the world was loaded, this will not persist!");
        // Deque<EntityItem> items = Queues.newArrayDeque();
        // cachedItems.put(world, items);
        // return items;
    }

    private static Map<ChunkCoordIntPair, Integer> getChunkMap(World world) {
        if (chunkStats.containsKey(world))
            return chunkStats.get(world);
        throw new Error("Tried to get a list of chunks before the world was loaded, this will not persist!");
        // Map<Chunk, Integer> items = new MapMaker().weakKeys().makeMap();
        // chunkStats.put(world, items);
        // return items;
    }

    private static void incrementChunkStat(World world, Chunk chunk) {
        Map<ChunkCoordIntPair, Integer> stats = getChunkMap(world);
        ChunkCoordIntPair ccip = chunk.getChunkCoordIntPair();
        if (!stats.containsKey(ccip)) {
            stats.put(ccip, 1);
        }
        else {
            stats.put(ccip, stats.get(ccip) + 1);
        }
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
        chunkStats.put(world, items.getChunkStats());
        startProfiling.put(world, items.getStartProfiling());
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
        Chunk chunk = world.getChunkFromBlockCoords(item.getPosition());
        incrementChunkStat(world, chunk);
    }

    public static void itemExpired(ItemExpireEvent event) {
        EntityItem item = event.entityItem;
        World world = item.worldObj;
        int itemsInWorld = getNumberOfItems(world);
        Deque<EntityItem> items = getCachedItems(world);

        if (itemsInWorld < EternalItems.getMaxItems() && items.isEmpty()) {
            event.extraLife = 1000;
            event.setCanceled(true);
        }
        else {
            items.add(item);
            item.lifespan += 1000;
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

    public static String[] getDebugLines(World world) {
        Deque<EntityItem> items = getCachedItems(world);
        String[] lines = new String[2];
        lines[0] = "Number of items in world: " + getNumberOfItems(world);
        lines[1] = "Current cache queue size: " + items.size();
        return lines;
    }

    public static String[] getItemPositionInfo(World world) {
        @SuppressWarnings("unchecked")
        List<EntityItem> items = world.getEntities(EntityItem.class, Predicates.alwaysTrue());

        String[] lines = new String[items.size()];
        for (int i = 0; i < items.size(); i++) {
            EntityItem item = items.get(i);
            lines[i] = item.toString();
        }
        return lines;
    }

    public static void resetChunkStats(World world) {
        getChunkMap(world).clear();
        MutableLong mutLong = startProfiling.get(world);
        mutLong.setValue(world.getTotalWorldTime());
    }

    public static String[] getChunkStats(World world) {
        String[] lines = new String[6];
        long length = world.getTotalWorldTime() - startProfiling.get(world).getValue();
        lines[0] = "Showing the 5 highest item producing chunks since " + length + " ticks ago";

        final Map<ChunkCoordIntPair, Integer> chunkMap = getChunkMap(world);
        Set<ChunkCoordIntPair> chunks = chunkMap.keySet();
        List<ChunkCoordIntPair> sortedChunks = Lists.newArrayList();
        sortedChunks.addAll(chunks);

        Collections.sort(sortedChunks, new Comparator<ChunkCoordIntPair>() {
            @Override
            public int compare(ChunkCoordIntPair o1, ChunkCoordIntPair o2) {
                int one = chunkMap.get(o1);
                int two = chunkMap.get(o2);
                return two - one;
            }
        });

        for (int i = 0; i < 5; i++) {
            if (i >= sortedChunks.size()) {
                lines[i + 1] = "No data (All other chunks produced 0 items)";
                continue;
            }
            ChunkCoordIntPair ccip = sortedChunks.get(i);
            int num = chunkMap.get(ccip);
            String pre = num + " item" + (num == 1 ? "" : "s") + " at chunk co-ords ";
            String coords = ccip.toString() + " center block co-ords [" + ccip.getCenterXPos() + "," + ccip.getCenterZPosition() + "]";
            lines[i + 1] = pre + coords;
        }
        return lines;
    }
}
