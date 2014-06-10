package org.mule.templates.util;

import java.util.concurrent.Semaphore;

/**
 * This class objective is to create a critical area around the application in
 * order to stop that two what ever threads access the area if other is in it.
 * 
 * A semaphore initialized to one, and which is used such that it only has at
 * most one permit available, can serve as a mutual exclusion lock. This is more
 * commonly known as a binary semaphore, because it only has two states: one
 * permit available, or zero permits available. When used in this way, the
 * binary semaphore has the property (unlike many Lock implementations), that
 * the "lock" can be released by a thread other than the owner (as semaphores
 * have no notion of ownership).
 * 
 * @author javiercasal
 */
public class PollSynchronizer {

	private Semaphore semaphore;
	private static final int MAX_PERMITS_NUMBER = 1;

	public Semaphore getLock() {
		if (semaphore == null) {
			this.semaphore = new Semaphore(MAX_PERMITS_NUMBER);
		}
		
		return this.semaphore;
	}

}