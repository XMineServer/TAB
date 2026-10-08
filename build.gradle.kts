plugins {
    id("tab.parent")
}

allprojects {
    group = "me.neznamy"
    // XMine: upstream's own number stays where upstream keeps it and changes only on a
    // rebase onto a new upstream release. The published version is the build address
    // <upstream>-<branch>-<date>-<hash>: .github/workflows/xmine-publish.yml reads the
    // upstream part from THIS line, computes the rest from git and passes the whole as
    // -PxmineVersion (wiki, conventions/versioning.md). It lands in the plugin descriptors
    // too, so the jar on the proxy says which build it is. Without -P a local build keeps
    // upstream's bare number, and publishing refuses (jar/build.gradle.kts).
    version = providers.gradleProperty("xmineVersion").getOrElse("6.1.3")
    description = "An all-in-one solution that works"

    ext.set("id", "tab")
    ext.set("website", "https://github.com/NEZNAMY/TAB")
    ext.set("author", "NEZNAMY")
    ext.set("credits", "Joseph T. McQuigg (JT122406)")
}

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
    ":velocity",
    ":fabric",
    ":neoforge"
//    ":forge"
)

val specialPaths = setOf(
    ":api",
    ":shared"
)

subprojects {
    when (path) {
        in platformPaths -> plugins.apply("tab.platform-conventions")
        in specialPaths -> plugins.apply("tab.standard-conventions")
        else -> plugins.apply("tab.base-conventions")
    }
}
