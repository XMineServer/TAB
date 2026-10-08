import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.jvm.tasks.Jar

plugins {
    id("com.gradleup.shadow")
    `maven-publish`
}

val brokenPlatformPaths = emptySet<String>()

// XMine: only the Velocity platform is built (settings.gradle.kts).
val platformPaths = setOf(":velocity")

val moddedPaths = emptySet<String>()

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

    build.get().dependsOn(shadowJar)
}

// XMine: publish the Velocity-only jar (the one we run on the proxy) into our Reposilite. The section is the fork
// candidate section fork-snapshot, not third-party: third-party mirrors foreign jars,
// this one we patch and build ourselves (wiki, ADR-0056). The section comes in via
// XMINE_MAVEN_URL from xmine-publish.yml.
publishing {
    publications {
        create<MavenPublication>("xmineFork") {
            groupId = "ru.xmine.thirdparty"
            artifactId = "tab-velocity"
            version = project.version.toString()

            artifact(tasks.named<ShadowJar>("shadowJar"))

            pom {
                name.set("TAB (Velocity)")
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
            url = uri(providers.environmentVariable("XMINE_MAVEN_URL").getOrElse("https://maven.xmine.world/fork-snapshot"))
            credentials {
                username = (providers.gradleProperty("xmineMavenUsername")
                    .orElse(providers.environmentVariable("XMINE_MAVEN_USERNAME"))).orNull
                password = (providers.gradleProperty("xmineMavenPassword")
                    .orElse(providers.environmentVariable("XMINE_MAVEN_PASSWORD"))).orNull
            }
        }
    }
}

// XMine: publish only with a build address. Without -PxmineVersion the version is
// upstream's bare number, and a local `publish` would put a coordinate that is not a build
// address into the candidate section - and coordinates there are immutable.
val xmineVersion = providers.gradleProperty("xmineVersion")
tasks.withType<PublishToMavenRepository>().configureEach {
    doFirst {
        if (!xmineVersion.isPresent) {
            throw GradleException("Publishing needs -PxmineVersion: the build address computed by .github/workflows/xmine-publish.yml")
        }
    }
}
