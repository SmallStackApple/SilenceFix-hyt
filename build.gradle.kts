plugins {
    id("java")
}

group = "dev.xinxin.silencefix"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(fileTree("libs/"))
    annotationProcessor(files("libs/client/lombok.jar"))
}
