import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    java
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.execodex"
version = "1.0.2"
description = "app"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-liquibase:4.0.1")
    runtimeOnly("org.postgresql:postgresql") // we do need this for liquibase
    implementation("org.postgresql:r2dbc-postgresql")

    // AWS SDK v2 S3 Async
    implementation(platform("software.amazon.awssdk:bom:2.29.50"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:netty-nio-client")

    //documentation
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.1")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<BootBuildImage>("bootBuildImage") {
    val tag = project.findProperty("imageTag")?.toString() ?: "latest"

    imageName.set("gluonstream/be-minio:$tag")
    tags.set(listOf("gluonstream/be-minio:latest"))
}
