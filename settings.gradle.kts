import org.gradle.authentication.http.BasicAuthentication
import java.util.Properties

val localProperties = Properties().apply {
    val file = file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

val mapboxDownloadsToken = providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN")
    .orElse(providers.environmentVariable("MAPBOX_DOWNLOADS_TOKEN"))
    .orElse(localProperties.getProperty("MAPBOX_DOWNLOADS_TOKEN", ""))

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                username = "mapbox"
                password = mapboxDownloadsToken.get()
            }
        }
    }
}

rootProject.name = "VendedoresMinum"
include(":app")
include(":shared")
