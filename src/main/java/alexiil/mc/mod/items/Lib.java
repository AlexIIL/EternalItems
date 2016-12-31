package alexiil.mc.mod.items;

public class Lib {
    public static class Mod {
        public static final String ID = "eternalitems";
        public static final String NAME = "Eternal Items";
        public static final String VERSION = "@VERSION@";
    }

    /** Holds all of the strings sent to players. These are enumerated here rather than placed all throughout the source
     * code as there aren't a lot of them. */
    public static class LocaleStrings {
        public static final String CHAT_FAIL_NULL_WORLD = "eternalitems.chat.fail.nullworld";
        public static final String CHAT_FAIL_NO_ITEMS = "eternalitems.chat.fail.noitems";
        public static final String CHAT_FAIL_INVALID_COMMAND = "eternalitems.chat.fail.invalidcommand";
        public static final String CHAT_STATS_RESET = "eternalitems.chat.stats.reset";
        public static final String CHAT_STATS_ITEM_COUNT = "eternalitems.chat.stats.itemcount";
        public static final String CHAT_STATS_CACHE_SIZE = "eternalitems.chat.stats.cachesize";
        public static final String CHAT_STATS_CHUNK_MOST_ITEMS_HEADER = "eternalitems.chat.stats.mostitems.header";
        public static final String CHAT_STATS_CHUNK_MOST_ITEMS_SINGULAR = "eternalitems.chat.stats.mostitems.singluar";
        public static final String CHAT_STATS_CHUNK_MOST_ITEMS_PLURAL = "eternalitems.chat.stats.mostitems.plural";
        public static final String CHAT_STATS_CHUNK_MOST_ITEMS_NO_MORE = "eternalitems.chat.stats.mostitems.none";
    }
}
