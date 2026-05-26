package sd2526.trab.impl.utils;

import java.util.concurrent.ConcurrentHashMap;

public class SyncPoint {

    private final ConcurrentHashMap<Long, Object> result = new ConcurrentHashMap<>();
    private static final Object NULL_RESULT = new Object();
    private long version = 0;
    private static SyncPoint instance;

    private SyncPoint() {}

    public static synchronized SyncPoint getSyncPoint() {
        if (instance == null) instance = new SyncPoint();
        return instance;
    }

    public synchronized String waitForResult(long n) {
        while (!result.containsKey(n))
            try { wait(); } catch (InterruptedException e) {}
        Object res = result.remove(n);
        return res == NULL_RESULT ? null : (String) res;
    }

    public synchronized void waitForVersion(long n) {
        while (version < n)
            try { wait(); } catch (InterruptedException e) {}
    }

    public synchronized void setResult(long n, String res) {
        result.putIfAbsent(n, res != null ? res : NULL_RESULT);
        if (n > version) version = n;
        notifyAll();
    }

    public synchronized long getVersion() {
        return version;
    }
    
}