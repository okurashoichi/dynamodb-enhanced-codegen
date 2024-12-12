package org.example

open class DynamoDbCodeGenExtension {
    var region: String = "us-east-1"
    var tableNames: List<String> = listOf()
    var outputDir: String = "build/generated-sources"
}