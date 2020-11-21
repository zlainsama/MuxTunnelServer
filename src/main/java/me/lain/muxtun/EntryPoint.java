package me.lain.muxtun;

public class EntryPoint {

    public static void main(String[] args) throws Exception {
        try {
            setupDefaults();
        } catch (SecurityException ignored) {
        }
        App.main(args);
    }

    private static void setupDefaults() {
        setPropertyIfNull("io.netty.maxDirectMemory", "0");
        setPropertyIfNull("io.netty.allocator.cacheTrimIntervalMillis", "30000");
    }

    private static void setPropertyIfNull(String key, String value) {
        if (System.getProperty(key) == null)
            System.setProperty(key, value);
    }

}
