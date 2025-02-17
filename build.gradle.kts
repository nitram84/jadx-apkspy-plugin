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
	id("com.github.ben-manes.versions") version "0.51.0"
}

dependencies {
	// use compile only scope to exclude jadx-core and its dependencies from result jar
	compileOnly("io.github.skylot:jadx-core:1.5.1")
	compileOnly("io.github.skylot:jadx-gui:1.5.1")

	// use same versions as jadx-gui
	compileOnly("com.fifesoft:rsyntaxtextarea:3.5.3")
	compileOnly("org.apache.commons:commons-lang3:3.17.0")
	compileOnly("commons-io:commons-io:2.18.0")
	compileOnly("ch.qos.logback:logback-classic:1.5.12")

	// use same versions as in jadx-java-convert
	compileOnly("org.ow2.asm:asm:9.7.1")
	implementation("org.ow2.asm:asm-tree:9.7.1")

	implementation("de.femtopedia.dex2jar:dex-tools:2.4.24")
	implementation("com.github.javaparser:javaparser-core:3.26.3")

	testImplementation("org.apache.commons:commons-lang3:3.17.0")
	testImplementation("ch.qos.logback:logback-classic:1.5.12")
	testImplementation("org.assertj:assertj-core:3.26.3")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
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
