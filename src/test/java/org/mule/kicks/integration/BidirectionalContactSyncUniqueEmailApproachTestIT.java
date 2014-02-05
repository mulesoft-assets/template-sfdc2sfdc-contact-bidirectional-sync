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
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.Schedulers;
import org.mule.construct.Flow;
import org.mule.kicks.builders.ContactBuilder;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Prober;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the flows for this Mule Kick that make calls to external systems.
 * 
 */
@SuppressWarnings("unchecked")
public class BidirectionalContactSyncUniqueEmailApproachTestIT extends AbstractKickTestCase {

	private static final String KICK_NAME = "sfdc2sfdc-bidirectional-contact-sync";
	private static final String SEPARATOR = "-";

	private static SubflowInterceptingChainLifecycleWrapper createContactInAFlow;
	private static SubflowInterceptingChainLifecycleWrapper createContactInBFlow;
	private static SubflowInterceptingChainLifecycleWrapper deleteContactFromAFlow;
	private static SubflowInterceptingChainLifecycleWrapper deleteContactFromBFlow;
	private static SubflowInterceptingChainLifecycleWrapper retrieveContactFromAFlow;
	private static SubflowInterceptingChainLifecycleWrapper retrieveContactFromBFlow;
	private static SubflowInterceptingChainLifecycleWrapper queryContactFromAFlow;
	private static SubflowInterceptingChainLifecycleWrapper queryContactFromBFlow;
	private static Flow mainFlow;

	private final Prober workingPollProber = new PollingProber(1200000l, 1000l);

	private List<String> contactsCreatedInA;
	private List<String> contactsCreatedInB;

	@BeforeClass
	public static void beforeTestClass() {
		System.setProperty("mule.env", "test");

		// Setting Default Watermark Expression to query SFDC with LastModifiedDate greater than ten seconds before current time
		System.setProperty("watermark.default.expression", "#[groovy: new Date(System.currentTimeMillis() - 1000000).format(\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\", TimeZone.getTimeZone('UTC'))]");

		// Setting Polling Frecuency to 10 seconds period
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

		// Flow for querying the contact in source system
		queryContactFromAFlow = getSubFlow("queryContactFromAFlow");
		queryContactFromAFlow.initialise();

		// Flow for querying the contact in target system
		queryContactFromBFlow = getSubFlow("queryContactFromBFlow");
		queryContactFromBFlow.initialise();

		mainFlow = getFlow("mainFlow");

		contactsCreatedInA = new ArrayList<String>();
		contactsCreatedInB = new ArrayList<String>();
	}

