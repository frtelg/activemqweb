package org.frtelg.activemqweb.configuration;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

@Slf4j
@Configuration
public class ConnectionConfiguration {
    private final Config config;

    public ConnectionConfiguration(@Value("${activemqweb.config-location}") Path configLocation) {
        File configFile = configLocation.toFile();

        if (configFile.exists()) {
            this.config = ConfigFactory.parseFile(configFile)
                    .getConfig("broker");
        } else {
            log.info("No configuration file found at {}, loading an empty config", configLocation);
            this.config = ConfigFactory.empty()
                    .withValue("broker", ConfigValueFactory.fromMap(Collections.emptyMap()));
        }
    }

    @Bean
    public Config connectionConfig() {
        return config;
    }
}
