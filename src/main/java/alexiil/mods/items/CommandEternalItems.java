package alexiil.mods.items;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

public class CommandEternalItems extends CommandBase {
    @Override
    public String getName() {
        return "eternalitems";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "eternalitems <debug|position|stats|reset>";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (args == null || args.length != 1)
            throw new WrongUsageException(getCommandUsage(sender));

        World world = sender.getEntityWorld();
        if (world == null) {
            sender.addChatMessage(new ChatComponentText("Could not find the world you are in!"));
            return;
        }

        if (args[0].equals("debug")) {
            String[] text = ItemCacheHandler.getDebugLines(world);
            for (String line : text)
                sender.addChatMessage(new ChatComponentText(line));
        }
        else if (args[0].equals("position")) {
            String[] text = ItemCacheHandler.getItemPositionInfo(world);
            for (String line : text)
                sender.addChatMessage(new ChatComponentText(line));
            if (text.length == 0)
                sender.addChatMessage(new ChatComponentText("There are no item entities in this world"));
        }
        else if (args[0].equals("stats")) {
            String[] text = ItemCacheHandler.getChunkStats(world);
            for (String line : text)
                sender.addChatMessage(new ChatComponentText(line));
        }
        else if (args[0].equals("reset")) {
            ItemCacheHandler.resetChunkStats(world);
            sender.addChatMessage(new ChatComponentText("Reset chunk item stats"));
        }
        else {
            sender.addChatMessage(new ChatComponentText(args[0] + " was not a valid argument!"));
        }

    }
}
