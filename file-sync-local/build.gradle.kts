import org.ajoberstar.grgit.Grgit
import org.unbrokendome.gradle.plugins.testsets.dsl.testSets

plugins {
  id("org.unbroken-dome.test-sets") version "4.0.0"
  id("com.google.cloud.tools.jib") version "3.3.2"
  id("org.ajoberstar.grgit") version "5.2.0"
}

testSets {
  create("integrationTest")
  create("performanceTest")
}

jib {
  from {
    image = "amazoncorretto:19"
  }
  to {
    image = "ghcr.io/will-molloy/file-sync-local"
  }
  container {
    mainClass = "com.willmolloy.sync.local.Main"
    environment = mapOf("SOURCE_PATH" to "/source", "DESTINATION_PATH" to "/destination")
    creationTime.set(gitCommitTime())
    jvmFlags = listOf("--enable-preview", "--add-modules", "jdk.incubator.concurrent")
  }
}

fun gitCommitTime(): String {
  Grgit.open(mapOf("currentDir" to rootProject.rootDir)).use {
    return it.head().dateTime.toInstant().toString()
  }
}

dependencies {
  implementation(project(":file-sync-core"))
}