import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
	`java-library`

	id("com.github.johnrengelman.shadow") version "8.1.1"
	id("com.diffplug.spotless") version "6.25.0"

	// auto update dependencies with 'useLatestVersions' task
	id("se.patrikerdes.use-latest-versions") version "0.2.18"
	id("com.github.ben-manes.versions") version "0.50.0"
}

dependencies {
	// use compile only scope to exclude jadx-core and its dependencies from result jar
	compileOnly("io.github.skylot:jadx-core:1.5.1-20240929.211704-2") {
		isChanging = true
	}
	compileOnly("io.github.skylot:jadx-gui:1.5.1-SNAPSHOT") {
		isChanging = true
	}

	// use same versions as jadx-gui
	compileOnly("com.fifesoft:rsyntaxtextarea:3.4.1")
	compileOnly("org.apache.commons:commons-lang3:3.17.0")
	compileOnly("commons-io:commons-io:2.17.0")
	compileOnly("ch.qos.logback:logback-classic:1.5.8")

	// use same versions as in jadx-java-input
	implementation("org.ow2.asm:asm:9.7")
	implementation("org.ow2.asm:asm-tree:9.7")

	implementation("de.femtopedia.dex2jar:dex-tools:2.4.22")
	implementation("com.github.javaparser:javaparser-core:3.25.10")

	testImplementation("org.apache.commons:commons-lang3:3.17.0")
	testImplementation("ch.qos.logback:logback-classic:1.5.8")
	testImplementation("org.assertj:assertj-core:3.24.2")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
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
	maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
	google()
}

java {
	sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11
}

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
