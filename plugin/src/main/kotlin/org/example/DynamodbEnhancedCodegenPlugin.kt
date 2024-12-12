package org.example

import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import java.io.File
import java.io.StringWriter
import java.net.URI
import java.util.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest

class DynamodbEnhancedCodegenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("dynamoDbCodeGen", DynamoDbCodeGenExtension::class.java)
        project.tasks.register("generateDynamoDbCode", GenerateDynamoDbCodeTask::class.java) {
            it.group = "Code Generation"
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
            .endpointOverride(URI.create("http://localhost:8000")) // ローカルのDynamoDBエンドポイント
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
            val describeTableRequest = DescribeTableRequest.builder()
                .tableName(tableName)
                .build()

            val tableDescription = dynamoDbClient.describeTable(describeTableRequest).table()

            val outputDir = File(extension.outputDir)
            outputDir.mkdirs()

            val template = cfg.getTemplate("template.ftl")
            val dataModel = mapOf(
                "className" to tableName.toUpperCamelCase(),
                "attributes" to tableDescription.attributeDefinitions().map { attr ->
                    mapOf(
                        "name" to attr.attributeName().replaceFirstChar { it.lowercase(Locale.getDefault()) },
                        "type" to mapAttributeTypeToKotlinType(attr.attributeType().toString()),
                        "partitionKey" to (attr.attributeName() == tableDescription.keySchema()
                            .firstOrNull { it.keyTypeAsString() == "HASH" }?.attributeName()),
                        "sortKey" to (attr.attributeName() == tableDescription.keySchema()
                            .firstOrNull { it.keyTypeAsString() == "RANGE" }?.attributeName())
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
            else -> "Any"
        }
    }
}

fun String.toUpperCamelCase(): String {
    return split(
        "-",
        "_"
    ).joinToString("") { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
}