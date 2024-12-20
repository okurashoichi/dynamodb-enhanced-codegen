/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Gradle plugin project to get you started.
 * For more details on writing Custom Plugins, please refer to https://docs.gradle.org/8.11.1/userguide/custom_plugins.html in the Gradle documentation.
 */

plugins {
    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`

    // Apply the Kotlin JVM plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)
    `maven-publish` // 必須
    id("com.gradle.plugin-publish") version "1.3.0"
}

group = "io.github.okurashoichi"
version = "0.1.0-alpha1"



repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation("software.amazon.awssdk:dynamodb:2.20.0") // DynamoDBクライアント
    implementation("software.amazon.awssdk:dynamodb-enhanced:2.20.0") // Enhanced Client
    implementation("com.squareup:kotlinpoet:1.14.2") // コード生成用
    implementation("org.freemarker:freemarker:2.3.33")
    // Use the Kotlin JUnit 5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    plugins {
        website = "https://github.com/okurashoichi/dynamodb-enhanced-codegen"
        vcsUrl = "https://github.com/okurashoichi/dynamodb-enhanced-codegen"
        // Define the plugin
        create("dynamodbEnhancedClientCodegenPlugin") {
            id = "io.github.okurashoichi.dynamodb.enhanced.codegen"
            displayName = "DynamoDB Enhanced Client Codegen Plugin"
            description = "A Gradle plugin that generates Kotlin code based on DynamoDB table schemas using the DynamoDB Enhanced Client."
            implementationClass = "io.github.okurashoichi.dynamodb.enhanced.codegen.DynamodbEnhancedClientCodegenPlugin"
            version = "0.1.2-alpha"
            tags = listOf("dynamodb", "codegen", "enhanced", "client")
        }
    }

}
tasks {
    processResources {
        from("src/main/resources") // リソースをパッケージに含める
        duplicatesStrategy = DuplicatesStrategy.INCLUDE // 重複を許容する
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.named<Task>("check") {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

tasks.named<Test>("test") {
    // Use JUnit Jupiter for unit tests.
    useJUnitPlatform()
}
