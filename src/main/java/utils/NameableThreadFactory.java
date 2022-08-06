package utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NameableThreadFactory implements ThreadFactory {
    private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private final boolean daemonizeThreads;

    public NameableThreadFactory(String _name, boolean _daemonizeThreads) {
        SecurityManager s = System.getSecurityManager();
        this.group = s != null ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.namePrefix = _name == null ? "UnnamedThreadPool-" + POOL_NUMBER.getAndIncrement() + "-thread-" : _name;
        this.daemonizeThreads = _daemonizeThreads;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(this.group, r, this.namePrefix + this.threadNumber.getAndIncrement(), 0L);
        t.setDaemon(this.daemonizeThreads);
        if (t.getPriority() != 5) {
            t.setPriority(5);
        }

        return t;
    }
}
