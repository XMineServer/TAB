import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.jvm.tasks.Jar

plugins {
    id("com.gradleup.shadow")
    `maven-publish`
}

val brokenPlatformPaths = setOf(
    ":bukkit:paper_1_20_5",
    ":bukkit:paper_1_21_2",
    ":bukkit:paper_1_21_4"
)

val platformPaths = setOf(
    ":bukkit",
    ":bukkit:paper_1_20_5",
    ":bukkit:paper_1_21_2",
    ":bukkit:paper_1_21_4",
    ":bukkit:paper_1_21_9",
    ":bukkit:paper_1_21_11",
    ":bukkit:paper_26_2",
    ":bukkit:v1_7_R4",
    ":bukkit:v1_8_R3",
    ":bukkit:v1_12_R1",
    ":bukkit:v1_16_R3",
    ":bukkit:v1_17_R1",
    ":bukkit:v1_18_R2",
    ":bukkit:v1_19_R1",
    ":bukkit:v1_19_R2",
    ":bukkit:v1_19_R3",
    ":bukkit:v1_20_R1",
    ":bukkit:v1_20_R2",
    ":bukkit:v1_20_R3",
    ":bukkit:v1_20_R4",
    ":bukkit:v1_21_R1",
    ":bukkit:v1_21_R2",
    ":bukkit:v1_21_R3",
    ":bukkit:v1_21_R4",
    ":bukkit:v1_21_R5",
    ":bukkit:v1_21_R6",
    ":bukkit:v1_21_R7",
    ":bukkit:v26_1",
    ":bukkit:v26_2",
    ":bungeecord",
    ":fand",
    ":velocity"
)

val moddedPaths = setOf(
    ":fabric",
    ":neoforge",
    ":forge"
)

val brokenPlatforms: List<Project> = brokenPlatformPaths.map { rootProject.project(it) }
val platforms: List<Project> = platformPaths.map { rootProject.project(it) }
val moddedPlatforms: List<Project> = moddedPaths.map { rootProject.project(it) }

tasks {
    shadowJar {
        archiveFileName.set("TAB v${project.version}.jar")
        // XMine: the universal jar is published as the main artifact, so it must carry no
        // Maven classifier. Shadow defaults archiveClassifier to "all", and maven-publish
        // reads the classifier off the task - archiveFileName above does not affect it,
        // so without this the jar would land in Reposilite as tab-<version>-all.jar.
        archiveClassifier.set("")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        fun registerPlatform(project: Project, jarTask: AbstractArchiveTask) {
            dependsOn(jarTask)
            dependsOn(project.tasks.withType<Jar>())
            from(zipTree(jarTask.archiveFile))
        }

        platforms.forEach { p ->
            val task = p.tasks.named<ShadowJar>("shadowJar").get()
            registerPlatform(p, task)
        }

        moddedPlatforms.forEach { p ->
            val task = p.tasks.named<Jar>("jar").get()
            registerPlatform(p, task)
        }
    }

    val shadowJarBrokenPaper = register<ShadowJar>("shadowJarBrokenPaper") {
        description = "Shadows only Paper versions 1.20.5 - 1.21.4, which break if jar has classes compiled with Java 24+."
        archiveFileName.set("TAB v${project.version} - Paper 1.20.5 - 1.21.4.jar")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        fun registerPlatform(project: Project, jarTask: AbstractArchiveTask) {
            dependsOn(jarTask)
            dependsOn(project.tasks.withType<Jar>())
            from(zipTree(jarTask.archiveFile))
        }

        brokenPlatforms.forEach { p ->
            val task = p.tasks.named<ShadowJar>("shadowJar").get()
            registerPlatform(p, task)
        }
    }

    build.get().dependsOn(shadowJar, shadowJarBrokenPaper)
}

// XMine: publish the universal jar (the one we actually run on the proxy - not the
// Paper-specific "broken Paper" jar) into our Reposilite third-party section.
publishing {
    publications {
        create<MavenPublication>("xmine") {
            groupId = "ru.xmine.thirdparty"
            artifactId = "tab"
            version = project.version.toString()

            artifact(tasks.named<ShadowJar>("shadowJar"))

            pom {
                name.set("TAB")
                description.set(
                    "TAB " + project.version + " - fork of NEZNAMY/TAB with environment " +
                    "variable substitution (!ENV) in YAML configuration files."
                )
                url.set("https://github.com/XMineServer/TAB")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "xmine"
            url = uri("https://maven.xmine.world/third-party")
            credentials {
                username = (providers.gradleProperty("xmineMavenUsername")
                    .orElse(providers.environmentVariable("XMINE_MAVEN_USERNAME"))).orNull
                password = (providers.gradleProperty("xmineMavenPassword")
                    .orElse(providers.environmentVariable("XMINE_MAVEN_PASSWORD"))).orNull
            }
        }
    }
}
