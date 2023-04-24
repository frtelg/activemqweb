package org.frtelg.activemqweb.dto;

import org.apache.activemq.command.ActiveMQMessage;

import java.util.Optional;

public record JmsHeaders(Optional<String> jmsDestination,
                         Optional<String> jmsReplyTo,
                         String jmsType,
                         int jmsDeliveryMode,
                         int jmsPriority,
                         String jmsMessageId,
                         long jmsTimestamp,
                         Optional<String> jmsCorrelationId,
                         long jmsExpiration,
                         boolean jmsRedelivered,
                         long jmsActiveMQBrokerInTime,
                         long jmsActiveMQBrokerOutTime) {
    public static JmsHeaders of(ActiveMQMessage message) {
        return new JmsHeaders(
                Optional.ofNullable(message.getJMSDestination()).map(Object::toString),
                Optional.ofNullable(message.getJMSReplyTo()).map(Object::toString),
                message.getJMSType(),
                message.getJMSDeliveryMode(),
                message.getJMSPriority(),
                message.getJMSMessageID(),
                message.getJMSTimestamp(),
                Optional.ofNullable(message.getJMSCorrelationID()),
                message.getJMSExpiration(),
                message.getJMSRedelivered(),
                message.getBrokerInTime(),
                message.getBrokerOutTime()
        );
    }
}
