package alexiil.mc.mod.items;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;

import org.apache.commons.lang3.mutable.MutableLong;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
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

public class ItemCacheHandler {
    private static final Map<World, Map<ChunkCoordIntPair, Deque<EntityItem>>> cachedItems = new MapMaker().weakKeys().makeMap();
    private static final Map<World, Map<ChunkCoordIntPair, Integer>> chunkStats = new MapMaker().weakKeys().makeMap();
    private static final Map<World, MutableLong> startProfiling = new MapMaker().weakKeys().makeMap();
    private static final Field entityItemAge, entityItemPickupDelay;

    static {
        Class<EntityItem> cls = EntityItem.class;
        Field[] fields = cls.getDeclaredFields();

        entityItemAge = fields[2];
        entityItemAge.setAccessible(true);
        EternalItems.log.info("[set-age] Got field: " + entityItemAge);

        entityItemPickupDelay = fields[3];
        entityItemPickupDelay.setAccessible(true);

        EternalItems.log.info("[get-pickup-delay] Got field: " + entityItemPickupDelay);
    }

    public static void preInit() {}

    private static Map<ChunkCoordIntPair, Deque<EntityItem>> getCachedItems(World world) {
        if (cachedItems.containsKey(world)) return cachedItems.get(world);
        throw new Error("Tried to get a list of cached items before the world was loaded, this will not persist!");
        // Deque<EntityItem> items = Queues.newArrayDeque();
        // cachedItems.put(world, items);
        // return items;
    }

    private static Deque<EntityItem> getCachedItems(World world, ChunkCoordIntPair ccip) {
        Map<ChunkCoordIntPair, Deque<EntityItem>> map = getCachedItems(world);
        if (!map.containsKey(ccip)) {
            map.put(ccip, new ArrayDeque<EntityItem>());
        }
        return map.get(ccip);
    }

