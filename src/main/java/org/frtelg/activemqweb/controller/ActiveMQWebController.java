package org.frtelg.activemqweb.controller;

import org.frtelg.activemqweb.dto.ConnectRequest;
import org.frtelg.activemqweb.dto.ConnectionResponse;
import org.frtelg.activemqweb.dto.JmsMessage;
import org.frtelg.activemqweb.dto.SendMessageRequest;
import org.frtelg.activemqweb.functional.ThrowingFunction;
import org.frtelg.activemqweb.service.ActiveMQWebService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.jms.Queue;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping(value = "/api", produces="application/json")
@CrossOrigin(origins = "*")
public class ActiveMQWebController {

    private final ActiveMQWebService jmsService;

    public ActiveMQWebController(ActiveMQWebService jmsService) {
        this.jmsService = jmsService;
    }

    @GetMapping("/queue")
    public ResponseEntity<List<String>> getAllQueues() {
        var queues = jmsService.getAllQueues();
        var queueNames = queues.stream()
                .map(ThrowingFunction.wrapCheckedException(Queue::getQueueName))
                .toList();

        return ResponseEntity.ok(queueNames);
    }

    @PostMapping("/queue")
    public ResponseEntity<Object> addQueue(@RequestParam @NotNull String queueName) {
        jmsService.addQueue(queueName);
        return ResponseEntity.ok()
                .build();
    }

    @GetMapping("/message")
    public ResponseEntity<List<JmsMessage>> getMessages(@RequestParam @NotNull String queueName) {
        var result = jmsService.getMessages(queueName);
        var responseBody = result.stream()
                .map(JmsMessage::of)
                .toList();

        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/send")
    public ResponseEntity<JmsMessage> sendMessage(@RequestBody SendMessageRequest sendMessageRequest) {
        var result = jmsService.sendMessage(sendMessageRequest.body(), sendMessageRequest.properties(),
                sendMessageRequest.destination(), sendMessageRequest.jmsCorrelationId(),
                sendMessageRequest.jmsReplyTo());

        var responseBody = JmsMessage.of(result);

        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/connect")
    public ResponseEntity<ConnectionResponse> connect(@RequestBody ConnectRequest request) {
        jmsService.connect(request.brokerUrl(), request.userName(), request.password());

        return ResponseEntity.ok(ConnectionResponse.connected(request.brokerUrl()));
    }

    @DeleteMapping("/connection")
    public ResponseEntity<Object> disconnect() {
        jmsService.disconnect();

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/connect")
    public ResponseEntity<ConnectionResponse> connectToConfiguredConnection(@RequestParam String connectionName) {
        var brokerUrl = jmsService.connect(connectionName);
        return ResponseEntity.ok(ConnectionResponse.connected(brokerUrl));
    }

    @GetMapping("/connected")
    public ResponseEntity<ConnectionResponse> getConnectionStatus() {
        boolean isConnected = jmsService.isConnected();

        if (isConnected) {
            var brokerUrl = jmsService.getCurrentBrokerUrl();
            return ResponseEntity.ok(ConnectionResponse.connected(brokerUrl));
        } else {
            return ResponseEntity.ok(ConnectionResponse.notConnected());
        }
    }

    @GetMapping("/connection")
    public ResponseEntity<List<String>> getConnections() {
        var connections = jmsService.listConnections();
        return ResponseEntity.ok(connections);
    }

}
