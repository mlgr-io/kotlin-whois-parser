import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {

    // use this map to add or override versions
    extra["versions"] = mapOf(
        "commons-net" to "3.8.0",
        "jackson" to "2.13.3",
        "mockk" to "1.12.0",
        "parboiled" to "1.4.1",
        "reflections" to "0.10.2",
    )

    apply(from = "https://raw.githubusercontent.com/mlgr-io/kotlin-skeleton/0.1/01-versions.gradle.kts")
    val plugins: List<String> = extra["plugins"] as List<String>

    dependencies {
        plugins.forEach { classpath(it) }
        // add additional buildscript plugins here
    }
}

extra["kotlinVersion"] = plugins.getPlugin(KotlinPluginWrapper::class.java).pluginVersion
apply(from = "https://raw.githubusercontent.com/mlgr-io/kotlin-skeleton/0.1/02-configuration.gradle.kts")

plugins {
    kotlin("jvm") version "1.8.0" // this should be the only place where the kotlin version is hard-coded
    // add additional plugins here
}

base {
    group = "io.mailguru"
    archivesName.set("mailguru-whois-parser")
}

val versions: Map<String, String> = (extra["versions"] as Map<String, String>)
dependencies {
    // add additional dependencies here
    implementation("commons-net:commons-net:" + versions["commons-net"])
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.parboiled:parboiled-java:" + versions["parboiled"])
    implementation("org.reflections:reflections:" + versions["reflections"])
    testImplementation("io.mockk:mockk:" + versions["mockk"])
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

plugins.withId("com.vanniktech.maven.publish.base") {
    configure<MavenPublishBaseExtension> {
        publishToMavenCentral(SonatypeHost.S01)
        signAllPublications()
        pom {
            name.set(project.name)
            description.set("A library for WHOIS response parsing.")
            url.set("https://github.com/mlgr-io/kotlin-whois-parser")
            licenses {
                license {
                    name.set("GNU General Public License, Version 3")
                    url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
                }
            }
            developers {
                developer {
                    name.set("Sascha Weyers")
                    email.set("sw@mailguru.io")
                    organization.set("mailguru UG (haftungsbeschränkt)")
                    organizationUrl.set("https://mailguru.io")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/mlgr-io/kotlin-whois-parser.git")
                developerConnection.set("scm:git:ssh://github.com:mlgr-io/kotlin-whois-parser.git")
                url.set("https://github.com/mlgr-io/kotlin-whois-parser")
            }
        }
        repositories {
            maven {
                url = if (version.toString().endsWith("SNAPSHOT")) {
                    uri(layout.buildDirectory.dir("repos/snapshots"))
                } else {
                    uri(layout.buildDirectory.dir("repos/releases"))
                }
            }
        }
    }
}
