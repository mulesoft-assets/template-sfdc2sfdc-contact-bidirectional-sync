package org.mule.templates.util;

import java.util.concurrent.locks.Lock;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;

/**
 * This class objective is to create a critical area around the application in order to stop that two what ever threads access the area if other is in it.
 * 
 * @author javiercasal
 */
public class PollSynchronizer implements MuleContextAware {

	private static final String LOCK_ID = "poll_semaphore_lock";
	private MuleContext muleContext;

	public void acquireLock() throws InterruptedException {
		getLock().lock();
		System.err.println("Lock acquired by " + Thread.currentThread().getName());
	}

	public void releaseLock() {
		getLock().unlock();
		System.err.println("Lock released by " + Thread.currentThread().getName());
	}

	@Override
	public void setMuleContext(MuleContext context) {
		this.muleContext = context;
	}

	private Lock getLock() {
		return muleContext.getLockFactory().createLock(LOCK_ID);
	}

}