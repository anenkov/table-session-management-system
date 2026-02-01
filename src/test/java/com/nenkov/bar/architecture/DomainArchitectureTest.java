package com.nenkov.bar.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.nenkov.bar")
class DomainArchitectureTest {

  @ArchTest
  static final ArchRule domain_should_not_depend_on_frameworks =
      noClasses()
          .that()
          .resideInAPackage("com.nenkov.bar.domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("org.springframework..", "reactor..", "io.r2dbc..");
}
