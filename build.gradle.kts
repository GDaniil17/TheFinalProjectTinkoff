import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
    application
}

group = "me.daniil"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    "https://jitpack.io"
}

dependencies {
    implementation("org.telegram:telegrambots:5.2.0")
    implementation("org.telegram:telegrambots-abilities:5.2.0")
    compile("org.slf4j:slf4j-simple:1.6.1")
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "MainKt"
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}