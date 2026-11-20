description = "api-gateway"

dependencies {

    add(
        "implementation",
        platform("org.springframework.cloud:spring-cloud-dependencies:2025.0.3")
    )

    add(
        "implementation",
        "org.springframework.cloud:spring-cloud-starter-gateway-server-webflux"
    )
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    add(
        "implementation",
        "org.springdoc:springdoc-openapi-starter-webflux-ui:2.10.0"
    )
}

tasks.named<org.gradle.language.jvm.tasks.ProcessResources>("processResources") {
    from(rootProject.file("docs/openapi.yaml")) {
        into("static")
    }
}