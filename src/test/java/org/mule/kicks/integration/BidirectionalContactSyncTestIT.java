package org.mule.kicks.integration;

import static org.mule.kicks.builders.ContactBuilder.aContact;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.construct.Flow;
import org.mule.kicks.builders.ContactBuilder;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the flows
 * for this Mule Kick that make calls to external systems.
 * 
 */
@SuppressWarnings("unchecked")
public class BidirectionalContactSyncTestIT extends AbstractKickTestCase {

	@Before
	public void setUp() throws InitialisationException {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void whenUpdatingAContactInSourceSystemTheBelongingContactgetsUpdatedInTargetSystem() throws Exception {

		/*
		 * Preparation
		 */

		// Build test contacts
		ContactBuilder johnDoe = aContact() //
				.with("FirstName", "John") //
				.with("LastName", "Doe") //
				.with("Email", "john.doe@mail.com");

		Map<String, String> johnDoeWithBasicDescription = johnDoe //
				.with("Description", "Please enter your description here") //
				.build();
		Map<String, String> johnDoeWithUpdatedDescription = johnDoe //
				.with("Description", "John Doe is the man!") //
				.build();

		// TODO: make it pritty :)
		// Create test contacts in Salesforce sandboxes
		List<Map<String, String>> salesforceAContacts = new ArrayList<Map<String, String>>();
		salesforceAContacts.add(johnDoeWithBasicDescription);

		List<Map<String, String>> salesforceBContacts = new ArrayList<Map<String, String>>();
		salesforceBContacts.add(johnDoeWithUpdatedDescription);

		SubflowInterceptingChainLifecycleWrapper createContactFlowInAFlow = getSubFlow("createContactFlowInAFlow");
		createContactFlowInAFlow.initialise();
		final List<SaveResult> payloadAfterExecutionInA = (List<SaveResult>) createContactFlowInAFlow.process(getTestEvent(salesforceAContacts, MessageExchangePattern.REQUEST_RESPONSE))
				.getMessage().getPayload();

		SubflowInterceptingChainLifecycleWrapper createContactFlowInBFlow = getSubFlow("createContactFlowInBFlow");
		createContactFlowInBFlow.initialise();
		final List<SaveResult> payloadAfterExecutionInB = (List<SaveResult>) createContactFlowInBFlow.process(getTestEvent(salesforceBContacts, MessageExchangePattern.REQUEST_RESPONSE))
				.getMessage().getPayload();

		// Get Id from created contacts
		String createdContactInAId = payloadAfterExecutionInA.get(0).getId();
		String createdContactInBId = payloadAfterExecutionInB.get(0).getId();

		/*
		 * Execution
		 */

		Flow mainFlow = getFlow("mainFlow");
		mainFlow.process(getTestEvent("", MessageExchangePattern.REQUEST_RESPONSE));

		/*
		 * Assertions
		 */
		// TODO - Cast properly (using String type)
		Map<?, ?> retrievedContactFromA = retrieveContact("retrieveContactFromAFlow", johnDoe.build());
		Map<?, ?> retrievedContactFromB =  retrieveContact("retrieveContactFromBFlow", johnDoe.build());

		final MapDifference<Object, Object> mapsDifference = Maps.difference(retrievedContactFromA, retrievedContactFromB);
		Assert.assertTrue("Some contacts are not synchronized between systems. " + mapsDifference.toString(), mapsDifference.areEqual());

		/*
		 * Tear down
		 */
		// TODO: make it pritty :) Refactor the test contacts using them as instance variables
		// Delete contacts created in Salesforce sandboxes
		SubflowInterceptingChainLifecycleWrapper deleteContactFromAFlow = getSubFlow("deleteContactFromAFlow");
		deleteContactFromAFlow.initialise();
		deleteContactFromAFlow.process(getTestEvent(createdContactInAId, MessageExchangePattern.REQUEST_RESPONSE));

		SubflowInterceptingChainLifecycleWrapper deleteContactFromBFlow = getSubFlow("deleteContactFromBFlow");
		deleteContactFromBFlow.initialise();
		deleteContactFromBFlow.process(getTestEvent(createdContactInBId, MessageExchangePattern.REQUEST_RESPONSE));
	}

	private Map<String, String> retrieveContact(String flowName, Map<String,String > contact) throws MuleException, Exception {
		SubflowInterceptingChainLifecycleWrapper retrieveContactFromAFlow = getSubFlow(flowName);
		retrieveContactFromAFlow.initialise();
		return  (Map<String, String>) retrieveContactFromAFlow.process(getTestEvent(contact, MessageExchangePattern.REQUEST_RESPONSE)).getMessage().getPayload();
	}

}
