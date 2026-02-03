plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-library`
    `maven-publish`
}

group = "com.consideredweb"
version = "0.2.0"

repositories {
    mavenCentral()
}

dependencies {
    api(libs.bundles.core)
    api(libs.bundles.jetty)

    testImplementation(libs.bundles.testing)
    testImplementation(kotlin("test"))
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    jvmToolchain(23)
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])

            pom {
                name.set("Consideredweb")
                description.set("Lightweight Kotlin HTTP framework")
                url.set("https://github.com/Aldercape/consideredweb")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Aldercape/consideredweb")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String?
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.token") as String?
            }
        }
    }
}
