import org.ajoberstar.grgit.Grgit

plugins {
  id("com.google.cloud.tools.jib") version "3.3.2"
  id("org.ajoberstar.grgit") version "5.2.0"
}

jib {
  from {
    image = "amazoncorretto:19"
  }
  to {
    image = "ghcr.io/will-molloy/file-sync-s3"
  }
  container {
    mainClass = "com.willmolloy.sync.s3.Main"
    environment = mapOf(
      "SOURCE_PATH" to "/source",
      "DESTINATION_BUCKET" to "",
      "DESTINATION_BUCKET_PREFIX" to "/"
    )
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
  implementation(project(":file-sync-local"))
  implementation("software.amazon.awssdk:s3:2.20.93")
}