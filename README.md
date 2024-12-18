# DynamoDB Enhanced Codegen Plugin

This project is a Gradle plugin that generates Kotlin code based on DynamoDB table schemas using the DynamoDB Enhanced Client.

## Features

- Scans DynamoDB table schemas and generates Kotlin classes automatically
- Supports local DynamoDB endpoints
- Uses Freemarker as the template engine

## Installation

Add the following to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.okurashoichi.dynamodb.enhanced.codegen") version "0.1.1-alpha"
}
```

## Usage
Start DynamoDB Local.
Add the plugin configuration to your build.gradle.kts

```kotlin
dynamodbEnhancedCodegen {
    region = "us-west-2"
    tableNames = listOf("Your", "Table", "Names")
    outputDir = "$buildDir/generated-src"
    endpoint = "http://localhost:8000" // Set the endpoint
}
```

Run the Gradle task to generate the code

```shell
./gradlew generateDynamoDbCode
```
## Configuration Options
- region: The region of DynamoDB
- tableNames: List of table names to scan
- outputDir: Output directory for the generated code
- endpoint: The endpoint of DynamoDB
