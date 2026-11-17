package br.com.curralzap.backend.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "br.com.curralzap.backend",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ModularMonolithArchitectureTest {

    private static final String BASE_PACKAGE = "br.com.curralzap.backend";
    private static final Path MAIN_SOURCE_ROOT = Path.of("src/main/java/br/com/curralzap/backend");
    private static final List<String> REQUIRED_BOUNDED_CONTEXTS = List.of(
            "auth",
            "billing",
            "users",
            "farms",
            "animals",
            "lots",
            "weighings",
            "feeding",
            "occurrences",
            "vaccines",
            "medications",
            "offers",
            "whatsapp",
            "notifications",
            "shared"
    );
    private static final List<String> REQUIRED_LAYERS = List.of("api", "application", "domain", "infrastructure");

    @Test
    void requiredBoundedContextsExposeTheExpectedLayerSkeleton() {
        assertThat(Files.exists(MAIN_SOURCE_ROOT.resolve("CurralZapBackendApplication.java"))).isTrue();

        for (String boundedContext : REQUIRED_BOUNDED_CONTEXTS) {
            Path contextRoot = MAIN_SOURCE_ROOT.resolve(boundedContext);
            assertThat(Files.isDirectory(contextRoot))
                    .as("bounded context %s should exist", boundedContext)
                    .isTrue();

            for (String layer : REQUIRED_LAYERS) {
                assertThat(Files.isDirectory(contextRoot.resolve(layer)))
                        .as("bounded context %s should expose %s", boundedContext, layer)
                        .isTrue();
            }
        }
    }

    @ArchTest
    static final ArchRule domain_does_not_depend_on_api_or_infrastructure =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..api..", "..infrastructure..");

    @ArchTest
    static final ArchRule application_does_not_depend_on_api =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..api..");

    @ArchTest
    static final ArchRule api_does_not_access_domain_or_infrastructure =
            classes()
                    .that().resideInAPackage("..api..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "java..",
                            "jakarta..",
                            "org.springframework..",
                            "org.slf4j..",
                            BASE_PACKAGE + "..api..",
                            BASE_PACKAGE + "..application..",
                            BASE_PACKAGE + ".shared..",
                            BASE_PACKAGE
                    );

    @ArchTest
    static final ArchRule controllers_must_live_in_api_packages =
            classes()
                    .that().areAnnotatedWith(RestController.class)
                    .or().areAnnotatedWith(Controller.class)
                    .should().resideInAPackage("..api..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule bounded_contexts_cannot_depend_on_other_contexts_api_or_infrastructure =
            noClasses()
                    .that().resideOutsideOfPackage("..shared..")
                    .and().resideOutsideOfPackage("..api..")
                    .and().resideOutsideOfPackage("..infrastructure..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..auth.api..", "..auth.infrastructure..",
                            "..billing.api..", "..billing.infrastructure..",
                            "..users.api..", "..users.infrastructure..",
                            "..farms.api..", "..farms.infrastructure..",
                            "..animals.api..", "..animals.infrastructure..",
                            "..lots.api..", "..lots.infrastructure..",
                            "..weighings.api..", "..weighings.infrastructure..",
                            "..feeding.api..", "..feeding.infrastructure..",
                            "..occurrences.api..", "..occurrences.infrastructure..",
                            "..vaccines.api..", "..vaccines.infrastructure..",
                            "..medications.api..", "..medications.infrastructure..",
                            "..offers.api..", "..offers.infrastructure..",
                            "..whatsapp.api..", "..whatsapp.infrastructure..",
                            "..notifications.api..", "..notifications.infrastructure.."
                    );
}
