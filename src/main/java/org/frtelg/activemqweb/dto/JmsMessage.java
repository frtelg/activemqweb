package org.frtelg.activemqweb.dto;

import lombok.SneakyThrows;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.Message;
import java.util.HashMap;
import java.util.Map;

public record JmsMessage(JmsHeaders headers, Map<String, Object> properties, String body) {
    @SneakyThrows
    public static JmsMessage of(ActiveMQTextMessage textMessage) {
        var headers = JmsHeaders.of(textMessage);
        var properties = parseProperties(textMessage);
        var body = textMessage.getText();

        return new JmsMessage(headers, properties, body);
    }

    @SneakyThrows
    private static Map<String, Object> parseProperties(Message message) {
        var propertyNames = message.getPropertyNames().asIterator();
        Map<String, Object> propertiesCollector = new HashMap<>();

        while (propertyNames.hasNext()) {
            var next = (String) propertyNames.next();
            propertiesCollector.put(next, message.getObjectProperty(next));
        }

        return propertiesCollector;
    }
}
