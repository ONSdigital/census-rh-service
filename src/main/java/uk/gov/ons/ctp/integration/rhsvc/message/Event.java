package uk.gov.ons.ctp.integration.rhsvc.message;

import java.sql.Timestamp;

abstract public class Event {

    private String type;
    private String source;
    private String channel;
    private Timestamp dataTime;
    private String transactionId;
    public String payload;

    final public String getType() {
        return type;
    }

    final public void setType(String type) {
        this.type = type;
    }

    final public String getSource() {
        return source;
    }

    final public void setSource(String source) {
        this.source = source;
    }

    final public String getChannel() {
        return channel;
    }

    final public void setChannel(String channel) {
        this.channel = channel;
    }

    final public Timestamp getDataTime() {
        return dataTime;
    }

    final public void setDataTime(Timestamp dataTime) {
        this.dataTime = dataTime;
    }

    final public String getTransactionId() {
        return transactionId;
    }

    final public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    abstract public String getPayload();

    abstract public void setPayload(String payload);

}
