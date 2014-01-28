package org.mule.kicks.integration;

import static org.junit.Assert.assertNotNull;
import static org.mule.kicks.builders.ContactBuilder.aContact;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.Schedulers;
import org.mule.kicks.builders.ContactBuilder;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Prober;

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
	
	private static final String KICK_NAME = "sfdc2sfdc-bidirectional-contact-sync";
	private static final String SEPARATOR = "-";
	
	private static SubflowInterceptingChainLifecycleWrapper createContactInAFlow;
	private static SubflowInterceptingChainLifecycleWrapper createContactInBFlow;
	private static SubflowInterceptingChainLifecycleWrapper deleteContactFromAFlow;
	private static SubflowInterceptingChainLifecycleWrapper deleteContactFromBFlow;
	private static SubflowInterceptingChainLifecycleWrapper retrieveContactFromAFlow;
	private static SubflowInterceptingChainLifecycleWrapper retrieveContactFromBFlow;
	
	private final Prober workingPollProber = new PollingProber(120000l,1000l);
	
	private List<String> contactsToBeDeletedInA;
	private List<String> contactsToBeDeletedInB;

	
    @BeforeClass
	public static void beforeTestClass() {
		System.setProperty("mule.env", "test");
		
		//Setting Default Watermark Expression to query SFDC with LastModifiedDate greater than ten seconds before current time
		System.setProperty("watermark.default.expression", "#[groovy: new Date(System.currentTimeMillis() - 10000).format(\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\", TimeZone.getTimeZone('UTC'))]");
		
		//Setting Polling Frecuency to 10 seconds period
    	System.setProperty("polling.frequency", "10000");
	}
	
	@Before
	public void setUp() throws MuleException {
		stopSchedulers();

		// Flow for create contacts in source system
		createContactInAFlow = getSubFlow("createContactInAFlow");
		createContactInAFlow.initialise();
		
		// Flow for create contacts in target system
		createContactInBFlow = getSubFlow("createContactInBFlow");
		createContactInBFlow.initialise();
		
		// Flow for deleting contacts in source system
		deleteContactFromAFlow = getSubFlow("deleteContactFromAFlow");
		deleteContactFromAFlow.initialise();
		
		// Flow for deleting contacts in target system
		deleteContactFromBFlow = getSubFlow("deleteContactFromBFlow");
		deleteContactFromBFlow.initialise();
		
		// Flow for querying the contact in source system
		retrieveContactFromAFlow = getSubFlow("retrieveContactFromAFlow");
		retrieveContactFromAFlow.initialise();
		
		// Flow for querying the contact in target system
		retrieveContactFromBFlow = getSubFlow("retrieveContactFromBFlow");
		retrieveContactFromBFlow.initialise();
		
		contactsToBeDeletedInA = new ArrayList<String>();
		contactsToBeDeletedInB = new ArrayList<String>();
	}

	@After
	public void tearDown() throws MuleException, Exception {
		final List<String> idList = new ArrayList<String>();
		for (String contact : contactsToBeDeletedInA) {
			idList.add(contact);
		}
		deleteContactFromAFlow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
		idList.clear();
		for (String contact : contactsToBeDeletedInB) {
			idList.add(contact);
		}
		deleteContactFromBFlow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}

	@Test
	public void whenUpdatingAContactInSourceSystemTheBelongingContactgetsUpdatedInTargetSystem() throws Exception {

		/*
		 * Preparation
		 */
		
		// Build test contacts
		final ContactBuilder johnDoe = aContact() //
				.with("FirstName", "John") //
				.with("LastName", "Doe") //
				.with("MailingCountry", "US")
				.with("Email", KICK_NAME + SEPARATOR + "john.doe@mail.com");

		Map<String, String> johnDoeWithBasicDescription = johnDoe //
				.with("Description", "Please enter your description here") //
				.build();
		Map<String, String> johnDoeWithUpdatedDescription = johnDoe //
				.with("Description", "John Doe is the man!") //
				.build();

		// Create test contacts in Salesforce sandboxes
		List<Map<String, String>> salesforceAContacts = new ArrayList<Map<String, String>>();
		salesforceAContacts.add(johnDoeWithBasicDescription);

		List<Map<String, String>> salesforceBContacts = new ArrayList<Map<String, String>>();
		salesforceBContacts.add(johnDoeWithUpdatedDescription);

		final List<SaveResult> payloadAfterExecutionInA = (List<SaveResult>) createContactInAFlow.process(getTestEvent(salesforceAContacts, MessageExchangePattern.REQUEST_RESPONSE))
				.getMessage().getPayload();

		final List<SaveResult> payloadAfterExecutionInB = (List<SaveResult>) createContactInBFlow.process(getTestEvent(salesforceBContacts, MessageExchangePattern.REQUEST_RESPONSE))
				.getMessage().getPayload();
		
		contactsToBeDeletedInA.add(payloadAfterExecutionInA.get(0).getId());
		contactsToBeDeletedInB.add(payloadAfterExecutionInB.get(0).getId());
		
		workingPollProber.check(new AssertionProbe() {
			@Override
			public void assertSatisfied() throws Exception {
				Map<String, String> retrievedContactFromA = (Map<String, String>) retrieveContactFromAFlow.process(getTestEvent(johnDoe.build(), MessageExchangePattern.REQUEST_RESPONSE)).getMessage().getPayload();
				Map<String, String> retrievedContactFromB =  (Map<String, String>) retrieveContactFromBFlow.process(getTestEvent(johnDoe.build(), MessageExchangePattern.REQUEST_RESPONSE)).getMessage().getPayload();
				
				assertNotNull("Contact in A is null.", retrievedContactFromA);
				assertNotNull("Contact in B is null.", retrievedContactFromB);
				
				System.out.println("Contact A = " + retrievedContactFromA);
				System.out.println("Contact B = " + retrievedContactFromB);
			}
		});

		startSchedulers();
		
		/*
		 * Assertions
		 */
		workingPollProber.check(new AssertionProbe() {
			@Override
			public void assertSatisfied() throws Exception {
				Map<String, String> retrievedContactFromA = (Map<String, String>) retrieveContactFromAFlow.process(getTestEvent(johnDoe.build(), MessageExchangePattern.REQUEST_RESPONSE)).getMessage().getPayload();
				Map<String, String> retrievedContactFromB =  (Map<String, String>) retrieveContactFromBFlow.process(getTestEvent(johnDoe.build(), MessageExchangePattern.REQUEST_RESPONSE)).getMessage().getPayload();
				
				final MapDifference<String, String> mapsDifference = Maps.difference(retrievedContactFromA, retrievedContactFromB);
				Assert.assertTrue("Some contacts are not synchronized between systems. " + mapsDifference.toString(), mapsDifference.areEqual());
			}
		});
	}
	

    // ***************************************************************
    // ======== Schedulers management methods ========
    // ***************************************************************
	
    private void stopSchedulers() throws MuleException {
        final Collection<Scheduler> schedulers = muleContext.getRegistry().lookupScheduler(Schedulers.allPollSchedulers());

        for (final Scheduler scheduler : schedulers) {
            scheduler.stop();
        }
    }
    
    private void startSchedulers() throws Exception {
        final Collection<Scheduler> schedulers = muleContext.getRegistry().lookupScheduler(Schedulers.allPollSchedulers());

    	for (final Scheduler scheduler : schedulers) {
    		scheduler.schedule();
    	}
    }

}
