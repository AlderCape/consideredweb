package com.consideredweb

import com.consideredweb.core.HttpServer
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

/**
 * ArchUnit tests to enforce architectural boundaries in the web framework.
 *
 * These tests ensure clean separation between:
 * - Core framework (domain model, interfaces)
 * - Server implementations (Jetty, Java HTTP)
 */
class ArchitectureTest {

    private val classes = ClassFileImporter()
        .withImportOption { location -> !location.contains("/test/") } // Exclude test classes
        .importPackages("com.consideredweb")

    @ArchTest
    fun `core framework should not depend on server implementations`() {
        val coreClasses = listOf(
            "Request", "HttpResponse", "HttpMethod", "Route", "RouteMatcher",
            "FrameworkHandler", "HttpServer", "HttpHandler"
        )

        coreClasses.forEach { coreClass ->
            noClasses()
                .that().haveSimpleNameStartingWith(coreClass) // More specific: class name must start with the core class name
                .and().areNotMemberClasses() // Exclude inner classes
                .should().dependOnClassesThat().haveSimpleNameContaining("JettyHttpServer")
                .orShould().dependOnClassesThat().haveSimpleNameContaining("JavaHttpServer")
                .because("Core framework classes should not depend on specific server implementations")
                .check(classes)
        }
    }

    @ArchTest
    fun `core framework should not use server-specific dependencies`() {
        noClasses()
            .that().resideInAPackage("com.consideredweb.core..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.eclipse.jetty..",
                "jakarta.servlet..",
                "com.sun.net.httpserver.."
            )
            .because("Core framework should not use server-specific dependencies")
            .check(classes)
    }

    @ArchTest
    fun `Jetty server should only use Jetty and Jakarta servlet dependencies`() {
        classes()
            .that().haveSimpleNameContaining("JettyHttpServer")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                "com.consideredweb..", // Allow all consideredweb packages
                "org.eclipse.jetty..",
                "jakarta.servlet..",
                "org.jetbrains.annotations..", // Allow Kotlin annotations
                "java..",
                "kotlin..",
                "kotlinx.."
            )
            .because("Jetty server should only use Jetty-specific dependencies and framework core")
            .check(classes)
    }

    @ArchTest
    fun `Java HTTP server should only use Java built-in dependencies`() {
        classes()
            .that().haveSimpleNameContaining("JavaHttpServer")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                "com.consideredweb..", // Allow all consideredweb packages
                "com.sun.net.httpserver..",
                "org.jetbrains.annotations..", // Allow Kotlin annotations
                "java..",
                "kotlin..",
                "kotlinx.."
            )
            .because("Java HTTP server should only use Java built-in dependencies and framework core")
            .check(classes)
    }

    @ArchTest
    fun `server implementations should not depend on each other`() {
        noClasses()
            .that().haveSimpleNameContaining("JettyHttpServer")
            .should().dependOnClassesThat().haveSimpleNameContaining("JavaHttpServer")
            .because("Server implementations should not depend on each other")
            .check(classes)

        noClasses()
            .that().haveSimpleNameContaining("JavaHttpServer")
            .should().dependOnClassesThat().haveSimpleNameContaining("JettyHttpServer")
            .because("Server implementations should not depend on each other")
            .check(classes)
    }

    @ArchTest
    fun `server implementations should implement HttpServer interface`() {
        classes()
            .that().haveSimpleNameEndingWith("HttpServer")
            .and().areNotInterfaces() // Exclude the interface itself
            .should().implement(HttpServer::class.java)
            .because("All server implementations should implement the HttpServer interface")
            .check(classes)
    }

    @ArchTest
    fun `core interfaces should be stable and not depend on implementations`() {
        classes()
            .that().areInterfaces()
            .and().resideInAPackage("com.consideredweb.core..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                "com.consideredweb.core..",
                "org.jetbrains.annotations..", // Allow Kotlin annotations
                "java..",
                "kotlin..",
                "kotlinx.."
            )
            .because("Core interfaces should only depend on other core classes and standard libraries")
            .check(classes)
    }

    @ArchTest
    fun `examples should not be used by framework code`() {
        noClasses()
            .that().resideInAPackage("com.consideredweb.core..")
            .should().dependOnClassesThat().haveSimpleNameContaining("Example")
            .because("Framework code should not depend on example classes")
            .check(classes)
    }
}