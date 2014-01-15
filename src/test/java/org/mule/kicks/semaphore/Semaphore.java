package org.mule.kicks.semaphore;

import java.io.Serializable;

import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;


public class Semaphore {
	private static final String SEMAPHORE_KEY = "";
	private ObjectStore<Serializable> os;

	public synchronized boolean red() throws ObjectStoreException {
		String status = (String) os.retrieve(SEMAPHORE_KEY);
		if (status.equals("idle")) {
			os.store(SEMAPHORE_KEY, "bussy");
			return true;
		}
		return false;
	}

	public synchronized void green() {
	}

	public ObjectStore<Serializable> getOs() {
		return os;
	}

	public void setOs(ObjectStore<Serializable> os) {
		this.os = os;
	}

}
