package com.yato.urlShortenerb.config;

import io.lettuce.core.ClientOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientOptionsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisConfig {

    // While Redis is unreachable, fail commands immediately instead of queueing
    // them until the command timeout, so the fallbacks (rate limiting allows the
    // request, cache reads go to the database) add no latency during an outage
    @Bean
    LettuceClientOptionsBuilderCustomizer rejectCommandsWhileDisconnected() {
        return builder -> builder.disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS);
    }
}