	@After
	public void tearDown() throws MuleException, Exception {
		final List<String> idList = new ArrayList<String>();
		for (String contact : contactsCreatedInA) {
			idList.add(contact);
		}
		deleteContactFromAFlow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
		idList.clear();
		for (String contact : contactsCreatedInB) {
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
		final ContactBuilder johnDoe = aContact().with("FirstName", "John")
													.with("LastName", "Doe")
													.with("MailingCountry", "US")
													.with("Email", KICK_NAME + SEPARATOR + System.currentTimeMillis() + "john.doe@mail.com");

		ContactBuilder johnDoeWithBasicDescription = johnDoe.with("Description", "Please enter your description here");
		ContactBuilder johnDoeWithUpdatedDescription = johnDoe.with("Description", "John Doe is the man!");

		// Create test contacts in Salesforce sandboxes
		List<Map<String, String>> salesforceAContacts = new ArrayList<Map<String, String>>();
		salesforceAContacts.add(johnDoeWithBasicDescription.build());

		List<Map<String, String>> salesforceBContacts = new ArrayList<Map<String, String>>();
		salesforceBContacts.add(johnDoeWithUpdatedDescription.build());

		final List<SaveResult> payloadAfterExecutionInA = (List<SaveResult>) createContactInAFlow.process(getTestEvent(salesforceAContacts, MessageExchangePattern.REQUEST_RESPONSE))
																									.getMessage()
																									.getPayload();

		final List<SaveResult> payloadAfterExecutionInB = (List<SaveResult>) createContactInBFlow.process(getTestEvent(salesforceBContacts, MessageExchangePattern.REQUEST_RESPONSE))
																									.getMessage()
																									.getPayload();

		contactsCreatedInA.add(payloadAfterExecutionInA.get(0)
														.getId());
		contactsCreatedInB.add(payloadAfterExecutionInB.get(0)
														.getId());

		checkThatTheContactsHaveBeenSuccessfullyCreated(johnDoe);

		List<Map<String, String>> contactInB = (List<Map<String, String>>) retrieveContactFromBFlow.process(
				getTestEvent(payloadAfterExecutionInB.get(0)
														.getId(), MessageExchangePattern.REQUEST_RESPONSE))
																									.getMessage()
																									.getPayload();
		Collection<Map<String, String>> contactUpdatesList = new ArrayList<Map<String, String>>();
		contactUpdatesList.add(johnDoeWithUpdatedDescription.with("LastModifiedDate", contactInB.get(0)
																								.get("LastModifiedDate"))
															.build());

		// Prepare the Mule Event to be processed
		Object payload = contactUpdatesList;
		MuleMessage message = new DefaultMuleMessage(payload, muleContext);
		message.setInvocationProperty("sourceSystem", "B");
		MuleEvent event = getTestEvent("");
		event.setMessage(message);

		/*
		 * Execution
		 */

		mainFlow.process(event);

		/*
		 * Assertions
		 */

		workingPollProber.check(new AssertionProbe() {
			@Override
			public void assertSatisfied() throws Exception {
				Map<String, String> retrievedContactFromA = (Map<String, String>) queryContactFromAFlow.process(getTestEvent(johnDoe.build(), MessageExchangePattern.REQUEST_RESPONSE))
																										.getMessage()
																										.getPayload();
				Map<String, String> retrievedContactFromB = (Map<String, String>) queryContactFromBFlow.process(getTestEvent(johnDoe.build(), MessageExchangePattern.REQUEST_RESPONSE))
																										.getMessage()
																										.getPayload();

				final MapDifference<String, String> mapsDifference = Maps.difference(retrievedContactFromA, retrievedContactFromB);
				Assert.assertTrue("Some contacts are not synchronized between systems. " + mapsDifference.toString(), mapsDifference.areEqual());
			}
		});
	}

	@Test
	public void noChangesOccurWhenTheLastUpdateForAContactHasBeenMadeInTheTargetSystem() throws Exception {

		/*
		 * Preparation
		 */

		// Build test contacts
		final ContactBuilder johnDoe = aContact().with("FirstName", "John")
													.with("LastName", "Doe")
													.with("MailingCountry", "US")
													.with("Email", KICK_NAME + SEPARATOR + System.currentTimeMillis() + "john.doe@mail.com");

		ContactBuilder johnDoeWithBasicDescription = johnDoe.with("Description", "Please enter your description here");
		final ContactBuilder johnDoeWithUpdatedDescription = johnDoe.with("Description", "John Doe is the man!");

		// Create test contacts in Salesforce sandboxes
		List<Map<String, String>> salesforceAContacts = new ArrayList<Map<String, String>>();
		salesforceAContacts.add(johnDoeWithBasicDescription.build());

		List<Map<String, String>> salesforceBContacts = new ArrayList<Map<String, String>>();
		salesforceBContacts.add(johnDoeWithUpdatedDescription.build());

		final List<SaveResult> payloadAfterExecutionInA = (List<SaveResult>) createContactInAFlow.process(getTestEvent(salesforceAContacts, MessageExchangePattern.REQUEST_RESPONSE))
																									.getMessage()
																									.getPayload();

		final List<SaveResult> payloadAfterExecutionInB = (List<SaveResult>) createContactInBFlow.process(getTestEvent(salesforceBContacts, MessageExchangePattern.REQUEST_RESPONSE))
																									.getMessage()
																									.getPayload();

		contactsCreatedInA.add(payloadAfterExecutionInA.get(0)
														.getId());
		contactsCreatedInB.add(payloadAfterExecutionInB.get(0)
														.getId());

		checkThatTheContactsHaveBeenSuccessfullyCreated(johnDoe);

		List<Map<String, String>> contactInA = (List<Map<String, String>>) retrieveContactFromAFlow.process(
				getTestEvent(payloadAfterExecutionInA.get(0)
														.getId(), MessageExchangePattern.REQUEST_RESPONSE))
																									.getMessage()
																									.getPayload();
		Collection<Map<String, String>> contactUpdatesList = new ArrayList<Map<String, String>>();
		contactUpdatesList.add(johnDoeWithBasicDescription.with("LastModifiedDate", contactInA.get(0)
																								.get("LastModifiedDate"))
															.build());

		// Prepare the Mule Event to be processed
		Object payload = contactUpdatesList;
		MuleMessage message = new DefaultMuleMessage(payload, muleContext);
		message.setInvocationProperty("sourceSystem", "A");
		MuleEvent event = getTestEvent("");
		event.setMessage(message);

		/*
		 * Execution
		 */

		mainFlow.process(event);

		/*
		 * Assertions
		 */

		workingPollProber.check(new AssertionProbe() {
			@Override
			public void assertSatisfied() throws Exception {
				Map<String, String> retrievedContactFromB = (Map<String, String>) queryContactFromBFlow.process(getTestEvent(johnDoe.build(), MessageExchangePattern.REQUEST_RESPONSE))
																										.getMessage()
																										.getPayload();

				Assert.assertTrue("The Contact in B has been updated when it should not", (johnDoeWithUpdatedDescription.build()
																														.get("Description").equals(retrievedContactFromB.get("Description"))));
			}
		});
	}

	private void checkThatTheContactsHaveBeenSuccessfullyCreated(final ContactBuilder contact) {
		workingPollProber.check(new AssertionProbe() {
			@Override
			public void assertSatisfied() throws Exception {
				Map<String, String> retrievedContactFromA = (Map<String, String>) queryContactFromAFlow.process(getTestEvent(contact.build(), MessageExchangePattern.REQUEST_RESPONSE))
																										.getMessage()
																										.getPayload();
				Map<String, String> retrievedContactFromB = (Map<String, String>) queryContactFromBFlow.process(getTestEvent(contact.build(), MessageExchangePattern.REQUEST_RESPONSE))
																										.getMessage()
																										.getPayload();

				assertNotNull("Contact in A is null", retrievedContactFromA);
				assertNotNull("Contact in B is null", retrievedContactFromB);
			}
		});
	}

	// ***************************************************************
	// ======== Schedulers management methods ========
	// ***************************************************************

	private void stopSchedulers() throws MuleException {
		final Collection<Scheduler> schedulers = muleContext.getRegistry()
															.lookupScheduler(Schedulers.allPollSchedulers());

		for (final Scheduler scheduler : schedulers) {
			scheduler.stop();
		}
	}
}
