plugins {
	java
	id("org.springframework.boot") version "3.5.3"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.srihari.ai"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

extra["springAiVersion"] = "1.0.0"
extra["springCloudVersion"] = "2025.0.0"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.ai:spring-ai-starter-model-chat-memory")
	implementation("org.springframework.ai:spring-ai-starter-model-openai")
	implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
	implementation("io.github.resilience4j:resilience4j-bulkhead")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("com.github.ben-manes.caffeine:caffeine")
	// Tracing dependencies - only include if tracing is needed
	// implementation("io.micrometer:micrometer-tracing-bridge-brave")
	// implementation("io.zipkin.reporter2:zipkin-reporter-brave")
	implementation("io.micrometer:micrometer-registry-prometheus")
	implementation("net.logstash.logback:logstash-logback-encoder:8.0")
	implementation("io.jsonwebtoken:jjwt-api:0.12.3")
	implementation("io.jsonwebtoken:jjwt-impl:0.12.3")
	implementation("io.jsonwebtoken:jjwt-jackson:0.12.3")
	implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.3.0")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
	
	// Enable parallel test execution
	systemProperty("junit.jupiter.execution.parallel.enabled", "true")
	systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
	systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
	
	// Configure parallel execution
	maxParallelForks = Runtime.getRuntime().availableProcessors()
	
	// JVM options for better parallel performance
	jvmArgs("-XX:+UseParallelGC", "-XX:ParallelGCThreads=4", "-Xmx1g")
	
	// Test execution settings
	testLogging {
		events("passed", "skipped", "failed")
		showStandardStreams = false
		showExceptions = true
		showCauses = true
		showStackTraces = true
	}
}

// JVM options for better build performance
tasks.withType<JavaCompile> {
	options.isFork = true
	options.forkOptions.jvmArgs?.addAll(listOf(
		"-Xmx512m",  // Optimized memory for development builds
		"-XX:+UseParallelGC",
		"-XX:MaxMetaspaceSize=256m",  // Limit metaspace to prevent memory leaks
		"-XX:+UseStringDeduplication"  // Reduce memory usage
	))
}

// Simple development task
tasks.register("dev") {
	group = "development"
	description = "Build and run the application"
	dependsOn("build", "bootRun")
}