    private static Map<ChunkCoordIntPair, Integer> getChunkMap(World world) {
        if (chunkStats.containsKey(world)) return chunkStats.get(world);
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
        } else {
            stats.put(ccip, stats.get(ccip) + 1);
        }
    }

    // FIXME: Cache this value dammit! This probably really expensive and called multiple times per tick when it really
    // doesn't need to be.
    private static int getNumberOfItems(World world) {
        return world.getEntities(EntityItem.class, Predicates.alwaysTrue()).size();
    }

    // FIXME: Cache this value dammit! This probably really expensive and called multiple times per tick when it really
    // doesn't need to be.
    private static int getNumberOfItems(World world, ChunkCoordIntPair ccip) {
        return world.getEntities(EntityItem.class, new ChunkPredicate(ccip)).size();
    }

    public static void worldLoaded(WorldEvent.Load event) {
        WorldServer world = (WorldServer) event.getWorld();
        ItemWorldSaveHandler items;
        WorldSavedData data = world.getPerWorldStorage().loadData(ItemWorldSaveHandler.class, ItemWorldSaveHandler.NAME);
        if (data == null) {
            items = new ItemWorldSaveHandler(ItemWorldSaveHandler.NAME);
            world.getPerWorldStorage().setData(ItemWorldSaveHandler.NAME, items);
        } else {
            items = (ItemWorldSaveHandler) data;
        }

        cachedItems.put(world, items.getItems());
        chunkStats.put(world, items.getChunkStats());
        startProfiling.put(world, items.getStartProfiling());
    }

    public static void itemAdded(EntityJoinWorldEvent event) {
        World world = event.getWorld();
        EntityItem item = (EntityItem) event.getEntity();
        int itemsInWorld = getNumberOfItems(world);
        ChunkCoordIntPair ccip = new ChunkCoordIntPair(((int) item.posX) >> 4, ((int) item.posZ) >> 4);
        Deque<EntityItem> items = getCachedItems(world, ccip);
        if (itemsInWorld >= EternalItems.getMaxItems() || getNumberOfItems(world, ccip) > EternalItems.getMaxItemsChunk()) {
            // Don't add the item to the world, instead add it to the cached queue
            event.setCanceled(true);
            items.add(item);
        }
        Chunk chunk = world.getChunkFromBlockCoords(item.getPosition());
        incrementChunkStat(world, chunk);
    }

    private static boolean setAgeTo0(EntityItem entity) {
        try {
            // TODO: Use an access transformer instead of this slow reflection
            entityItemAge.set(entity, new Integer(0));
            return true;
        } catch (IllegalArgumentException e) {
            EternalItems.log.warn("[set-age] Did you select the wrong field AlexIIL?", e);
        } catch (IllegalAccessException e) {
            EternalItems.log.warn("[set-age] Did you not give yourself access to it properly AlexIIL?", e);
        }
        return false;
    }

    private static int getPickupDelay(EntityItem entity) {
        try {
            // TODO: Use an access transformer instead of this slow reflection
            return entityItemPickupDelay.getInt(entity);
        } catch (IllegalArgumentException e) {
            EternalItems.log.warn("[get-pickup-delay] Did you select the wrong field AlexIIL?", e);
        } catch (IllegalAccessException e) {
            EternalItems.log.warn("[get-pickup-delay] Did you not give yourself access to it properly AlexIIL?", e);
        }
        return 0;
    }

    public static void itemExpired(ItemExpireEvent event) {
        EntityItem item = event.getEntityItem();
        if (item.isDead) return;
        if (getPickupDelay(item) == 32767) return;// Infinite pickup delay
        World world = item.worldObj;
        int itemsInWorld = getNumberOfItems(world);

        ChunkCoordIntPair ccip = new ChunkCoordIntPair(((int) item.posX) >> 4, ((int) item.posZ) >> 4);
        Deque<EntityItem> items = getCachedItems(world, ccip);

        if (setAgeTo0(item)) {
            item.lifespan = 1000;
        } else {
            item.lifespan += 1000;
        }

        boolean spaceInWorld = itemsInWorld < EternalItems.getMaxItems();

        if (!spaceInWorld) {
            items.add(item);
        } else {
            int itemsInChunk = getNumberOfItems(world, ccip);
            boolean spaceInChunk = itemsInChunk < EternalItems.getMaxItemsChunk();
            if (!spaceInChunk) {
                items.add(item);
            } else {
                event.setExtraLife(1000);
                event.setCanceled(true);
            }
        }
    }

    public static void worldTick(WorldTickEvent worldTickEvent) {
        if (!worldTickEvent.phase.equals(Phase.END)) return;
        World world = worldTickEvent.world;
        tryAddItems(world, getCachedItems(world));
    }

    private static void tryAddItems(World world, Map<ChunkCoordIntPair, Deque<EntityItem>> map) {
        while (addItem(world, map));
    }

    /** @return <code>True</code> if an item from the queue was added to the world without going over the item cap */
    private static boolean addItem(World world, Map<ChunkCoordIntPair, Deque<EntityItem>> map) {
        if (map.isEmpty()) return false;

        int itemsInWorld = getNumberOfItems(world);
        if (itemsInWorld >= EternalItems.getMaxItems()) return false;

        boolean added = false;

        for (ChunkCoordIntPair ccip : map.keySet()) {
            Deque<EntityItem> queue = map.get(ccip);
            if (queue.isEmpty()) continue;
            boolean spaceInChunk = getNumberOfItems(world, ccip) < EternalItems.getMaxItemsChunk();
            if (!spaceInChunk) {
                continue;
            }
            EntityItem entity = queue.remove();
            entity.worldObj = world;// If this has been loaded, then it could be null, so just set it anyway
            world.spawnEntityInWorld(entity);
            itemsInWorld++;
            added = true;

            if (itemsInWorld >= EternalItems.getMaxItems()) {
                return true;
            }
        }
        return added;
    }

    public static ITextComponent[] getDebugLines(World world) {
        Map<ChunkCoordIntPair, Deque<EntityItem>> items = getCachedItems(world);
        ITextComponent[] lines = new ITextComponent[2];
        int numInWorld = getNumberOfItems(world);
        int max = EternalItems.getMaxItems();

        lines[0] = new TextComponentTranslation(Lib.LocaleStrings.CHAT_STATS_ITEM_COUNT, numInWorld, max);
        int size = 0;
        int numChunks = 0;
        for (Entry<ChunkCoordIntPair, Deque<EntityItem>> entry : items.entrySet()) {
            int added = entry.getValue().size();
            size += added;
            if (added > 0) {
                numChunks++;
            }
        }
        lines[1] = new TextComponentTranslation(Lib.LocaleStrings.CHAT_STATS_CACHE_SIZE, size, numChunks);
        return lines;
    }

    public static String[] getItemPositionInfo(World world) {
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

    public static ITextComponent[] getChunkStats(World world) {
        ITextComponent[] lines = new ITextComponent[6];
        long length = world.getTotalWorldTime() - startProfiling.get(world).getValue();
        lines[0] = new TextComponentTranslation(Lib.LocaleStrings.CHAT_STATS_CHUNK_MOST_ITEMS_HEADER, length);
        // "Showing the 5 highest item producing chunks since " + length + " ticks ago";

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
                lines[i + 1] = new TextComponentTranslation(Lib.LocaleStrings.CHAT_STATS_CHUNK_MOST_ITEMS_NO_MORE);
                break;
            }
            ChunkCoordIntPair ccip = sortedChunks.get(i);
            int num = chunkMap.get(ccip);

            boolean plural = num != 1;
            String chunkCoords = "[" + ccip.chunkXPos + ", " + ccip.chunkZPos + "]";

            String key = plural ? Lib.LocaleStrings.CHAT_STATS_CHUNK_MOST_ITEMS_PLURAL : Lib.LocaleStrings.CHAT_STATS_CHUNK_MOST_ITEMS_SINGULAR;

            lines[i + 1] = new TextComponentTranslation(key, num, chunkCoords);
        }
        return lines;
    }

    private static class ChunkPredicate implements Predicate<Entity> {
        private final ChunkCoordIntPair ccip;

        private ChunkPredicate(ChunkCoordIntPair ccip) {
            this.ccip = ccip;
        }

        @Override
        public boolean apply(Entity input) {
            return input.posX - ccip.getXStart() <= 16 && input.posZ - ccip.getZStart() <= 16;
        }
    }
}
