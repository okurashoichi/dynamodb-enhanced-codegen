/*
 * This source file was generated by the Gradle 'init' task
 */
package io.github.okurashoichi.dynamodb.enhanced.codegen

import java.io.File
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * A simple unit test for the 'org.example.greeting' plugin.
 */
class DynamodbEnhancedCodegenPluginTest {
    @Test fun `plugin registers task`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.okurashoichi.dynamodb.enhanced.codegen")

        // Verify the result
        assertNotNull(project.tasks.findByName("generateDynamoDbCode"))
    }

    @Test fun `plugin execute`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.okurashoichi.dynamodb.enhanced.codegen")

        // Configure the extension
        val extension = project.extensions.getByType(io.github.okurashoichi.dynamodb.enhanced.codegen.DynamoDbCodeGenExtension::class.java)
        extension.region = "ap-northeast-1"
        extension.tableNames = listOf("token")
        extension.outputDir = "build/generated-sources"

        // Execute the task
        val task = project.tasks.findByName("generateDynamoDbCode")
        task?.actions?.forEach { it.execute(task) }

        // Verify the result
        val generatedFile = File(extension.outputDir, "Token.kt")
        assert(generatedFile.exists()) { "Generated file not found: ${generatedFile.path}" }
    }
}
