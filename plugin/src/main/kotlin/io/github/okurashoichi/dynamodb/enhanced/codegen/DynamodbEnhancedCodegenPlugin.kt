package io.github.okurashoichi.dynamodb.enhanced.codegen

import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import java.io.File
import java.io.StringWriter
import java.net.URI
import java.util.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest
import software.amazon.awssdk.services.dynamodb.model.ScanRequest

class DynamodbEnhancedClientCodegenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("dynamoDbEnhancedClientCodeGen", DynamoDbCodeGenExtension::class.java)
        project.tasks.register("generateDynamoDbCode", GenerateDynamoDbCodeTask::class.java) {
            it.group = "DynamoDB Enhanced Client Code Generation"
            it.description = "Generates DynamoDB Enhanced Client classes"
            it.extension = extension
        }
    }
}

abstract class GenerateDynamoDbCodeTask : org.gradle.api.DefaultTask() {
    @Input
    lateinit var extension: DynamoDbCodeGenExtension

    @org.gradle.api.tasks.TaskAction
    fun generate() {
        val classLoader = Thread.currentThread().contextClassLoader
        val templateStream = classLoader.getResourceAsStream("templates/template.ftl")
        if (templateStream == null) {
            throw IllegalStateException("Template file not found in resources")
        }
        val dynamoDbClient = software.amazon.awssdk.services.dynamodb.DynamoDbClient.builder()
            .endpointOverride(URI.create(extension.endpoint))
            .region(software.amazon.awssdk.regions.Region.of(extension.region))
            .build()
        val cfg = Configuration(Configuration.VERSION_2_3_31).apply {
            setClassLoaderForTemplateLoading(classLoader, "/templates")
            templateUpdateDelayMilliseconds = 0 // キャッシュを無効化
            templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
            logTemplateExceptions = false
            wrapUncheckedExceptions = true
        }

        extension.tableNames.forEach { tableName ->
            val scanRequest = ScanRequest.builder()
                .tableName(tableName)
                .build()
            val scanResponse = dynamoDbClient.scan(scanRequest)
            val allAttributes = scanResponse.items().flatMap { it.entries }.distinctBy { it.key }

            val describeTableRequest = DescribeTableRequest.builder()
                .tableName(tableName)
                .build()
            val tableDescription = dynamoDbClient.describeTable(describeTableRequest).table()


            val outputDir = File(extension.outputDir)
            outputDir.mkdirs()

            val template = cfg.getTemplate("template.ftl")
            val dataModel = mapOf(
                "className" to tableName.toUpperCamelCase(),
                "attributes" to allAttributes.map { (attr, attrValue) ->
                    mapOf(
                        "attributeName" to attr,
                        "name" to attr.toLowerCamelCase(),
                        "type" to determineAttributeType(attrValue),
                        "defaultValue" to determineDefaultValue(determineAttributeType(attrValue)),
                        "partitionKey" to (attr == tableDescription.keySchema()
                            .firstOrNull { it.keyTypeAsString() == "HASH" }?.attributeName()),
                        "sortKey" to (attr == tableDescription.keySchema()
                            .firstOrNull { it.keyTypeAsString() == "RANGE" }?.attributeName()),
                        "secondaryPartitionKey" to (tableDescription.globalSecondaryIndexes().any { gsi ->
                            gsi.keySchema().any { it.attributeName() == attr && it.keyTypeAsString() == "HASH" }
                        } || tableDescription.localSecondaryIndexes().any { lsi ->
                            lsi.keySchema().any { it.attributeName() == attr && it.keyTypeAsString() == "HASH" }
                        }),
                        "secondarySortKey" to (tableDescription.globalSecondaryIndexes().any { gsi ->
                            gsi.keySchema().any { it.attributeName() == attr && it.keyTypeAsString() == "RANGE" }
                        } || tableDescription.localSecondaryIndexes().any { lsi ->
                            lsi.keySchema().any { it.attributeName() == attr && it.keyTypeAsString() == "RANGE" }
                        }),
                        "secondaryPartitionKeyIndexName" to (tableDescription.globalSecondaryIndexes().firstOrNull { gsi ->
                            gsi.keySchema().any { it.attributeName() == attr && it.keyTypeAsString() == "HASH" }
                        }?.indexName() ?: tableDescription.localSecondaryIndexes().firstOrNull { lsi ->
                            lsi.keySchema().any { it.attributeName() == attr && it.keyTypeAsString() == "HASH" }
                        }?.indexName()),
                        "secondarySortKeyIndexName" to (tableDescription.globalSecondaryIndexes().firstOrNull { gsi ->
                            gsi.keySchema().any { it.attributeName() == attr && it.keyTypeAsString() == "RANGE" }
                        }?.indexName() ?: tableDescription.localSecondaryIndexes().firstOrNull { lsi ->
                            lsi.keySchema().any { it.attributeName() == attr && it.keyTypeAsString() == "RANGE" }
                        }?.indexName()),
                        "globalSecondaryIndexes" to tableDescription.globalSecondaryIndexes().filter { gsi ->
                            gsi.keySchema().any { it.attributeName() == attr }
                        }.map { gsi ->
                            mapOf(
                                "indexName" to gsi.indexName(),
                                "keyType" to gsi.keySchema().first { it.attributeName() == attr }.keyTypeAsString()
                            )
                        },
                        "localSecondaryIndexes" to tableDescription.localSecondaryIndexes().filter { lsi ->
                            lsi.keySchema().any { it.attributeName() == attr }
                        }.map { lsi ->
                            mapOf(
                                "indexName" to lsi.indexName(),
                                "keyType" to lsi.keySchema().first { it.attributeName() == attr }.keyTypeAsString()
                            )
                        }
                    )
                }
            )

            val writer = StringWriter()
            template.process(dataModel, writer)

            val file = File(outputDir, "${tableName.toUpperCamelCase()}.kt")
            file.writeText(writer.toString())
        }
    }

