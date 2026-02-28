package com.kprflow.enterprise.data.repository

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiDocumentationRepository @Inject constructor(
    private val postgrest: Postgrest
) {
    
    companion object {
        // API versions
        const val API_VERSION_V1 = "v1"
        const val API_VERSION_V2 = "v2"
        const val CURRENT_API_VERSION = API_VERSION_V1
        
        // Documentation types
        const val DOC_TYPE_OPENAPI = "OPENAPI"
        const val DOC_TYPE_SWAGGER = "SWAGGER"
        const val DOC_TYPE_POSTMAN = "POSTMAN"
        const val DOC_TYPE_RAML = "RAML"
        
        // Endpoint categories
        const val CATEGORY_AUTH = "AUTHENTICATION"
        const val CATEGORY_DOSSIER = "DOSSIER"
        const val CATEGORY_DOCUMENT = "DOCUMENT"
        const val CATEGORY_PAYMENT = "PAYMENT"
        const val CATEGORY_USER = "USER"
        const val CATEGORY_ANALYTICS = "ANALYTICS"
        const val CATEGORY_ADMIN = "ADMIN"
        const val CATEGORY_WEBHOOK = "WEBHOOK"
        
        // HTTP methods
        val HTTP_METHODS = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")
        
        // Status codes
        const val STATUS_OK = 200
        const val STATUS_CREATED = 201
        const val STATUS_NO_CONTENT = 204
        const val STATUS_BAD_REQUEST = 400
        const val STATUS_UNAUTHORIZED = 401
        const val STATUS_FORBIDDEN = 403
        const val STATUS_NOT_FOUND = 404
        const val STATUS_METHOD_NOT_ALLOWED = 405
        const val STATUS_CONFLICT = 409
        const val STATUS_UNPROCESSABLE_ENTITY = 422
        const val STATUS_INTERNAL_SERVER_ERROR = 500
        const val STATUS_NOT_IMPLEMENTED = 501
        const val STATUS_SERVICE_UNAVAILABLE = 503
    }
    
    suspend fun generateOpenApiSpec(
        version: String = CURRENT_API_VERSION,
        includeDeprecated: Boolean = false,
        includeExamples: Boolean = true
    ): Result<OpenApiSpec> {
        return try {
            val endpoints = getApiEndpoints(version, includeDeprecated).getOrNull().orEmpty()
            val schemas = getApiSchemas(version).getOrNull().orEmpty()
            val examples = if (includeExamples) getApiExamples(version).getOrNull().orEmpty() else emptyList()
            
            val spec = OpenApiSpec(
                openapi = "3.0.3",
                info = OpenApiInfo(
                    title = "KPRFlow Enterprise API",
                    description = "Comprehensive API for KPRFlow Enterprise mortgage management system",
                    version = version,
                    contact = OpenApiContact(
                        name = "API Support",
                        email = "api-support@kprflow.com",
                        url = "https://kprflow.com/support"
                    ),
                    license = OpenApiLicense(
                        name = "MIT",
                        url = "https://opensource.org/licenses/MIT"
                    )
                ),
                servers = listOf(
                    OpenApiServer(
                        url = "https://api.kprflow.com/$version",
                        description = "Production Server"
                    ),
                    OpenApiServer(
                        url = "https://staging-api.kprflow.com/$version",
                        description = "Staging Server"
                    ),
                    OpenApiServer(
                        url = "http://localhost:8080/$version",
                        description = "Development Server"
                    )
                ),
                paths = generatePaths(endpoints, examples),
                components = OpenApiComponents(
                    schemas = schemas.associateBy { it.name },
                    securitySchemes = mapOf(
                        "bearerAuth" to OpenApiSecurityScheme(
                            type = "http",
                            scheme = "bearer",
                            bearerFormat = "JWT"
                        ),
                        "apiKey" to OpenApiSecurityScheme(
                            type = "apiKey",
                            `in` = "header",
                            name = "X-API-Key"
                        )
                    )
                ),
                security = listOf(mapOf("bearerAuth" to emptyList<String>())),
                tags = generateTags(endpoints)
            )
            
            // Save generated spec
            saveApiDocumentation(version, DOC_TYPE_OPENAPI, spec)
                .getOrNull()
            
            Result.success(spec)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generateSwaggerSpec(
        version: String = CURRENT_API_VERSION
    ): Result<SwaggerSpec> {
        return try {
            val endpoints = getApiEndpoints(version).getOrNull().orEmpty()
            val schemas = getApiSchemas(version).getOrNull().orEmpty()
            
            val spec = SwaggerSpec(
                swagger = "2.0",
                info = SwaggerInfo(
                    title = "KPRFlow Enterprise API",
                    description = "Comprehensive API for KPRFlow Enterprise mortgage management system",
                    version = version,
                    contact = SwaggerContact(
                        name = "API Support",
                        email = "api-support@kprflow.com"
                    ),
                    license = SwaggerLicense(
                        name = "MIT",
                        url = "https://opensource.org/licenses/MIT"
                    )
                ),
                host = "api.kprflow.com",
                basePath = "/$version",
                schemes = listOf("https", "http"),
                consumes = listOf("application/json"),
                produces = listOf("application/json"),
                securityDefinitions = mapOf(
                    "BearerAuth" to SwaggerSecurityDefinition(
                        type = "apiKey",
                        name = "Authorization",
                        `in` = "header"
                    )
                ),
                security = listOf(mapOf("BearerAuth" to emptyList<String>())),
                paths = generateSwaggerPaths(endpoints),
                definitions = schemas.associateBy { it.name },
                tags = generateSwaggerTags(endpoints)
            )
            
            // Save generated spec
            saveApiDocumentation(version, DOC_TYPE_SWAGGER, spec)
                .getOrNull()
            
            Result.success(spec)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generatePostmanCollection(
        version: String = CURRENT_API_VERSION,
        includeTests: Boolean = true
    ): Result<PostmanCollection> {
        return try {
            val endpoints = getApiEndpoints(version).getOrNull().orEmpty()
            val examples = getApiExamples(version).getOrNull().orEmpty()
            
            val collection = PostmanCollection(
                info = PostmanInfo(
                    name = "KPRFlow Enterprise API - $version",
                    description = "Complete API collection for KPRFlow Enterprise",
                    schema = "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
                ),
                auth = PostmanAuth(
                    type = "bearer",
                    bearer = listOf(
                        PostmanAuthItem(
                            key = "token",
                            value = "{{jwt_token}}",
                            type = "string"
                        )
                    )
                ),
                variable = listOf(
                    PostmanVariable(
                        key = "base_url",
                        value = "https://api.kprflow.com/$version",
                        type = "string"
                    ),
                    PostmanVariable(
                        key = "jwt_token",
                        value = "",
                        type = "string"
                    )
                ),
                item = generatePostmanFolders(endpoints, examples, includeTests)
            )
            
            // Save generated collection
            saveApiDocumentation(version, DOC_TYPE_POSTMAN, collection)
                .getOrNull()
            
            Result.success(collection)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun registerApiEndpoint(
        path: String,
        method: String,
        summary: String,
        description: String,
        category: String,
        parameters: List<ApiParameter>,
        requestBody: ApiRequestBody? = null,
        responses: Map<String, ApiResponse>,
        tags: List<String> = emptyList(),
        deprecated: Boolean = false,
        version: String = CURRENT_API_VERSION,
        createdBy: String
    ): Result<String> {
        return try {
            val endpointData = mapOf(
                "path" to path,
                "method" to method,
                "summary" to summary,
                "description" to description,
                "category" to category,
                "parameters" to parameters,
                "request_body" to requestBody,
                "responses" to responses,
                "tags" to tags,
                "deprecated" to deprecated,
                "version" to version,
                "is_active" to true,
                "created_by" to createdBy,
                "created_at" to Instant.now().toString(),
                "updated_at" to Instant.now().toString()
            )
            
            val endpoint = postgrest.from("api_endpoints")
                .insert(endpointData)
                .maybeSingle()
                .data
            
            endpoint?.let { 
                    Result.success(it.id)
                }
                ?: Result.failure(Exception("Failed to register API endpoint"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateApiEndpoint(
        endpointId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        return try {
            val updateData = updates + ("updated_at" to Instant.now().toString())
            
            postgrest.from("api_endpoints")
                .update(updateData)
                .filter { eq("id", endpointId) }
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deprecateApiEndpoint(
        endpointId: String,
        deprecationReason: String,
        deprecationDate: Instant = Instant.now(),
        alternativeEndpoint: String? = null
    ): Result<Unit> {
        return try {
            val updateData = mapOf(
                "deprecated" to true,
                "deprecation_reason" to deprecationReason,
                "deprecation_date" to deprecationDate.toString(),
                "alternative_endpoint" to alternativeEndpoint,
                "updated_at" to Instant.now().toString()
            )
            
            postgrest.from("api_endpoints")
                .update(updateData)
                .filter { eq("id", endpointId) }
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getApiEndpoints(
        version: String = CURRENT_API_VERSION,
        includeDeprecated: Boolean = false,
        category: String? = null
    ): Result<List<ApiEndpoint>> {
        return try {
            var query = postgrest.from("api_endpoints")
                .select()
                .filter { eq("version", version) }
                .order("category, path, method")
            
            if (!includeDeprecated) {
                query = query.filter { eq("deprecated", false) }
            }
            
            category?.let { query = query.filter { eq("category", it) } }
            
            val endpoints = query.data
            Result.success(endpoints)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getApiSchemas(
        version: String = CURRENT_API_VERSION
    ): Result<List<ApiSchema>> {
        return try {
            val schemas = postgrest.from("api_schemas")
                .select()
                .filter { eq("version", version) }
                .order("name")
                .data
            
            Result.success(schemas)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getApiExamples(
        version: String = CURRENT_API_VERSION
    ): Result<List<ApiExample>> {
        return try {
            val examples = postgrest.from("api_examples")
                .select()
                .filter { eq("version", version) }
                .order("endpoint_path, method")
                .data
            
            Result.success(examples)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun testApiEndpoint(
        endpointId: String,
        testParameters: Map<String, Any>? = null,
        testBody: Map<String, Any>? = null
    ): Result<ApiTestResult> {
        return try {
            val endpoint = getApiEndpointById(endpointId).getOrNull()
                ?: return Result.failure(Exception("Endpoint not found"))
            
            // Simulate API test
            val testResult = simulateApiTest(endpoint, testParameters, testBody)
            
            // Save test result
            saveApiTestResult(endpointId, testResult)
                .getOrNull()
            
            Result.success(testResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun validateApiDocumentation(
        version: String = CURRENT_API_VERSION,
        docType: String = DOC_TYPE_OPENAPI
    ): Result<ValidationResult> {
        return try {
            val documentation = getApiDocumentation(version, docType).getOrNull()
                ?: return Result.failure(Exception("Documentation not found"))
            
            // Simulate validation
            val issues = mutableListOf<String>()
            
            if (docType == DOC_TYPE_OPENAPI) {
                // Validate OpenAPI spec
                val spec = documentation.content as OpenApiSpec
                
                if (spec.paths.isEmpty()) {
                    issues.add("No paths defined in OpenAPI spec")
                }
                
                if (spec.components.schemas.isEmpty()) {
                    issues.add("No schemas defined in OpenAPI spec")
                }
                
                // Validate path formats
                spec.paths.forEach { (path, pathItem) ->
                    if (!path.startsWith("/")) {
                        issues.add("Invalid path format: $path")
                    }
                }
            }
            
            val validationResult = ValidationResult(
                isValid = issues.isEmpty(),
                issues = issues,
                warnings = emptyList(),
                validatedAt = Instant.now().toString()
            )
            
            Result.success(validationResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getApiDocumentation(
        version: String,
        docType: String
    ): Result<ApiDocumentation> {
        return try {
            val documentation = postgrest.from("api_documentation")
                .select()
                .filter { 
                    eq("version", version)
                    eq("doc_type", docType)
                }
                .order("created_at", ascending = false)
                .limit(1)
                .maybeSingle()
                .data
            
            documentation?.let { Result.success(it) }
                ?: Result.failure(Exception("Documentation not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getApiUsageStatistics(
        startDate: Instant? = null,
        endDate: Instant? = null,
        version: String = CURRENT_API_VERSION
    ): Result<ApiUsageStatistics> {
        return try {
            // Simulate usage statistics
            val statistics = ApiUsageStatistics(
                totalRequests = 10000,
                successfulRequests = 9500,
                failedRequests = 500,
                averageResponseTime = 250.5,
                topEndpoints = listOf(
                    ApiEndpointUsage("/dossiers", "GET", 2000),
                    ApiEndpointUsage("/dossiers", "POST", 1500),
                    ApiEndpointUsage("/documents", "GET", 1200),
                    ApiEndpointUsage("/payments", "GET", 1000),
                    ApiEndpointUsage("/users", "GET", 800)
                ),
                errorBreakdown = mapOf(
                    "400" to 200,
                    "401" to 150,
                    "404" to 100,
                    "500" to 50
                ),
                generatedAt = Instant.now().toString()
            )
            
            Result.success(statistics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeDocumentationUpdates(): Flow<DocumentationUpdate> = flow {
        try {
            // TODO: Implement real-time updates via Supabase Realtime
            emit(DocumentationUpdate.SpecUpdated)
        } catch (e: Exception) {
            emit(DocumentationUpdate.Error(e.message ?: "Unknown error"))
        }
    }
    
    // Private helper methods
    private suspend fun getApiEndpointById(endpointId: String): Result<ApiEndpoint> {
        return try {
            val endpoint = postgrest.from("api_endpoints")
                .select()
                .filter { eq("id", endpointId) }
                .maybeSingle()
                .data
            
            endpoint?.let { Result.success(it) }
                ?: Result.failure(Exception("Endpoint not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun saveApiDocumentation(
        version: String,
        docType: String,
        content: Any
    ): Result<String> {
        return try {
            val docData = mapOf(
                "version" to version,
                "doc_type" to docType,
                "content" to content,
                "generated_at" to Instant.now().toString()
            )
            
            val documentation = postgrest.from("api_documentation")
                .insert(docData)
                .maybeSingle()
                .data
            
            documentation?.let { 
                    Result.success(it.id)
                }
                ?: Result.failure(Exception("Failed to save documentation"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun saveApiTestResult(
        endpointId: String,
        testResult: ApiTestResult
    ): Result<String> {
        return try {
            val testData = mapOf(
                "endpoint_id" to endpointId,
                "status_code" to testResult.statusCode,
                "response_time" to testResult.responseTime,
                "success" to testResult.success,
                "error_message" to testResult.errorMessage,
                "test_data" to testResult.testData,
                "response_data" to testResult.responseData,
                "tested_at" to Instant.now().toString()
            )
            
            val result = postgrest.from("api_test_results")
                .insert(testData)
                .maybeSingle()
                .data
            
            result?.let { 
                    Result.success(it.id)
                }
                ?: Result.failure(Exception("Failed to save test result"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun simulateApiTest(
        endpoint: ApiEndpoint,
        parameters: Map<String, Any>?,
        body: Map<String, Any>?
    ): ApiTestResult {
        // Simulate API test
        val success = (1..100).random() > 10 // 90% success rate
        val statusCode = if (success) {
            when (endpoint.method) {
                "POST" -> 201
                "DELETE" -> 204
                else -> 200
            }
        } else {
            (400..500).random()
        }
        
        return ApiTestResult(
            endpointId = endpoint.id,
            statusCode = statusCode,
            responseTime = (100..1000).random().toLong(),
            success = success,
            errorMessage = if (success) null else "Simulated error",
            testData = mapOf("parameters" to parameters, "body" to body),
            responseData = if (success) mapOf("message" to "Success") else null,
            testedAt = Instant.now().toString()
        )
    }
    
    private fun generatePaths(
        endpoints: List<ApiEndpoint>,
        examples: List<ApiExample>
    ): Map<String, OpenApiPathItem> {
        val paths = mutableMapOf<String, OpenApiPathItem>()
        
        endpoints.forEach { endpoint ->
            val pathItem = paths.getOrPut(endpoint.path) { OpenApiPathItem() }
            
            val operation = OpenApiOperation(
                summary = endpoint.summary,
                description = endpoint.description,
                operationId = "${endpoint.method.lowercase()}${endpoint.path.replace("/", "_")}",
                parameters = endpoint.parameters,
                requestBody = endpoint.requestBody,
                responses = endpoint.responses,
                tags = endpoint.tags,
                deprecated = endpoint.deprecated
            )
            
            // Add examples if available
            val endpointExamples = examples.filter { 
                it.endpointPath == endpoint.path && it.method == endpoint.method 
            }
            if (endpointExamples.isNotEmpty()) {
                operation.examples = endpointExamples.associate { 
                    it.name to OpenApiExample(
                        summary = it.description,
                        value = it.exampleData
                    )
                }
            }
            
            when (endpoint.method.uppercase()) {
                "GET" -> pathItem.get = operation
                "POST" -> pathItem.post = operation
                "PUT" -> pathItem.put = operation
                "DELETE" -> pathItem.delete = operation
                "PATCH" -> pathItem.patch = operation
                "HEAD" -> pathItem.head = operation
                "OPTIONS" -> pathItem.options = operation
            }
        }
        
        return paths
    }
    
    private fun generateSwaggerPaths(endpoints: List<ApiEndpoint>): Map<String, SwaggerPathItem> {
        val paths = mutableMapOf<String, SwaggerPathItem>()
        
        endpoints.forEach { endpoint ->
            val pathItem = paths.getOrPut(endpoint.path) { SwaggerPathItem() }
            
            val operation = SwaggerOperation(
                summary = endpoint.summary,
                description = endpoint.description,
                operationId = "${endpoint.method.lowercase()}${endpoint.path.replace("/", "_")}",
                parameters = endpoint.parameters,
                responses = endpoint.responses,
                tags = endpoint.tags,
                deprecated = endpoint.deprecated
            )
            
            when (endpoint.method.uppercase()) {
                "GET" -> pathItem.get = operation
                "POST" -> pathItem.post = operation
                "PUT" -> pathItem.put = operation
                "DELETE" -> pathItem.delete = operation
                "PATCH" -> pathItem.patch = operation
                "HEAD" -> pathItem.head = operation
                "OPTIONS" -> pathItem.options = operation
            }
        }
        
        return paths
    }
    
    private fun generateTags(endpoints: List<ApiEndpoint>): List<OpenApiTag> {
        return endpoints
            .flatMap { it.tags }
            .distinct()
            .map { tag ->
                OpenApiTag(
                    name = tag,
                    description = "Operations related to $tag"
                )
            }
    }
    
    private fun generateSwaggerTags(endpoints: List<ApiEndpoint>): List<SwaggerTag> {
        return endpoints
            .flatMap { it.tags }
            .distinct()
            .map { tag ->
                SwaggerTag(
                    name = tag,
                    description = "Operations related to $tag"
                )
            }
    }
    
    private fun generatePostmanFolders(
        endpoints: List<ApiEndpoint>,
        examples: List<ApiExample>,
        includeTests: Boolean
    ): List<PostmanItem> {
        val folders = mutableMapOf<String, MutableList<PostmanRequest>>()
        
        endpoints.forEach { endpoint ->
            val category = endpoint.category
            val requests = folders.getOrPut(category) { mutableListOf() }
            
            val request = PostmanRequest(
                name = "${endpoint.method} ${endpoint.path}",
                request = PostmanRequestDetails(
                    method = endpoint.method,
                    header = listOf(
                        PostmanHeader(
                            key = "Content-Type",
                            value = "application/json"
                        ),
                        PostmanHeader(
                            key = "Authorization",
                            value = "Bearer {{jwt_token}}"
                        )
                    ),
                    body = PostmanRequestBody(
                        mode = "raw",
                        raw = endpoint.requestBody?.example ?: "{}"
                    ),
                    url = PostmanUrl(
                        raw = "{{base_url}}${endpoint.path}",
                        host = listOf("{{base_url}}"),
                        path = endpoint.path.split("/").filter { it.isNotEmpty() }
                    )
                ),
                response = emptyList()
            )
            
            // Add examples as sample responses
            val endpointExamples = examples.filter { 
                it.endpointPath == endpoint.path && it.method == endpoint.method 
            }
            if (endpointExamples.isNotEmpty()) {
                request.response = endpointExamples.map { example ->
                    PostmanResponse(
                        name = example.name,
                        originalRequest = PostmanRequestDetails(
                            method = endpoint.method,
                            header = emptyList(),
                            body = null,
                            url = PostmanUrl(
                                raw = "{{base_url}}${endpoint.path}",
                                host = emptyList(),
                                path = emptyList()
                            )
                        ),
                        status = "OK",
                        code = 200,
                        body = example.exampleData.toString()
                    )
                }
            }
            
            requests.add(request)
        }
        
        return folders.map { (category, requests) ->
            PostmanItem(
                name = category,
                item = requests
            )
        }
    }
}

// Data classes for API documentation
data class OpenApiSpec(
    val openapi: String,
    val info: OpenApiInfo,
    val servers: List<OpenApiServer>,
    val paths: Map<String, OpenApiPathItem>,
    val components: OpenApiComponents,
    val security: List<Map<String, List<String>>>,
    val tags: List<OpenApiTag>
)

data class OpenApiInfo(
    val title: String,
    val description: String,
    val version: String,
    val contact: OpenApiContact,
    val license: OpenApiLicense
)

data class OpenApiContact(
    val name: String,
    val email: String,
    val url: String
)

data class OpenApiLicense(
    val name: String,
    val url: String
)

data class OpenApiServer(
    val url: String,
    val description: String
)

data class OpenApiPathItem(
    val get: OpenApiOperation? = null,
    val post: OpenApiOperation? = null,
    val put: OpenApiOperation? = null,
    val delete: OpenApiOperation? = null,
    val patch: OpenApiOperation? = null,
    val head: OpenApiOperation? = null,
    val options: OpenApiOperation? = null
)

data class OpenApiOperation(
    val summary: String,
    val description: String,
    val operationId: String,
    val parameters: List<ApiParameter>,
    val requestBody: ApiRequestBody?,
    val responses: Map<String, ApiResponse>,
    val tags: List<String>,
    val deprecated: Boolean,
    val examples: Map<String, OpenApiExample>? = null
)

data class OpenApiComponents(
    val schemas: Map<String, ApiSchema>,
    val securitySchemes: Map<String, OpenApiSecurityScheme>
)

data class OpenApiSecurityScheme(
    val type: String,
    val scheme: String? = null,
    val bearerFormat: String? = null,
    val location: String? = null,
    val name: String? = null
)

data class OpenApiTag(
    val name: String,
    val description: String
)

data class OpenApiExample(
    val summary: String,
    val value: Any
)

// Swagger data classes
data class SwaggerSpec(
    val swagger: String,
    val info: SwaggerInfo,
    val host: String,
    val basePath: String,
    val schemes: List<String>,
    val consumes: List<String>,
    val produces: List<String>,
    val securityDefinitions: Map<String, SwaggerSecurityDefinition>,
    val security: List<Map<String, List<String>>>,
    val paths: Map<String, SwaggerPathItem>,
    val definitions: Map<String, ApiSchema>,
    val tags: List<SwaggerTag>
)

data class SwaggerInfo(
    val title: String,
    val description: String,
    val version: String,
    val contact: SwaggerContact,
    val license: SwaggerLicense
)

data class SwaggerContact(
    val name: String,
    val email: String
)

data class SwaggerLicense(
    val name: String,
    val url: String
)

data class SwaggerSecurityDefinition(
    val type: String,
    val name: String,
    val location: String
)

data class SwaggerPathItem(
    val get: SwaggerOperation? = null,
    val post: SwaggerOperation? = null,
    val put: SwaggerOperation? = null,
    val delete: SwaggerOperation? = null,
    val patch: SwaggerOperation? = null,
    val head: SwaggerOperation? = null,
    val options: SwaggerOperation? = null
)

data class SwaggerOperation(
    val summary: String,
    val description: String,
    val operationId: String,
    val parameters: List<ApiParameter>,
    val responses: Map<String, ApiResponse>,
    val tags: List<String>,
    val deprecated: Boolean
)

data class SwaggerTag(
    val name: String,
    val description: String
)

// Postman data classes
data class PostmanCollection(
    val info: PostmanInfo,
    val auth: PostmanAuth,
    val variable: List<PostmanVariable>,
    val item: List<PostmanItem>
)

data class PostmanInfo(
    val name: String,
    val description: String,
    val schema: String
)

data class PostmanAuth(
    val type: String,
    val bearer: List<PostmanAuthItem>
)

data class PostmanAuthItem(
    val key: String,
    val value: String,
    val type: String
)

data class PostmanVariable(
    val key: String,
    val value: String,
    val type: String
)

data class PostmanItem(
    val name: String,
    val item: List<PostmanRequest>? = null,
    val request: PostmanRequest? = null
)

data class PostmanRequest(
    val name: String,
    val request: PostmanRequestDetails,
    val response: List<PostmanResponse>
)

data class PostmanRequestDetails(
    val method: String,
    val header: List<PostmanHeader>,
    val body: PostmanRequestBody?,
    val url: PostmanUrl
)

data class PostmanHeader(
    val key: String,
    val value: String
)

data class PostmanRequestBody(
    val mode: String,
    val raw: String
)

data class PostmanUrl(
    val raw: String,
    val host: List<String>,
    val path: List<String>
)

data class PostmanResponse(
    val name: String,
    val originalRequest: PostmanRequestDetails,
    val status: String,
    val code: Int,
    val body: String
)

// API data classes
data class ApiEndpoint(
    val id: String,
    val path: String,
    val method: String,
    val summary: String,
    val description: String,
    val category: String,
    val parameters: List<ApiParameter>,
    val requestBody: ApiRequestBody?,
    val responses: Map<String, ApiResponse>,
    val tags: List<String>,
    val deprecated: Boolean,
    val deprecationReason: String?,
    val deprecationDate: String?,
    val alternativeEndpoint: String?,
    val version: String,
    val isActive: Boolean,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)

data class ApiParameter(
    val name: String,
    val location: String, // "query", "header", "path", "cookie"
    val required: Boolean,
    val type: String,
    val description: String,
    val example: Any?
)

data class ApiRequestBody(
    val description: String,
    val required: Boolean,
    val contentType: String,
    val schema: ApiSchema,
    val example: Any?
)

data class ApiResponse(
    val description: String,
    val contentType: String,
    val schema: ApiSchema?,
    val example: Any?
)

data class ApiSchema(
    val name: String,
    val type: String,
    val properties: Map<String, ApiSchemaProperty>,
    val required: List<String>,
    val description: String,
    val example: Any?
)

data class ApiSchemaProperty(
    val type: String,
    val description: String,
    val required: Boolean,
    val example: Any?
)

data class ApiExample(
    val id: String,
    val endpointPath: String,
    val method: String,
    val name: String,
    val description: String,
    val exampleData: Any,
    val version: String
)

data class ApiTestResult(
    val endpointId: String,
    val statusCode: Int,
    val responseTime: Long,
    val success: Boolean,
    val errorMessage: String?,
    val testData: Map<String, Any>,
    val responseData: Map<String, Any>?,
    val testedAt: String
)

data class ApiDocumentation(
    val id: String,
    val version: String,
    val docType: String,
    val content: Any,
    val generatedAt: String
)

data class ValidationResult(
    val isValid: Boolean,
    val issues: List<String>,
    val warnings: List<String>,
    val validatedAt: String
)

data class ApiUsageStatistics(
    val totalRequests: Long,
    val successfulRequests: Long,
    val failedRequests: Long,
    val averageResponseTime: Double,
    val topEndpoints: List<ApiEndpointUsage>,
    val errorBreakdown: Map<String, Long>,
    val generatedAt: String
)

data class ApiEndpointUsage(
    val path: String,
    val method: String,
    val count: Long
)

sealed class DocumentationUpdate {
    object SpecUpdated : DocumentationUpdate()
    object EndpointAdded : DocumentationUpdate()
    object EndpointDeprecated : DocumentationUpdate()
    data class Error(val message: String) : DocumentationUpdate()
}
