import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.Properties

plugins {
	`java-library`

	id("maven-publish")
	id("com.gradleup.shadow") version "9.4.1"
	id("com.diffplug.spotless") version "8.5.1"

	// auto update dependencies with 'useLatestVersions' task
	id("se.patrikerdes.use-latest-versions") version "0.2.19"
	id("com.github.ben-manes.versions") version "0.54.0"
}

dependencies {
	compileOnly("org.jetbrains:annotations:26.1.0")

	// use compile only scope to exclude jadx-core and its dependencies from result jar
	compileOnly("io.github.skylot:jadx-cli:1.5.5")
	compileOnly("io.github.skylot:jadx-core:1.5.5")
	compileOnly("io.github.skylot:jadx-gui:1.5.5")

	// use same versions as jadx-gui
	compileOnly("com.fifesoft:rsyntaxtextarea:3.6.1")
	compileOnly("org.apache.commons:commons-lang3:3.20.0")
	compileOnly("commons-io:commons-io:2.21.0")
	compileOnly("ch.qos.logback:logback-classic:1.5.32")

	// use same versions as in jadx-java-convert
	compileOnly("org.ow2.asm:asm:9.9.1")
	compileOnly("org.ow2.asm:asm-tree:9.9.1")

	implementation("org.apktool:apktool-lib:3.0.2") {
		// exclude iBotPeaches fork, use provided version of jadx
		// Known Issues are https://github.com/iBotPeaches/Apktool/issues/3767 and https://github.com/iBotPeaches/Apktool/issues/3943
		// See https://github.com/iBotPeaches/Apktool/pull/4027
		exclude(group = "com.github.iBotPeaches.smali", module = "smali-baksmali")
		exclude(group = "com.github.iBotPeaches.smali", module = "smali")
		exclude(group = "com.google.guava", module = "guava")
		exclude(group = "commons-io", module = "commons-io")
		exclude(group = "org.apache.commons", module = "commons-text")
	}

	implementation("de.femtopedia.dex2jar:dex-tools:2.4.35")
	implementation("com.github.javaparser:javaparser-core:3.28.1")

	testImplementation("io.github.skylot:jadx-cli:1.5.5")
	testImplementation("io.github.skylot:jadx-core:1.5.5")

	testImplementation("org.apache.commons:commons-lang3:3.20.0")
	testImplementation("ch.qos.logback:logback-classic:1.5.32")
	testImplementation("org.assertj:assertj-core:3.27.7")
	testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.3")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.3")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.3")
}

allprojects {
	apply(plugin = "java")
	apply(plugin = "com.diffplug.spotless")

	configure<SpotlessExtension> {
		java {
			importOrderFile("$rootDir/config/code-formatter/eclipse.importorder")
			eclipse().configFile("$rootDir/config/code-formatter/eclipse.xml")
			removeUnusedImports()
			commonFormatOptions()
		}
		kotlinGradle {
			ktlint()
			commonFormatOptions()
		}
		format("misc") {
			target("**/*.gradle", "**/*.xml", "**/.gitignore", "**/.properties")
			targetExclude(".gradle/**", ".idea/**", "*/build/**")
			commonFormatOptions()
		}
	}
}

fun FormatExtension.commonFormatOptions() {
	lineEndings = LineEnding.UNIX
	encoding = Charsets.UTF_8
	trimTrailingWhitespace()
	endWithNewline()
}

repositories {
	mavenLocal()
	mavenCentral()
	google()
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

group = "com.github.nitram84"
version = System.getenv("VERSION") ?: "dev"

tasks {
	withType(Test::class) {
		useJUnitPlatform()
	}
	val shadowJar =
		withType(ShadowJar::class) {
			archiveClassifier.set("") // remove '-all' suffix
		}

	// copy result jar into "build/dist" directory
	register<Copy>("dist") {
		dependsOn(shadowJar)
		dependsOn(withType(Jar::class))

		from(shadowJar)
		into(layout.buildDirectory.dir("dist"))
	}
}

val generateVersionProperties by tasks.registering {
	val outputDir = layout.buildDirectory.dir("generated/resources")
	val outputFile = outputDir.get().file("versions.properties")
	outputs.dir(outputDir)

	doLast {
		val apktoolDep =
			configurations.implementation.get().dependencies.find {
				it.group == "org.apktool" && it.name == "apktool-lib"
			}

		if (apktoolDep != null && apktoolDep.version != null) {
			val props = Properties()
			props.setProperty("apktool.version", apktoolDep.version)
			outputFile.asFile.writer().use { writer ->
				props.store(writer, "Do not edit - This file is generated.")
			}
		}
	}
}

tasks.processResources {
	dependsOn(generateVersionProperties)
}

sourceSets {
	main {
		resources.srcDir(generateVersionProperties)
	}
}

publishing {
	publications {
		create<MavenPublication>("shadow") {
			from(components["shadow"])
		}
	}
}
