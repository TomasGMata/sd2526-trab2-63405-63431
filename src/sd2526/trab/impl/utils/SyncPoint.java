package sd2526.trab.impl.utils;

import java.util.concurrent.ConcurrentHashMap;

public class SyncPoint {

    private final ConcurrentHashMap<Long, Object> result = new ConcurrentHashMap<>();
    private static final Object NULL_RESULT = new Object();
    private long version = -1;  // começa em -1, não 0
    private static SyncPoint instance;

    private SyncPoint() {}

    public static synchronized SyncPoint getSyncPoint() {
        if (instance == null) instance = new SyncPoint();
        return instance;
    }

    // Chamado pela REST thread — bloqueia até o Kafka consumer processar o offset n
    public synchronized String waitForResult(long n) {
        while (!result.containsKey(n))  // espera até o resultado estar disponível
            try { wait(); } catch (InterruptedException e) {}
        Object res = result.remove(n);
        return res == NULL_RESULT ? null : (String) res;
    }

    // Chamado pela Kafka consumer thread após executar a operação
    public synchronized void setResult(long n, String res) {
        result.put(n, res != null ? res : NULL_RESULT);
        version = Math.max(version, n);  // nunca recua
        notifyAll();
    }
}