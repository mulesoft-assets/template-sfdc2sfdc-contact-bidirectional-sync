package org.mule.kicks.checks;

import static org.mule.kicks.builders.ContactBuilder.aContact;

import java.util.Map;
import static org.mule.kicks.checks.SalesforceContactIdentity.areEqual;

import org.junit.Assert;
import org.junit.Test;

public class SalesforceContactIdentityTest {

	@Test
	public void theContactIdentityConsiderEqualAContactWithItself() {
		Map<String, String> johnDoe = aContact() //
				.with("FirstName", "John") //
				.with("LastName", "Doe") //
				.build();

		Assert.assertTrue("Something is not ok: a contact is not equal to himself", areEqual(johnDoe, johnDoe));
	}

	@Test
	public void theContactIdentityConsiderDifferentTwoDifferentContacts() {
		Map<String, String> johnDoe = aContact() //
				.with("FirstName", "John") //
				.with("LastName", "Doe") //
				.build();

		Map<String, String> jeanDeo = aContact() //
				.with("FirstName", "Jean") //
				.with("LastName", "Deo") //
				.build();

		Assert.assertTrue("We have a problem: the contact identity considers that John Doe is equal to Jean Deo!", !areEqual(johnDoe, jeanDeo));
	}

}
