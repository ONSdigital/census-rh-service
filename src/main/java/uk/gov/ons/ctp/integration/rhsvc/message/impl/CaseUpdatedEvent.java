package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import uk.gov.ons.ctp.integration.rhsvc.message.Event;

public class CaseUpdatedEvent extends Event {

    public String getPayload() {
        return super.payload;
    }

    public void setPayload(String payload) {
        super.payload = payload;
    }
}
