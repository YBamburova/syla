package com.syla.core;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class BuildConfiguration {
    @Bean Clock clock() { return Clock.systemUTC(); }
}
