package org.mule.kicks.semaphore;

import java.io.Serializable;

import org.apache.log4j.Logger;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;

/**
 * This class objective is to create a critical area around the application in
 * order to stop that two what ever threads access the area if other is in it.
 * 
 * @author javiercasal
 */
public class PollSemaphore {

	private static final Logger log = Logger.getLogger(PollSemaphore.class);

	private static final String SEMAPHORE_KEY = "status";
	private static final String BUSY_STATUS = "BUSY";
	private static final String AVAILABLE_STATUS = "IDLE";

	private ObjectStore<Serializable> objectStore;

	public synchronized boolean red() {
		String status;

		try {
			status = (String) objectStore.retrieve(SEMAPHORE_KEY);
			if (status.equals(AVAILABLE_STATUS)) {
				objectStore.store(SEMAPHORE_KEY, BUSY_STATUS);
				return true;
			}
		} catch (ObjectStoreException e) {
			log.error("The ObjectStore is not available");
		}

		return false;
	}

	public synchronized boolean green() {
		try {
			objectStore.store(SEMAPHORE_KEY, AVAILABLE_STATUS);
			return true;
		} catch (ObjectStoreException e) {
			log.error("The ObjectStore is not available");
		}

		return false;
	}

	public ObjectStore<Serializable> getObjectStore() {
		return objectStore;
	}

	public void setObjectStore(ObjectStore<Serializable> objectStore) {
		this.objectStore = objectStore;
	}

}
