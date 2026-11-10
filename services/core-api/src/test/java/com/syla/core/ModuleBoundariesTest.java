package com.syla.core;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleBoundariesTest {

    @Test
    void modulesRespectTheirPublicApis() {
        var modules = ApplicationModules.of(CoreApplication.class);
        // Fail if detection stops finding the module; verify must not pass on an empty model.
        assertThat(modules.stream().map(module -> module.getIdentifier().toString())).contains("health");
        modules.verify();
    }
}
