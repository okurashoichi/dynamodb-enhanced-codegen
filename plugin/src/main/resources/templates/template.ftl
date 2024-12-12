<#-- template.ftl -->
package com.beppukeirin.common.aws.dynamodb.table

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import java.time.LocalDateTime

@DynamoDbBean
data class ${className}(
<#list attributes as attribute>
    <#if attribute.partitionKey?? && attribute.partitionKey>
    @get:DynamoDbPartitionKey
    </#if>
    <#if attribute.sortKey?? && attribute.sortKey>
    @get:DynamoDbSortKey
    </#if>
    @get:DynamoDbAttribute("${attribute.name}")
    var ${attribute.name}: ${attribute.type}? = null

</#list>
)