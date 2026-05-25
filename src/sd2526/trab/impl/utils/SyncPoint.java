package sd2526.trab.impl.utils;

import java.util.concurrent.ConcurrentHashMap;

public class SyncPoint {

    private final ConcurrentHashMap<Long, Object> result = new ConcurrentHashMap<>();
    private static final Object NULL_RESULT = new Object();
    private long version = 0;  // inicia em 0, como no lab
    private static SyncPoint instance;

    private SyncPoint() {}

    public static synchronized SyncPoint getSyncPoint() {
        if (instance == null) instance = new SyncPoint();
        return instance;
    }

    // Bloqueia até o offset n ter sido processado E ter resultado disponível
    public synchronized String waitForResult(long n) {
        while (!result.containsKey(n))
            try { wait(); } catch (InterruptedException e) {}
        Object res = result.remove(n);
        return res == NULL_RESULT ? null : (String) res;
    }

    // Bloqueia até version >= n (garante causalidade para leituras)
    public synchronized void waitForVersion(long n) {
        while (version < n)
            try { wait(); } catch (InterruptedException e) {}
    }

    // Chamado pelo Kafka consumer — regista o resultado da operação com offset n
    public synchronized void setResult(long n, String res) {
        /*if (n <= version && version != 0)
            throw new RuntimeException("Version " + n + " is already set");*/
        result.putIfAbsent(n, res != null ? res : NULL_RESULT);
        if (n > version) version = n;
        notifyAll();
    }

    public synchronized long getVersion() {
        return version;
    }
    
}