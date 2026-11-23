description = "api-gateway"

dependencies {

    implementation(
        platform("org.springframework.cloud:spring-cloud-dependencies:2025.0.3")
    )
    implementation(
        "org.springframework.cloud:spring-cloud-starter-gateway-server-webflux"
    )
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.10.0")

//    testImplementation("org.wiremock:wiremock-standalone:3.12.2")
    testImplementation("org.wiremock:wiremock-jetty12:3.13.2")
//    testImplementation("org.wiremock:wiremock:3.13.2")
//    testImplementation("org.testcontainers:testcontainers")
}

tasks.named<org.gradle.language.jvm.tasks.ProcessResources>("processResources") {
    from(rootProject.file("docs/openapi.yaml")) {
        into("static")
    }
}