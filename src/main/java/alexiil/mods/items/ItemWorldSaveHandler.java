package alexiil.mods.items;

import java.util.Deque;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldSavedData;

import com.google.common.collect.Queues;

public class ItemWorldSaveHandler extends WorldSavedData {
    public static final String NAME = "alexiil.mods.items.data";
    private Deque<EntityItem> items = Queues.newArrayDeque();

    public ItemWorldSaveHandler() {
        this(NAME);
    }

    public ItemWorldSaveHandler(String name) {
        super(name);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        NBTTagList list = nbt.getTagList("items", 10);
        items.clear();
        for (int i = 0; i < list.tagCount(); i++) {
            EntityItem entity = new EntityItem(null, 0, 0, 0);
            entity.readFromNBT(list.getCompoundTagAt(i));
            items.add(entity);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (EntityItem item : items) {
            NBTTagCompound itemNBT = new NBTTagCompound();
            item.writeToNBT(itemNBT);
            list.appendTag(itemNBT);
        }
        nbt.setTag("items", list);
    }

    @Override
    public boolean isDirty() {
        // Just assume that the list has been changed at some point during the world changing
        return true;
    }

    public Deque<EntityItem> getItems() {
        return items;
    }

    public void setItems(Deque<EntityItem> items) {
        this.items = items;
    }
}
