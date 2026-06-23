import com.hypherionmc.modpublisher.properties.CurseEnvironment
import com.hypherionmc.modpublisher.properties.ModLoader
import com.hypherionmc.modpublisher.properties.ReleaseType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    `maven-publish`
    idea
    kotlin("jvm") version "2.2.20"
    id("net.neoforged.moddev") version "2.0.141"
    id("com.hypherionmc.modutils.modpublisher") version "2.2.+"
    id("com.diffplug.spotless") version "8.0.0"
}

val minecraftVersion: String = property("minecraft_version").toString()
val minecraftVersionRange: String = property("minecraft_version_range").toString()
val neoVersion: String = property("neo_version").toString()
val neoVersionRange: String = property("neo_version_range").toString()
val loaderVersionRange: String = property("loader_version_range").toString()
val ae2Version: String = property("ae2_version").toString()
val ae2VersionRange: String = property("ae2_version_range").toString()
val emiVersion: String = property("emi_version").toString()
val jadeVersion: String = property("jade_version").toString()
val appliedSortingVersion: String = property("applied_sorting_version").toString()
val parchmentMinecraftVersion: String = property("parchment_minecraft_version").toString()
val parchmentMappingsVersion: String = property("parchment_mappings_version").toString()
val modId: String = property("mod_id").toString()
val modName: String = property("mod_name").toString()
val modLicense: String = property("mod_license").toString()
val modVersion: String = property("mod_version").toString()
val modGroupId: String = property("mod_group_id").toString()
val modAuthors: String = property("mod_authors").toString()
val modDescription: String = property("mod_description").toString()
val curseforgeProjectId: String = property("curseforge_project_id").toString()
val modrinthProjectId: String = property("modrinth_project_id").toString()
val githubRepository: String = property("github_repository").toString()
val licenseFileName = "LICENSE"

version = modVersion
group = modGroupId

repositories {
    mavenCentral()
    maven {
        name = "ModMaven"
        url = uri("https://modmaven.dev/")
    }
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
    }
    maven {
        name = "TerraformersMC"
        url = uri("https://maven.terraformersmc.com/")
    }
}

base {
    archivesName = modId
}

java {
    withSourcesJar()
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

kotlin {
    jvmToolchain(21)
}

neoForge {
    version = neoVersion

    parchment {
        mappingsVersion = parchmentMappingsVersion
        minecraftVersion = parchmentMinecraftVersion
    }

    runs {
        register("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        register("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        register("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        register("data") {
            data()
            programArguments.addAll(
                "--mod",
                modId,
                "--all",
                "--output",
                file("src/generated/resources/").absolutePath,
                "--existing",
                file("src/main/resources/").absolutePath
            )
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets["main"])
        }
    }
}

dependencies {
    compileOnly("org.appliedenergistics:appliedenergistics2:$ae2Version")
    runtimeOnly("org.appliedenergistics:appliedenergistics2:$ae2Version")

    runtimeOnly("dev.emi:emi-neoforge:$emiVersion")
    runtimeOnly("maven.modrinth:jade:$jadeVersion")
    runtimeOnly("maven.modrinth:applied-sorting:$appliedSortingVersion")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.jar {
    manifest {
        attributes(
            "Specification-Title" to modName,
            "Specification-Vendor" to modAuthors,
            "Specification-Version" to modVersion,
            "Implementation-Title" to project.name,
            "Implementation-Version" to modVersion,
            "Implementation-Vendor" to modAuthors,
            "Built-On-Minecraft" to minecraftVersion,
            "MixinConfigs" to "$modId.mixins.json"
        )
    }
}

tasks.named<Jar>("sourcesJar") {
    from(rootProject.file("LICENSE")) {
        rename("LICENSE", licenseFileName)
    }
}

val generateModMetadata by tasks.registering(ProcessResources::class) {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "neo_version" to neoVersion,
        "neo_version_range" to neoVersionRange,
        "loader_version_range" to loaderVersionRange,
        "ae2_version_range" to ae2VersionRange,
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_license" to modLicense,
        "mod_version" to modVersion,
        "mod_authors" to modAuthors,
        "mod_description" to modDescription
    )

    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into("build/generated/sources/modMetadata")
}

tasks.processResources {
    from(rootProject.file("LICENSE")) {
        rename("LICENSE", licenseFileName)
    }
    dependsOn(generateModMetadata)
}

sourceSets {
    main {
        resources {
            srcDir("src/generated/resources")
            srcDir(generateModMetadata)
        }
    }
}

neoForge.ideSyncTask(generateModMetadata)

publisher {
    apiKeys {
        curseforge(System.getenv("CURSEFORGE_TOKEN"))
        modrinth(System.getenv("MODRINTH_TOKEN"))
        github(System.getenv("GITHUB_TOKEN"))
    }

    setReleaseType(ReleaseType.RELEASE)
    setLoaders(ModLoader.NEOFORGE)
    setCurseEnvironment(CurseEnvironment.CLIENT)

    curseID.set(curseforgeProjectId)
    modrinthID.set(modrinthProjectId)
    changelog.set(providers.fileContents(layout.projectDirectory.file("CHANGELOG.md")).asText)
    projectVersion.set(modVersion)
    displayName.set("[$minecraftVersion] v$modVersion")
    setGameVersions(minecraftVersion)
    setJavaVersions(21)
    artifact.set(tasks.named("jar"))
    addAdditionalFile(tasks.named("sourcesJar"))

    curseDepends {
        required("applied-energistics-2")
    }

    modrinthDepends {
        optional("ae2")
    }

    github {
        repo(githubRepository)
        tag("$minecraftVersion/v$modVersion")
        displayName("[$minecraftVersion] v$modVersion")
        createTag(true)
        createRelease(true)
        updateRelease(true)
        target("1.21.1")
    }
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri("file://${project.projectDir}/repo")
        }
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

spotless {
    encoding("UTF-8")

    format("misc") {
        target(".gitignore")

        trimTrailingWhitespace()
        indentWithSpaces(4)
        endWithNewline()
    }
    java {
        target("src/*/java/**/*.java", "src/*/scala/**/*.java")

        toggleOffOn()
        importOrderFile(file("spotless.importorder"))
        removeUnusedImports()
        eclipse("4.19").configFile(file("spotless.eclipseformat.xml"))
    }
    kotlin {
        target("src/*/kotlin/**/*.kt", "src/*/java/**/*.kt")

        toggleOffOn()
        trimTrailingWhitespace()
        endWithNewline()
        ktlint("1.7.1").editorConfigOverride(
            mapOf(
                "ktlint_code_style" to "intellij_idea"
            )
        )
    }
    scala {
        target("src/*/scala/**/*.scala")

        scalafmt("3.7.15")
    }
}
