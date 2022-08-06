package utils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadUtils {
	public static ThreadPoolExecutor ioThreadPool = new ThreadPoolExecutor(4, 200, 10L,TimeUnit.MINUTES
			, new LinkedBlockingQueue(), new NameableThreadFactory("io-stock", false));
}
