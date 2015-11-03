package alexiil.mods.items;

public class Lib {
    public static class Mod {
        public static final String ID = "eternalitems";
        public static final String NAME = "Eternal Items";
        public static final String VERSION = "@VERSION@";
        public static final String COMMIT_HASH = "@COMMIT_HASH@";

        private static final String ALEXIIL_LIB_MIN = "@ALEXIIL_LIB_MIN@";
        private static final String ALEXIIL_LIB_MAX = "@ALEXIIL_LIB_MAX@";

        public static final String DEPS = "required-after:alexiillib[" + ALEXIIL_LIB_MIN + "," + ALEXIIL_LIB_MAX + ")";

        public static int buildType() {
            if (COMMIT_HASH.startsWith("@"))
                return 0;
            if (COMMIT_HASH.startsWith("manual "))
                return 1;
            return 2;
        }
    }
}
