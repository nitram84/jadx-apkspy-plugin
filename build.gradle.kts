import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
	`java-library`

	id("com.gradleup.shadow") version "9.3.1"
	id("com.diffplug.spotless") version "7.0.4"

	// auto update dependencies with 'useLatestVersions' task
	id("se.patrikerdes.use-latest-versions") version "0.2.19"
	id("com.github.ben-manes.versions") version "0.53.0"
}

dependencies {
	// use compile only scope to exclude jadx-core and its dependencies from result jar
	compileOnly("io.github.skylot:jadx-cli:1.5.3")
	compileOnly("io.github.skylot:jadx-core:1.5.3")
	compileOnly("io.github.skylot:jadx-gui:1.5.3")

	// use same versions as jadx-gui
	compileOnly("com.fifesoft:rsyntaxtextarea:3.6.0")
	compileOnly("org.apache.commons:commons-lang3:3.20.0")
	compileOnly("commons-io:commons-io:2.21.0")
	compileOnly("ch.qos.logback:logback-classic:1.5.22")

	// use same versions as in jadx-java-convert
	compileOnly("org.ow2.asm:asm:9.9.1")
	implementation("org.ow2.asm:asm-tree:9.9.1")

	implementation("de.femtopedia.dex2jar:dex-tools:2.4.34")
	implementation("com.github.javaparser:javaparser-core:3.28.0")

	testImplementation("io.github.skylot:jadx-cli:1.5.3")
	testImplementation("io.github.skylot:jadx-core:1.5.3")

	testImplementation("org.apache.commons:commons-lang3:3.20.0")
	testImplementation("ch.qos.logback:logback-classic:1.5.22")
	testImplementation("org.assertj:assertj-core:3.27.7")
	testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.2")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.2")
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
