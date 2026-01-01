plugins {
    id("java")
}

group = "dev.xinxin.silencefix"
version = "1.0-SNAPSHOT"

sourceSets {
    main {
        java.srcDir("src/main/java")
        resources.srcDir("src/main/resources")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(fileTree("libs/"))
}