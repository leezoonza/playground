package com.zoonza.playground;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;


class LayerDependencyTest {
    private static final String DOMAIN = "Domain";
    private static final String APPLICATION = "Application";
    private static final String PRESENTATION = "Presentation";
    private static final String INFRASTRUCTURE = "Infrastructure";

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.zoonza.playground");

    @Test
    @DisplayName("레이어는 정해진 방향으로만 의존한다")
    void layerDependencies() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer(DOMAIN).definedBy("..internal.domain..")
                .layer(APPLICATION).definedBy("..internal.application..")
                .layer(PRESENTATION).definedBy("..internal.presentation..")
                .layer(INFRASTRUCTURE).definedBy("..internal.infrastructure..")
                .whereLayer(DOMAIN).mayNotAccessAnyLayer()
                .whereLayer(APPLICATION).mayOnlyAccessLayers(DOMAIN)
                .whereLayer(PRESENTATION).mayOnlyAccessLayers(DOMAIN, APPLICATION)
                .whereLayer(INFRASTRUCTURE).mayOnlyAccessLayers(DOMAIN, APPLICATION)
                .check(classes);
    }
}
