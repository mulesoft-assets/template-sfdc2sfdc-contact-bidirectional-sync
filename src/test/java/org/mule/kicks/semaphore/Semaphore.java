package org.mule.kicks.semaphore;

import java.io.Serializable;

import org.apache.log4j.Logger;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;


public class Semaphore {
	
	private static final Logger log = Logger.getLogger(Semaphore.class.getName());
	private static final String SEMAPHORE_KEY = "status";
	private static final String AVAILABLE_STATUS = "IDLE";
	private static final String BUSY_STATUS = "BUSY";
	private ObjectStore<Serializable> os;

	public synchronized boolean red() {
		String status;
		
		try {
			status = (String) os.retrieve(SEMAPHORE_KEY);
			if (status.equals(AVAILABLE_STATUS)) {
				os.store(SEMAPHORE_KEY, BUSY_STATUS);
				return true;
			}
		} catch (ObjectStoreException e) {
			log.error("The ObjectStore is not available");
		}
		
		return false;
	}

	public synchronized boolean green() {
		
		try {
			os.store(SEMAPHORE_KEY, AVAILABLE_STATUS);
			return true;
		} catch (ObjectStoreException e) {
			log.error("The ObjectStore is not available");
		}
		
		return false;
	}

	public ObjectStore<Serializable> getOs() {
		return os;
	}

	public void setOs(ObjectStore<Serializable> os) {
		this.os = os;
	}

}
