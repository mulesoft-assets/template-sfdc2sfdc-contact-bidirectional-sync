package org.mule.kicks.builders;

import java.util.HashMap;
import java.util.Map;

public class ContactBuilder {

	private Map<String, String> fields = new HashMap<String, String>();

	public static ContactBuilder aContact() {
		return new ContactBuilder();
	}
	
	public ContactBuilder with(String field, String value) {
		this.fields.put(field, value);
		return this;
	}
	
	public Map<String, String> build() {
		return fields;
	}
}
