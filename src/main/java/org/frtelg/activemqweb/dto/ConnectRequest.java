package org.frtelg.activemqweb.dto;

public record ConnectRequest(
        String brokerUrl,
        String userName,
        String password
) {}
