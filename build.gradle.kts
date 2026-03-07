val mapstructVersion: String by project
val archunitJunit5Version: String by project
val springdocOpenapi: String by project

plugins {
  java
  id("org.springframework.boot") version "3.5.6"
  id("io.spring.dependency-management") version "1.1.7"
  id("com.diffplug.spotless") version "7.0.4"
}

group = "ee.finalthesis"

version = "0.0.1-SNAPSHOT"

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

configurations {
  compileOnly { extendsFrom(configurations.annotationProcessor.get()) }
}

spotless {
  java {
    removeUnusedImports()
    googleJavaFormat("1.27.0").reflowLongStrings()
    formatAnnotations()
    targetExclude("build/generated/**")
  }
}

sourceSets {
  main {
    java {
      srcDir("build/generated/sources/annotationProcessor/java/main")
    }
  }
}

tasks.test {
  useJUnitPlatform()
}

repositories { mavenCentral() }

dependencies {
  annotationProcessor("org.hibernate.orm:hibernate-jpamodelgen")
  annotationProcessor("org.projectlombok:lombok")
  annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  compileOnly("org.projectlombok:lombok")

  developmentOnly("org.springframework.boot:spring-boot-devtools")
  developmentOnly("org.springframework.boot:spring-boot-docker-compose")

  implementation("com.fasterxml.jackson.datatype:jackson-datatype-hibernate6")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  implementation("org.liquibase:liquibase-core")
  implementation("org.mapstruct:mapstruct:$mapstructVersion")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:$springdocOpenapi")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocOpenapi")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("io.jsonwebtoken:jjwt-api:0.12.6")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.postgresql:postgresql")

  testImplementation("com.tngtech.archunit:archunit-junit5-api:$archunitJunit5Version") {
    exclude(group = "org.slf4j", module = "slf4j-api")
  }
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.boot:spring-boot-testcontainers")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:junit-jupiter")
  testImplementation("org.testcontainers:postgresql")
  testImplementation("org.testcontainers:testcontainers")

  testRuntimeOnly("com.tngtech.archunit:archunit-junit5-engine:$archunitJunit5Version") {
    exclude(group = "org.slf4j", module = "slf4j-api")
  }
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
