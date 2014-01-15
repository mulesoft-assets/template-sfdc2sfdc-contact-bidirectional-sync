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
import org.mule.api.lifecycle.InitialisationException;
import org.mule.construct.Flow;
import org.mule.kicks.builders.ContactBuilder;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

/**
 * The objective of this class is to validate the correct behavior of the flows
 * for this Mule Kick that make calls to external systems.
 * 
 */
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
		ContactBuilder johnDoe = aContact().with("FirstName", "John").with("LastName", "Doe").with("Email", "john.doe@mail.com");

		Map<String, String> johnDoeWithBasicDescription = johnDoe.with("Description", "Please enter your description here").build();
		Map<String, String> johnDoeWithUpdatedDescription = johnDoe.with("Description", "John Doe is the man!").build();

		// Create test contacts in Salesforce sandboxes
		List<Map<String, String>> systemAContacts = new ArrayList<Map<String, String>>();
		systemAContacts.add(johnDoeWithBasicDescription);

		List<Map<String, String>> systemBContacts = new ArrayList<Map<String, String>>();
		systemBContacts.add(johnDoeWithUpdatedDescription);

		SubflowInterceptingChainLifecycleWrapper createContactFlowInAFlow = getSubFlow("createContactFlowInAFlow");
		createContactFlowInAFlow.initialise();
		createContactFlowInAFlow.process(getTestEvent(systemAContacts, MessageExchangePattern.REQUEST_RESPONSE)).getMessage().getPayload();

		SubflowInterceptingChainLifecycleWrapper createContactFlowInBFlow = getSubFlow("createContactFlowInBFlow");
		createContactFlowInBFlow.initialise();
		createContactFlowInBFlow.process(getTestEvent(systemBContacts, MessageExchangePattern.REQUEST_RESPONSE)).getMessage().getPayload();

		/*
		 * Execution
		 */
		Flow mainFlow = getFlow("mainFlow");
		mainFlow.process(getTestEvent("Hello World!", MessageExchangePattern.REQUEST_RESPONSE));

		/*
		 * Assertions
		 */
		SubflowInterceptingChainLifecycleWrapper retrieveContactFromAFlow = getSubFlow("retrieveContactFromAFlow");
		retrieveContactFromAFlow.initialise();
		Map<?, ?> retrievedContactFromA = (Map<?, ?>) retrieveContactFromAFlow.process(getTestEvent(johnDoe.build(), MessageExchangePattern.REQUEST_RESPONSE)).getMessage().getPayload();

		SubflowInterceptingChainLifecycleWrapper retrieveContactFromBFlow = getSubFlow("retrieveContactFromBFlow");
		retrieveContactFromBFlow.initialise();
		Map<?, ?> retrievedContactFromB = (Map<?, ?>) retrieveContactFromBFlow.process(getTestEvent(johnDoe.build(), MessageExchangePattern.REQUEST_RESPONSE)).getMessage()
				.getPayload();

		final MapDifference<Object, Object> mapsDifference = Maps.difference(retrievedContactFromA, retrievedContactFromB);
		Assert.assertTrue("Some contacts are not synchronized between systems. " + mapsDifference.toString(), mapsDifference.areEqual());
	}

}
