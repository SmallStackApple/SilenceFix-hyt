plugins {
    id("java")
}

group = "dev.xinxin.silencefix"
version = "1.0"

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

val clientLibs = fileTree("libs/client") { exclude("lombok.jar") }

dependencies {
    implementation(clientLibs)
    compileOnly(fileTree("libs/minecraft"))
    compileOnly(files("libs/client/lombok.jar"))
    annotationProcessor(files("libs/client/lombok.jar"))
}

tasks.jar {
    archiveBaseName.set("SilenceFix")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude(
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA",
        "META-INF/*.EC",
        "module-info.class"
    )
    from({
        clientLibs.filter { it.extension == "jar" }.map { zipTree(it) }
    })
}

val versionId = "SilenceFix-$version"

tasks.register("packageClient") {
    group = "build"
    description = "Packages the client into build/dist (versions/$versionId + libraries)"
    dependsOn(tasks.jar)

    doLast {
        val dist = layout.buildDirectory.dir("dist").get().asFile
        dist.deleteRecursively()

        val versionDir = File(dist, "versions/$versionId")
        versionDir.mkdirs()
        tasks.jar.get().archiveFile.get().asFile.copyTo(File(versionDir, "$versionId.jar"), overwrite = true)

        val jsonText = File(projectDir, "packaging/SilenceFix.json").readText().replace("@ID@", versionId)
        File(versionDir, "$versionId.json").writeText(jsonText)

        val renames = mapOf(
            "lwjgl-2.9.4.jar" to "lwjgl.jar",
            "lwjgl_util-2.9.4.jar" to "lwjgl_util.jar"
        )
        val json = groovy.json.JsonSlurper().parseText(jsonText) as Map<*, *>
        val libraries = json["libraries"] as List<Map<*, *>>
        for (lib in libraries) {
            val parts = (lib["name"] as String).split(":")
            val (g, n, v) = parts
            val natives = (lib["natives"] as? Map<*, *>)?.get("windows") as? String
            val fileName = "$n-$v" + (if (natives != null) "-$natives" else "") + ".jar"
            val src = File(projectDir, "libs/minecraft/" + (renames[fileName] ?: fileName))
            if (!src.exists()) throw GradleException("Missing library file: $src")
            val dest = File(dist, "libraries/${g.replace('.', '/')}/$n/$v/$fileName")
            dest.parentFile.mkdirs()
            src.copyTo(dest, overwrite = true)
        }
    }
}
