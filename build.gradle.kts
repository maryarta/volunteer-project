import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("org.springframework.boot") version "3.5.0" apply false
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")

    group = "ru.maryarta"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    // JDK для компиляции и запуска тестов.
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    dependencies {
        // BOM согласует версии зависимостей со Spring Boot.
        add(
            "implementation",
            platform(SpringBootPlugin.BOM_COORDINATES)
        )
        add(
            "testImplementation",
            "org.springframework.boot:spring-boot-starter-test"
        )
        add(
            "testRuntimeOnly",
            "org.junit.platform:junit-platform-launcher"
        )
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()

        testLogging {
            events("failed", "skipped")
            exceptionFormat =
                org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}