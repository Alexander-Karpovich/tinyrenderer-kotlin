import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    application
    kotlin("jvm") version "1.4.10"
    kotlin("kapt") version "1.4.10"

    id("org.openjfx.javafxplugin") version "0.0.8"
}

javafx {
    modules = listOf("javafx.controls", "javafx.fxml")
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8


configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.1")
    implementation("net.imagej:ij:1.52q")
    implementation("no.tornado:tornadofx:1.7.19") {
        exclude("org.jetbrains.kotlin")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}
tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.akar.tinyrenderer.MainKt"
    }
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

application {
    mainClassName = "com.akar.tinyrenderer.MainKt"
}

tasks {
    "build" {
        dependsOn
    }
}

