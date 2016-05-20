package alexiil.mc.mod.items;

import java.util.*;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

import org.apache.commons.lang3.mutable.MutableLong;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

public class ItemWorldSaveHandler extends WorldSavedData {
    public static final String NAME = "alexiil.mods.items.data";
    public static final ThreadLocal<World> WORLD_HOLDER = new ThreadLocal<World>();
    private final Map<ChunkPos, Deque<EntityItem>> items = Maps.newHashMap();
    private final Map<ChunkPos, Integer> chunkStats = Maps.newHashMap();
    private final MutableLong startProfiling = new MutableLong(0);
    private World world;

    public ItemWorldSaveHandler() {
        this(NAME);
    }

    public ItemWorldSaveHandler(String name) {
        super(name);
        setWorld(WORLD_HOLDER.get());
    }

    public void setWorld(World world) {
        this.world = world;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        startProfiling.setValue(nbt.getLong("startProfiling"));

        NBTTagList list = nbt.getTagList("items", 10);
        items.clear();
        for (int i = 0; i < list.tagCount(); i++) {
            EntityItem entity = new EntityItem(getWorld(), 0, 0, 0);
            entity.readFromNBT(list.getCompoundTagAt(i));
            ChunkPos ccip = new ChunkPos(((int) entity.posX) >> 4, ((int) entity.posY) >> 4);
            if (!items.containsKey(ccip)) {
                items.put(ccip, new ArrayDeque<EntityItem>());
            }
            items.get(ccip).add(entity);
        }

        list = nbt.getTagList("chunkStats", 11);
        chunkStats.clear();
        for (int i = 0; i < list.tagCount(); i++) {
            int[] intArray = list.getIntArrayAt(i);
            if (intArray.length != 3) {
                EternalItems.log.warn("[item-saving] Found an integer array that was not 3 long!! " + "(array = " + Arrays.toString(intArray) + ", index = " + i + ")");
            } else {
                chunkStats.put(new ChunkPos(intArray[0], intArray[1]), intArray[2]);
            }
        }
    }

    private World getWorld() {
        if (world == null) throw new NullPointerException();
        return world;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setLong("startProfiling", startProfiling.longValue());

        NBTTagList list = new NBTTagList();
        for (Collection<EntityItem> items : this.items.values()) {
            for (EntityItem item : items) {
                NBTTagCompound itemNBT = new NBTTagCompound();
                item.writeToNBT(itemNBT);
                list.appendTag(itemNBT);
            }
        }
        nbt.setTag("items", list);

        list = new NBTTagList();
        for (Entry<ChunkPos, Integer> chunk : chunkStats.entrySet()) {
            ChunkPos ccip = chunk.getKey();
            int[] intArray = new int[] { ccip.chunkXPos, ccip.chunkZPos, chunk.getValue() };
            list.appendTag(new NBTTagIntArray(intArray));
        }
        nbt.setTag("chunkStats", list);
        return nbt;
    }

    @Override
    public boolean isDirty() {
        // Just assume that the list has been changed at some point during the world changing
        return true;
    }

    public Map<ChunkPos, Deque<EntityItem>> getItems() {
        return items;
    }

    public Map<ChunkPos, Integer> getChunkStats() {
        return chunkStats;
    }

    public MutableLong getStartProfiling() {
        return startProfiling;
    }
}
