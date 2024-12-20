package io.github.okurashoichi.dynamodb.enhanced.codegen

open class DynamoDbCodeGenExtension {
    var region: String = "us-east-1"
    var tableNames: List<String> = listOf()
    var outputDir: String = "build/generated-sources"
    var endpoint: String = "http://localhost:8000" // デフォルト値を設定
    var packageName: String = "dynamodb.enhanced.codegen"
}