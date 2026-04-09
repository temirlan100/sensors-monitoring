plugins {
    kotlin("jvm") version "2.3.10" apply false
    kotlin("plugin.allopen") version "2.3.10" apply false
    id("io.quarkus") apply false
}

allprojects {
    group = "com.beamtrail"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.allopen")
    apply(plugin = "io.quarkus")

    val quarkusPlatformGroupId: String by project
    val quarkusPlatformArtifactId: String by project
    val quarkusPlatformVersion: String by project

    dependencies {
        "implementation"(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
        "implementation"("io.quarkus:quarkus-kotlin")
        "implementation"("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        "implementation"("io.quarkus:quarkus-arc")

        "testImplementation"("io.quarkus:quarkus-junit5")
    }

    configure<org.jetbrains.kotlin.allopen.gradle.AllOpenExtension> {
        annotation("jakarta.enterprise.context.ApplicationScoped")
        annotation("io.quarkus.test.junit.QuarkusTest")
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            javaParameters.set(true)
        }
    }
}
