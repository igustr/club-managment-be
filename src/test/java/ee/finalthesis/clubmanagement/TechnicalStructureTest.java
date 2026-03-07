package ee.finalthesis.clubmanagement;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packagesOf = ClubManagementApplication.class,
    importOptions = DoNotIncludeTests.class)
class TechnicalStructureTest {

  @ArchTest
  static final ArchRule respectsTechnicalArchitectureLayers =
      layeredArchitecture()
          .consideringAllDependencies()
          .layer("Config")
          .definedBy("..config..")
          .layer("Security")
          .definedBy("..security..")
          .layer("Controller")
          .definedBy("..api.controller..")
          .layer("Service")
          .definedBy("..service..")
          .layer("Persistence")
          .definedBy("..repository..")
          .layer("Domain")
          .definedBy("..domain..")
          .optionalLayer("Common")
          .definedBy("..common..")
          .whereLayer("Controller")
          .mayOnlyBeAccessedByLayers("Config")
          .whereLayer("Service")
          .mayOnlyBeAccessedByLayers("Controller", "Config", "Security")
          .whereLayer("Persistence")
          .mayOnlyBeAccessedByLayers("Service", "Config", "Security")
          .whereLayer("Config")
          .mayOnlyBeAccessedByLayers("Service", "Security")
          .whereLayer("Domain")
          .mayOnlyBeAccessedByLayers(
              "Persistence", "Service", "Controller", "Config", "Security", "Common")
          .ignoreDependency(resideInAPackage("..security.."), alwaysTrue())
          .ignoreDependency(resideInAPackage("..common.."), alwaysTrue());
}
