package org.eso.ias.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class CmdReplyThreadFactory implements ThreadFactory  {

    /* The thread group for this factory */
    private final ThreadGroup group = new ThreadGroup("Cmd/Reply thread group");

    /** The logger */
    private static final Logger logger = LoggerFactory.getLogger(CmdReplyThreadFactory.class);

    /** Count the number of created threads */
    private final AtomicInteger threadIdx = new AtomicInteger(0);

    /**
     * @see {@link ThreadFactory#newThread(Runnable)}
     * @param runnable
     * @return
     */
    @Override
    public Thread newThread(Runnable runnable) {
        final int idx = threadIdx.incrementAndGet();
        Thread t = new Thread(group,runnable,"Command/Reply thread "+idx);
        t.setDaemon(true);
        logger.debug("Thread {} created; estimated active threads {}",idx,group.activeCount());
        return t;
    }
}
