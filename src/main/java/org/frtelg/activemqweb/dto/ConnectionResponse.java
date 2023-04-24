package org.frtelg.activemqweb.dto;

import java.util.Optional;

public record ConnectionResponse(Optional<String> brokerUrl, boolean connected) {
    public static ConnectionResponse connected(String brokerUrl) {
        return new ConnectionResponse(Optional.of(brokerUrl), true);
    }

    public static ConnectionResponse notConnected() {
        return new ConnectionResponse(Optional.empty(), false);
    }
}
