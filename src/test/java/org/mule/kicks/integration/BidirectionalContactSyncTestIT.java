package org.mule.kicks.integration;

import static org.mule.kicks.builders.ContactBuilder.aContact;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.construct.Flow;
import org.mule.kicks.builders.ContactBuilder;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;

/**
 * The objective of this class is to validate the correct behavior of the flows
 * for this Mule Kick that make calls to external systems.
 * 
 */
public class BidirectionalContactSyncTestIT extends AbstractKickTestCase {

	private static SubflowInterceptingChainLifecycleWrapper checkContactflow;
	
	@Before
	public void setUp() throws InitialisationException {
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("createContactFlow");
		flow.initialise();
	}

	@After
	public void tearDown() {
	}

	@Test
	public void whenUpdatingAContactInSourceSystemTheBelongingContactgetsUpdatedInTargetSystem() throws Exception {
		// Preparation
		ContactBuilder johnDoe = aContact()
				.with("FirstName", "John")
				.with("LastName", "Doe")
				.with("Email", "john.doe@mail.com");
		
		Map<String, String> johnDoeWithBasicDescription = johnDoe
				.with("Description", "Please enter your description here")
				.build();
		Map<String, String> johnDoeWithUpdatedDescription = johnDoe
				.with("Description", "John Doe is the man!")
				.build();
		
		// Execution
		Flow flow = getFlow("mainFlow");
		flow.process(getTestEvent("Hello!", MessageExchangePattern.REQUEST_RESPONSE));


        // Assertions

	}

}
