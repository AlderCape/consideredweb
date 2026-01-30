package com.consideredweb.openapi

import com.consideredweb.core.JsonSupport
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.functions

object SchemaGenerator {

    /**
     * Generate schema from a KotlinX Serialization serializer
     */
    fun generateSchemaFromSerializer(serializer: kotlinx.serialization.KSerializer<*>): Schema {
        val descriptor = serializer.descriptor

        // Basic implementation - just create object schema with properties
        val properties = mutableMapOf<String, Schema>()
        val required = mutableListOf<String>()

        // For now, extract field names from descriptor and create string properties
        for (i in 0 until descriptor.elementsCount) {
            val elementName = descriptor.getElementName(i)
            val isOptional = descriptor.isElementOptional(i)

            properties[elementName] = Schema(type = "string") // Simplified for now

            if (!isOptional) {
                required.add(elementName)
            }
        }

        // Try to get example data using reflection (this is more complex without reified types)
        val example = getExampleDataFromClassName(descriptor.serialName)

        return Schema(
            type = "object",
            properties = properties,
            required = if (required.isNotEmpty()) required else null,
            example = example
        )
    }

    /**
     * Generate schema for a type with example data if available
     */
    inline fun <reified T> generateSchemaForType(): Schema {
        val serializer = serializer<T>()
        val descriptor = serializer.descriptor

        // Basic implementation - just create object schema with properties
        val properties = mutableMapOf<String, Schema>()
        val required = mutableListOf<String>()

        // For now, extract field names from descriptor and create string properties
        for (i in 0 until descriptor.elementsCount) {
            val elementName = descriptor.getElementName(i)
            val isOptional = descriptor.isElementOptional(i)

            properties[elementName] = Schema(type = "string") // Simplified for now

            if (!isOptional) {
                required.add(elementName)
            }
        }

        // Try to get example data from openApiExample() method
        val example = getExampleData<T>()

        return Schema(
            type = "object",
            properties = properties,
            required = if (required.isNotEmpty()) required else null,
            example = example
        )
    }

    /**
     * Extract example data from openApiExample() companion object method
     */
    inline fun <reified T> getExampleData(): String? {
        return try {
            val kClass = T::class
            val companionObject = kClass.companionObject

            if (companionObject != null) {
                val companionInstance = companionObject.objectInstance
                val openApiExampleMethod = companionObject.functions.find { it.name == "openApiExample" }

                if (openApiExampleMethod != null && companionInstance != null) {
                    val exampleInstance = openApiExampleMethod.call(companionInstance) as T
                    JsonSupport.toJson(exampleInstance)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If example generation fails, silently continue without example
            null
        }
    }

    /**
     * Get example data from class name (fallback when we don't have reified types)
     */
    private fun getExampleDataFromClassName(className: String): String? {
        // For now, this is a simplified approach - we can't easily get examples
        // without reified types. This could be enhanced with a registry approach.
        return null
    }

    /**
     * Get simple class name for schema reference
     */
    fun getSchemaName(klass: KClass<*>): String {
        return klass.simpleName ?: "Unknown"
    }
}