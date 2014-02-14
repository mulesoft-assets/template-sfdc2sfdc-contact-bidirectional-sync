package org.mule.kicks.workaround;

import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.MuleTestUtils;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.util.UUID;

public class BatchAutoreferenceWorkaround extends AbstractMessageTransformer {

	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
		Object payload = message.getPayload();
		MuleMessage newMessage = new DefaultMuleMessage(payload, muleContext);
		newMessage.setCorrelationId(null);
		((DefaultMuleMessage) message).setUniqueId(UUID.getUUID());
		
		MuleEvent event = null;
		try {
			event = MuleTestUtils.getTestEvent("", MessageExchangePattern.REQUEST_RESPONSE, muleContext);
			event.setMessage(newMessage);
		} catch (Exception e) {
			logger.error("Error produced while creating a new Mule Event");
		}
		return event;
	}

}
