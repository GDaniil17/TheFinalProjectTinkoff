import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "me.daniil"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api("org.telegram:telegrambots:5.2.0")
    api("org.telegram:telegrambots-abilities:5.2.0")
    api("org.slf4j:slf4j-simple:1.6.1")
    api("org.postgresql", "postgresql", "42.2.20")
}

tasks.withType<Test> {
    useJUnitPlatform()
    doLast {
        println("This is executed during the execution phase.")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "11"
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveFileName.set("kursach.jar")
        mergeServiceFiles()
        manifest {
            attributes(
                mapOf<String, String>(Pair("Main-Class", "MainKt"))
            )
        }
    }
}
