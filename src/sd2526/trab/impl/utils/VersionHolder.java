package sd2526.trab.impl.utils;

public class VersionHolder {
    private static final ThreadLocal<Long> version = new ThreadLocal<>();

    public static void set(long v) { version.set(v); }
    public static Long get() {
        Long v = version.get(); 
        version.remove(); 
        return v != null ? v : -1L;
    }
}