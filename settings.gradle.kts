enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        mavenCentral() // Netty, SnakeYaml, json-simple, Guava, Kyori event, bStats, AuthLib, LuckPerms
        maven("https://repo.viaversion.com/") // ViaVersion
        maven("https://repo.william278.net/releases/") // VelocityScoreboardAPI
        maven("https://repo.codemc.org/repository/nms/") // CraftBukkit + NMS
        maven("https://repo.papermc.io/repository/maven-public/") // paperweight, Velocity, Adventure, BungeeCord-API
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceholderAPI
        maven("https://repo.opencollab.dev/maven-snapshots/") // Floodgate, Bungeecord-proxy
        maven("https://repo.purpurmc.org/snapshots") // Purpur
        maven("https://jitpack.io") // PremiumVanish, Vault, YamlAssist, RedisBungee
        maven("https://mvn.lib.co.nz/public") // LibsDisguises
        maven("https://repo.william278.net/velocity/") // Velocity-proxy
        // XMine: модуль :fand исключён — repo.fandmc.cn лежит (502), а Fand у нас не используется.
    }
}

pluginManagement {
    includeBuild("build-logic")
    repositories {
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://maven.architectury.dev/")
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "TAB"

include(":api")
include(":shared")
include(":velocity")
// XMine: собираем только Velocity — TAB у нас стоит лишь на прокси (на нодах TAB-Bridge).
// Платформы Bukkit/BungeeCord/модовые не нужны и тянут лишние репозитории.
include(":jar")
