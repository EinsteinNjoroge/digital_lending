package com.digital.lending;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityVerificationTest {

    @Test
    void verifiesApplicationModules() {
        ApplicationModules.of(LendingApplication.class).verify();
    }
}
