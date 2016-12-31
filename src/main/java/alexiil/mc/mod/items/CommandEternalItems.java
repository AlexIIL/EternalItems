package alexiil.mc.mod.items;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class CommandEternalItems extends CommandBase {
    private static final String[] SUB_COMMANDS = { "debug", "position", "stats", "reset" };

    @Override
    public String getCommandName() {
        return "eternalitems";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public List<String> getTabCompletionOptions(MinecraftServer server,
                                          ICommandSender sender,
                                          String[] args,
                                          BlockPos targetPos) {
        return getListOfStringsMatchingLastWord(args, SUB_COMMANDS);
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "eternalitems <debug|position|stats|reset>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args == null || args.length != 1) throw new WrongUsageException(getCommandUsage(sender));

        World world = sender.getEntityWorld();
        if (world == null) {
            sender.addChatMessage(new TextComponentTranslation(Lib.LocaleStrings.CHAT_FAIL_NULL_WORLD));
            return;
        }

        if (args[0].equals("debug")) {
            ITextComponent[] text = ItemCacheHandler.getDebugLines(world);
            for (ITextComponent line : text) {
                sender.addChatMessage(line);
            }
        } else if (args[0].equals("position")) {
            String[] text = ItemCacheHandler.getItemPositionInfo(world);
            for (String line : text) {
                sender.addChatMessage(new TextComponentString(line));
            }
            if (text.length == 0) {
                sender.addChatMessage(new TextComponentTranslation(Lib.LocaleStrings.CHAT_FAIL_NO_ITEMS));
            }
        } else if (args[0].equals("stats")) {
            ITextComponent[] text = ItemCacheHandler.getChunkStats(world);
            for (ITextComponent line : text) {
                if (line == null) break;
                sender.addChatMessage(line);
            }
        } else if (args[0].equals("reset")) {
            ItemCacheHandler.resetChunkStats(world);
            sender.addChatMessage(new TextComponentTranslation(Lib.LocaleStrings.CHAT_STATS_RESET));
        } else {
            sender.addChatMessage(new TextComponentTranslation(Lib.LocaleStrings.CHAT_FAIL_INVALID_COMMAND, args[0]));
        }

    }
}
