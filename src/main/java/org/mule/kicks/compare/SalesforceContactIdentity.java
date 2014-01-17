package org.mule.kicks.compare;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SalesforceContactIdentity {
	
	private final static List<String> contactIdentifierFields = Arrays.asList("FirstName", "LastName", "Email");

	
	public static boolean areEqual(Map<String, String> contact1, Map<String, String> contact2) {
		return hashCode(contact1) == hashCode(contact2);
	}
	
	public static int hashCode(Map<String, String> contact) {
		int result = 17;
		for (String contactIdentifierField : contactIdentifierFields) {
			if (contact.containsKey(contactIdentifierField)) {
				final String fieldValue = contact.get(contactIdentifierField);
				result = 37 * result + fieldValue.hashCode();
			}
		}
		return result;
	}
}
