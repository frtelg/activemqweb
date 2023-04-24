package org.frtelg.activemqweb.dto;

import java.util.Map;
import java.util.Optional;

public record SendMessageRequest(String body,
                                 Map<String, String> properties,
                                 String destination,
                                 Optional<String> jmsCorrelationId,
                                 Optional<String> jmsReplyTo) {}
