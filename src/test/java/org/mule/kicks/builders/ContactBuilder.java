package org.mule.kicks.builders;

import java.util.HashMap;
import java.util.Map;

public class ContactBuilder {

	private Map<String, String> fields;

	public ContactBuilder() {
		this.fields = new HashMap<String, String>();
	}
	
	public ContactBuilder(ContactBuilder original) {
		this.fields = original.fields;
	}

	public static ContactBuilder aContact() {
		return new ContactBuilder();
	}
	
	public ContactBuilder with(String field, String value) {
		ContactBuilder contactCopy = new ContactBuilder(this);
		contactCopy.fields.put(field, value);
		return contactCopy;
	}
	
	public Map<String, String> build() {
		return fields;
	}
}
