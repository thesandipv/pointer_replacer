plugins {
  id(afterroot.plugins.kotlin.jvm.get().pluginId)
  alias(libs.plugins.google.protobuf)
}

// Setup protobuf configuration, generating lite Java and Kotlin classes
protobuf {
  protoc {
    artifact = libs.protobuf.protoc.get().toString()
  }
  generateProtoTasks {
    all().configureEach {
      builtins {
        named("java") {
          option("lite")
        }
        register("kotlin") {
          option("lite")
        }
      }
    }
  }
}

dependencies {
  api(libs.protobuf.kotlin.lite)
}
