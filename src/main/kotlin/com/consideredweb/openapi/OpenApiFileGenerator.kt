package com.consideredweb.openapi

import com.consideredweb.core.Route
import java.io.File
import java.nio.file.Paths

/**
 * Configuration for OpenAPI file generation
 */
data class OpenApiConfig(
    val enabled: Boolean = true,
    val outputPath: String = "openapi.json",
    val title: String = "API",
    val version: String = "1.0.0",
    val baseDirectory: String? = null // If null, uses current working directory
)

/**
 * Automatic OpenAPI spec file generator
 */
object OpenApiFileGenerator {

    /**
     * Generate OpenAPI spec file with configuration
     */
    fun generateFile(routes: List<Route>, config: OpenApiConfig) {
        if (!config.enabled) {
            println("OpenAPI generation disabled")
            return
        }

        try {
            val openApiJson = OpenApiGenerator.generate(
                routes = routes,
                title = config.title,
                version = config.version
            )

            val outputPath = resolveOutputPath(config)
            val file = File(outputPath)

            // Ensure parent directory exists
            file.parentFile?.mkdirs()

            // Write the OpenAPI spec
            file.writeText(openApiJson)

            println("OpenAPI spec generated: ${file.absolutePath}")
        } catch (e: Exception) {
            println("Failed to generate OpenAPI spec: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Generate OpenAPI spec file with default configuration from environment
     */
    fun generateFile(routes: List<Route>, title: String = "API", version: String = "1.0.0") {
        val config = createConfigFromEnvironment(title, version)
        generateFile(routes, config)
    }

    /**
     * Create configuration from environment variables
     */
    private fun createConfigFromEnvironment(title: String, version: String): OpenApiConfig {
        val enabled = System.getenv("OPENAPI_GENERATION_ENABLED")?.toBoolean()
            ?: System.getProperty("openapi.generation.enabled")?.toBoolean()
            ?: isDevEnvironment()

        val outputPath = System.getenv("OPENAPI_OUTPUT_PATH")
            ?: System.getProperty("openapi.output.path")
            ?: "openapi.json"

        val baseDir = System.getenv("OPENAPI_BASE_DIR")
            ?: System.getProperty("openapi.base.dir")

        return OpenApiConfig(
            enabled = enabled,
            outputPath = outputPath,
            title = title,
            version = version,
            baseDirectory = baseDir
        )
    }

    /**
     * Detect if we're in a development environment
     */
    private fun isDevEnvironment(): Boolean {
        val env = System.getenv("ENVIRONMENT") ?: System.getProperty("environment") ?: ""
        val profile = System.getenv("SPRING_PROFILES_ACTIVE") ?: System.getProperty("spring.profiles.active") ?: ""

        return when {
            env.contains("dev", ignoreCase = true) -> true
            env.contains("local", ignoreCase = true) -> true
            profile.contains("dev", ignoreCase = true) -> true
            profile.contains("local", ignoreCase = true) -> true
            env.contains("prod", ignoreCase = true) -> false
            env.contains("production", ignoreCase = true) -> false
            else -> true // Default to enabled for development
        }
    }

    /**
     * Resolve the full output path
     */
    private fun resolveOutputPath(config: OpenApiConfig): String {
        return if (config.baseDirectory != null) {
            Paths.get(config.baseDirectory, config.outputPath).toString()
        } else {
            config.outputPath
        }
    }
}