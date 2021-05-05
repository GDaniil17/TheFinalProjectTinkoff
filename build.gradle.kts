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

println("This is executed during the configuration phase.")

tasks.register("configured") {
    println("This is also executed during the configuration phase, because :configured is used in the build.")
}

tasks.register("test") {
    doLast {
        println("This is executed during the execution phase.")
    }
}

tasks.register("testBoth") {
    doFirst {
        println("This is executed first during the execution phase.")
    }
    doLast {
        println("This is executed last during the execution phase.")
    }
    println("This is executed during the configuration phase as well, because :testBoth is used in the build.")
}
tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

gradle

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