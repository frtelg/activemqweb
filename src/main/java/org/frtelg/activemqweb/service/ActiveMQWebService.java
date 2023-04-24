package org.frtelg.activemqweb.service;

import org.frtelg.activemqweb.exception.NotFoundException;
import com.typesafe.config.Config;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.*;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.frtelg.activemqweb.functional.ThrowingConsumer;
import org.frtelg.activemqweb.functional.ThrowingFunction;
import org.springframework.stereotype.Service;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

import static org.apache.activemq.command.ActiveMQDestination.QUEUE_TYPE;

@Service
@Slf4j
public class ActiveMQWebService {
    private ActiveMQConnectionFactory connectionFactory;
    private final Config connectionConfig;

    public ActiveMQWebService(Config connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    public void connect(String brokerUrl, String userName, String password) {
        var prefetchPolicy = new ActiveMQPrefetchPolicy();
        prefetchPolicy.setQueueBrowserPrefetch(1);

        log.info("Connecting to broker {}...", brokerUrl);
        this.connectionFactory = new ActiveMQConnectionFactory(userName, password, brokerUrl);
        connectionFactory.setTrustAllPackages(false);
        connectionFactory.setTrustedPackages(List.of("org.frtelg"));
        connectionFactory.setPrefetchPolicy(prefetchPolicy);
        log.info("Connection successful");
    }

    public String connect(String connectionName) {
        var config = connectionConfig.getConfig(connectionName);
        var url = config.getString("amqurl");
        var userName = config.getString("username");
        var password = config.getString("password");

        connect(url, userName, password);

        return url;
    }

    public void disconnect() {
        this.connectionFactory = null;
    }

    public boolean isConnected() {
        return connectionFactory != null;
    }

    public String getCurrentBrokerUrl() {
        return connectionFactory.getBrokerURL();
    }

    public List<Queue> getAllQueues() {
        return withConnection(connection -> connection.getDestinationSource()
                .getQueues()
                .stream()
                .map(Queue.class::cast)
                .toList()
        );
    }

    public void addQueue(String name) {
        var queue = new ActiveMQQueue(name);

        withSession(session -> session.createConsumer(queue));
    }

    @SneakyThrows
    public List<ActiveMQTextMessage> getMessages(String queueName) {
        return withSession(session -> {
            var queue = new ActiveMQQueue(queueName);

            log.info("Getting messages for queue {}", queueName);
            log.info("Getting messages for queue {}", queue);

            ActiveMQQueueBrowser browser = (ActiveMQQueueBrowser) session.createBrowser(queue);

            try {
                return getAllMessagesOnQueue(browser);
            } finally {
                browser.close();
            }
        });
    }

    public ActiveMQTextMessage sendMessage(String messageBody,
                                           Map<String, String> properties,
                                           String destination,
                                           Optional<String> jmsCorrelationId,
                                           Optional<String> jmsReplyTo) {
        Destination parsedDestintation = ActiveMQDestination.createDestination(destination, QUEUE_TYPE);
        return withSession(session -> {
            if (parsedDestintation instanceof Topic t) {
                return sendToTopic(messageBody, properties, t, jmsCorrelationId, jmsReplyTo);
            }

            if (parsedDestintation instanceof Queue q) {
                return sendToQueue(messageBody, properties, q, jmsCorrelationId, jmsReplyTo);
            }

            throw new IllegalArgumentException("Cannot parse destination " + destination);
        });
    }

    public List<String> listConnections() {
        return connectionConfig
                .entrySet()
                .stream()
                .map(Map.Entry::getKey)
                .map(n -> n.split("[.]")[0])
                .distinct()
                .sorted()
                .toList();
    }

    @SneakyThrows
    private <T> T withConnection(ThrowingFunction<ActiveMQConnection, T> withConnectionFunction) {
        if (connectionFactory == null) {
            throw new NotFoundException("No ActiveMQ connection was configured");
        }

        ActiveMQConnection connection = (ActiveMQConnection) connectionFactory.createConnection();

        try {
            connection.start();
            return withConnectionFunction.apply(connection);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        } finally {
            connection.close();
        }
    }

    private <T> T withSession(ThrowingFunction<ActiveMQSession, T> withSessionFunction) {
        return withConnection(connection -> {
            ActiveMQSession session = (ActiveMQSession) connection.createSession(false, 2);
            try {
                return withSessionFunction.apply(session);
            } finally {
                session.close();
            }
        });
    }

    private ActiveMQTextMessage sendToTopic(String messageBody,
                                            Map<String, String> properties,
                                            Topic topic,
                                            Optional<String> jmsCorrelationId,
                                            Optional<String> jmsReplyTo) {
        return withSession(session -> {
            var publisher = session.createPublisher(topic);

            try {
                return send(messageBody, properties, publisher, jmsCorrelationId, jmsReplyTo);
            } finally {
                publisher.close();
            }
        });
    }

    private ActiveMQTextMessage sendToQueue(String messageBody,
                                            Map<String, String> properties,
                                            Queue queue,
                                            Optional<String> jmsCorrelationId,
                                            Optional<String> jmsReplyTo) {
        return withSession(session -> {
            var sender = session.createSender(queue);
            try {
                return send(messageBody, properties, sender, jmsCorrelationId, jmsReplyTo);
            } finally {
                sender.close();
            }
        });
    }

    private ActiveMQTextMessage send(String messageBody,
                                     Map<String, String> properties,
                                     MessageProducer messageProducer,
                                     Optional<String> jmsCorrelationId,
                                     Optional<String> jmsReplyTo) {
        return withSession(session -> {
            var message = session.createTextMessage();
            message.setText(messageBody);
            jmsCorrelationId.ifPresent(ThrowingConsumer.wrapCheckedException(message::setJMSCorrelationID));
            jmsReplyTo.ifPresent(ThrowingConsumer.wrapCheckedException(replyTo -> message.setJMSReplyTo(ActiveMQDestination.createDestination(replyTo, QUEUE_TYPE))));

            properties.forEach((k, v) -> {
                try {
                    message.setStringProperty(k, v);
                } catch (JMSException e) {
                    throw new UnexpectedException(e);
                }
            });

            messageProducer.send(message);

            return (ActiveMQTextMessage) message;
        });
    }

    @SneakyThrows
    private List<ActiveMQTextMessage> getAllMessagesOnQueue(ActiveMQQueueBrowser browser) {
        var it = browser.getEnumeration()
                .asIterator();

        List<ActiveMQTextMessage> messagesCollector = new ArrayList<>();

        while (it.hasNext()) {
            try {
                messagesCollector.add((ActiveMQTextMessage) it.next());
            } catch (ClassCastException e) {
                log.error("Not possible to cast message as ActiveMQTextMessage", e);
            }
        }

        return messagesCollector;
    }
}
