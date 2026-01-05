plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("kapt") version "1.9.25"
}

group = "kz.aqyldy-kundelik"
version = "0.0.1-SNAPSHOT"
description = "aqyldy-kundelik-backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // --- Core Web ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // --- Persistence / DB ---
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    // testRuntimeOnly("com.h2database:h2") // удобно для unit-тестов без PG

    // --- Security / JWT ---
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    // Если будешь сам выпускать токены — добавь Nimbus:
    implementation("com.nimbusds:nimbus-jose-jwt:9.37") // версия примерная; можно обновить

    // --- Observability / Ops ---
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Кэширование
    implementation("org.springframework.boot:spring-boot-starter-cache")
    // Redis как кэш/сессии/ratelimit
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Очереди/уведомления
    implementation("org.springframework.boot:spring-boot-starter-amqp") // RabbitMQ

    // Работа с S3 (MinIO) - AWS SDK v2
    implementation(platform("software.amazon.awssdk:bom:2.20.0"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:s3-transfer-manager")

    // MinIO SDK (оставляем для совместимости, если понадобится)
    implementation("io.minio:minio:8.5.11")

    // TwelveMonkeys ImageIO для поддержки дополнительных форматов (WebP, TIFF и т.д.)
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-core:3.11.0")

    // OpenAPI/Swagger UI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // Маппинг DTO↔Entity (если нужен MapStruct)
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    kapt("org.mapstruct:mapstruct-processor:1.5.5.Final")

    // --- Testing ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")
    testImplementation("org.testcontainers:postgresql:1.19.7")
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
