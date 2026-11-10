package com.syla.core.identity.internal;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("syla.identity")
record IdentityProperties(URI issuer, String adminClientId, String adminClientSecret) {}