    private fun mapAttributeTypeToKotlinType(attributeType: String): String {
        return when (attributeType) {
            "S" -> "String"
            "N" -> "Int"
            "B" -> "ByteArray"
            "BOOL" -> "Boolean"
            "M" -> "Map<String, Any>"
            "L" -> "List<Any>"
            "SS" -> "Set<String>"
            "NS" -> "Set<Int>"
            "BS" -> "Set<ByteArray>"
            else -> "Any"
        }
    }

    fun determineAttributeType(attributeValue: AttributeValue): String {
        return when {
            attributeValue.s() != null -> "String"
            attributeValue.n() != null -> "Int"
            attributeValue.b() != null -> "ByteArray"
            attributeValue.bool() != null -> "Boolean"
            attributeValue.m() != null -> "Map<String, Any>"
            attributeValue.l() != null -> "List<Any>"
            attributeValue.ss() != null -> "Set<String>"
            attributeValue.ns() != null -> "Set<Int>"
            attributeValue.bs() != null -> "Set<ByteArray>"
            else -> "Any"
        }
    }

    fun determineDefaultValue(type: String): String {
        return when (type) {
            "String" -> "\"\""
            "Int" -> "0"
            "ByteArray" -> "byteArrayOf()"
            "Boolean" -> "false"
            "Map<String, Any>" -> "emptyMap()"
            "List<Any>" -> "emptyList()"
            "Set<String>" -> "emptySet()"
            "Set<Int>" -> "emptySet()"
            "Set<ByteArray>" -> "emptySet()"
            else -> "Any()"
        }
    }
}

fun String.toUpperCamelCase(): String {
    return split(
        "-",
        "_"
    ).joinToString("") { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
}

fun String.toLowerCamelCase(): String {
    return split("-", "_").mapIndexed { index, s ->
        if (index == 0) s.lowercase(Locale.getDefault())
        else s.replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }.joinToString("")
}