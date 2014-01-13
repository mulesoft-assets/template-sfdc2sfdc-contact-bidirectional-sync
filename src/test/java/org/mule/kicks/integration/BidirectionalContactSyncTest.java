package org.mule.kicks.integration;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.construct.Flow;
import org.mule.munit.runner.functional.FunctionalMunitSuite;

/**
 * The objective of this class is to validate the correct behavior of the flows
 * for this Mule Kick that make calls to external systems.
 * 
 */
public class BidirectionalContactSyncTest extends FunctionalMunitSuite {

	@Before
	public void setUp() {

	}

	@After
	public void tearDown() {
	}

	@Test
	public void whenUpdatingAContactInSourceSystemTheBelongingContactgetsUpdatedInTargetSystem() throws Exception {
		// Preparation
		//whenMessageProcessor("...").ofNamespace("...").thenReturnSameEvent();
		
		// Execution
        runFlow("mainFlow", testEvent(null));

        // Assertions
        verifyCallOfMessageProcessor("delete-entity").ofNamespace("clarizen").times(0);
        verifyCallOfMessageProcessor("create-notification").ofNamespace("cloudhub").times(1);
	}

}
