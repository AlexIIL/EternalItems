package alexiil.mc.mod.items;

import java.util.*;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

import org.apache.commons.lang3.mutable.MutableLong;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldSavedData;

public class ItemWorldSaveHandler extends WorldSavedData {
    public static final String NAME = "alexiil.mods.items.data";
    private final Map<ChunkCoordIntPair, Deque<EntityItem>> items = Maps.newHashMap();
    private final Map<ChunkCoordIntPair, Integer> chunkStats = Maps.newHashMap();
    private final MutableLong startProfiling = new MutableLong(0);

    public ItemWorldSaveHandler() {
        this(NAME);
    }

    public ItemWorldSaveHandler(String name) {
        super(name);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        startProfiling.setValue(nbt.getLong("startProfiling"));

        NBTTagList list = nbt.getTagList("items", 10);
        items.clear();
        for (int i = 0; i < list.tagCount(); i++) {
            EntityItem entity = new EntityItem(null, 0, 0, 0);
            entity.readFromNBT(list.getCompoundTagAt(i));
            ChunkCoordIntPair ccip = new ChunkCoordIntPair(((int) entity.posX) >> 4, ((int) entity.posY) >> 4);
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
                chunkStats.put(new ChunkCoordIntPair(intArray[0], intArray[1]), intArray[2]);
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
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
        for (Entry<ChunkCoordIntPair, Integer> chunk : chunkStats.entrySet()) {
            ChunkCoordIntPair ccip = chunk.getKey();
            int[] intArray = new int[] { ccip.chunkXPos, ccip.chunkZPos, chunk.getValue() };
            list.appendTag(new NBTTagIntArray(intArray));
        }
        nbt.setTag("chunkStats", list);
    }

    @Override
    public boolean isDirty() {
        // Just assume that the list has been changed at some point during the world changing
        return true;
    }

    public Map<ChunkCoordIntPair, Deque<EntityItem>> getItems() {
        return items;
    }

    public Map<ChunkCoordIntPair, Integer> getChunkStats() {
        return chunkStats;
    }

    public MutableLong getStartProfiling() {
        return startProfiling;
    }
}
