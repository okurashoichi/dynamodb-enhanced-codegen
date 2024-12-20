<#-- template.ftl -->
package ${packageName}

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey

@DynamoDbBean
data class ${className}(
<#list attributes as attribute>
    <#if attribute.partitionKey>
    @get:DynamoDbPartitionKey
    </#if>
    <#if attribute.sortKey>
    @get:DynamoDbSortKey
    </#if>
    <#if attribute.secondaryPartitionKey>
    @get:DynamoDbSecondaryPartitionKey(indexNames = ["${attribute.secondaryPartitionKeyIndexName}"])
    </#if>
    <#if attribute.secondarySortKey>
    @get:DynamoDbSecondarySortKey(indexNames = ["${attribute.secondarySortKeyIndexName}"])
    </#if>
    @get:DynamoDbAttribute("${attribute.attributeName}")
    var ${attribute.name}: ${attribute.type}<#if !attribute.partitionKey && !attribute.sortKey>? = null<#else> = ${attribute.defaultValue}</#if><#if attribute_has_next>,</#if>

</#list>
)