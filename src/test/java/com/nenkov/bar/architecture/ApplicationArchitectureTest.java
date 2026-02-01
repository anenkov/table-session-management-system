package com.nenkov.bar.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.nenkov.bar")
class ApplicationArchitectureTest {

  @ArchTest
  static final ArchRule application_must_not_depend_on_web_persistence_or_infrastructure =
      noClasses()
          .that()
          .resideInAPackage("com.nenkov.bar.application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "com.nenkov.bar.web..",
              "com.nenkov.bar.persistence..",
              "com.nenkov.bar.infrastructure..",
              "com.nenkov.bar.user..",
              "org.springframework.web..",
              "org.springframework.data..",
              "io.r2dbc..",
              "reactor..",
              "jakarta.persistence..",
              "javax.persistence..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule application_repository_package_contains_interfaces_only =
      classes()
          .that()
          .resideInAPackage("com.nenkov.bar.application..repository..")
          .should()
          .beInterfaces()
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule application_gateway_package_contains_interfaces_only =
      classes()
          .that()
          .resideInAPackage("com.nenkov.bar.application..gateway..")
          .should()
          .beInterfaces()
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule application_client_package_contains_interfaces_only =
      classes()
          .that()
          .resideInAPackage("com.nenkov.bar.application..client..")
          .should()
          .beInterfaces()
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule domain_must_not_depend_on_application =
      noClasses()
          .that()
          .resideInAPackage("com.nenkov.bar.domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("com.nenkov.bar.application..");
}
