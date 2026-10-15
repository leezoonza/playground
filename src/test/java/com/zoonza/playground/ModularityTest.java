package com.zoonza.playground;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {
    private final ApplicationModules modules = ApplicationModules.of(PlaygroundApplication.class);

    @Test
    @DisplayName("모듈 간 의존성 규칙을 위반하지 않는다")
    void verifyModularStructure() {
        modules.verify();
    }
}
