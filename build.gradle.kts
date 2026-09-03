import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.Properties

plugins {
	`java-library`

	id("maven-publish")
	alias(libs.plugins.shadow)
	alias(libs.plugins.spotless)

	// auto update dependencies with 'useLatestVersions' task
	alias(libs.plugins.use.latest.versions)
	alias(libs.plugins.ben.manes.versions)
}

val apkArtifact: Configuration = configurations.create("apkArtifact")

dependencies {
	compileOnly(libs.annotations)

	// use compile only scope to exclude jadx-core and its dependencies from result jar
	compileOnly(libs.jadx.cli)
	compileOnly(libs.jadx.core)
	compileOnly(libs.jadx.gui)

	// use same versions as jadx-gui
	compileOnly(libs.rsyntaxtextarea)
	compileOnly(libs.commons.lang3)
	compileOnly(libs.commons.io)
	compileOnly(libs.logback.classic)

	// use same versions as in jadx-java-convert
	compileOnly(libs.asm)
	compileOnly(libs.asm.tree)

	implementation(libs.apktool.lib) {
		// exclude iBotPeaches fork, use provided version of jadx
		// Known Issues are https://github.com/iBotPeaches/Apktool/issues/3767 and https://github.com/iBotPeaches/Apktool/issues/3943
		// See https://github.com/iBotPeaches/Apktool/pull/4027
		exclude(group = "com.github.iBotPeaches.smali", module = "smali-baksmali")
		exclude(group = "com.github.iBotPeaches.smali", module = "smali")
		exclude(group = "com.google.guava", module = "guava")
		exclude(group = "commons-io", module = "commons-io")
		exclude(group = "org.apache.commons", module = "commons-text")
	}

	implementation(libs.dex.tools)
	implementation(libs.javaparser)

	testImplementation(libs.jadx.cli)
	testImplementation(libs.jadx.core)

	testImplementation(libs.commons.lang3)
	testImplementation(libs.logback.classic)
	testImplementation(libs.assertj.core)
	testImplementation(libs.junit.jupiter.api)
	testRuntimeOnly(libs.junit.jupiter.engine)
	testRuntimeOnly(libs.junit.platform.launcher)

	apkArtifact("org.beigesoft:beige-uml-android:2.1.11:aligned@apk")
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

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
}

val shadowJar =
	tasks.withType<ShadowJar>().map { shadowTask ->
		shadowTask.archiveClassifier.set("") // remove '-all' suffix
		shadowTask
	}

// copy result jar into "build/dist" directory
val dist =
	tasks.register<Copy>("dist") {
		dependsOn(shadowJar)
		dependsOn(tasks.withType<Jar>())
		from(shadowJar)
		into(layout.buildDirectory.dir("dist"))
	}

val generateVersionProperties =
	tasks.register("generateVersionProperties") {
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

val copyApkToTestResources =
	tasks.register<Copy>("copyApkToTestResources") {
		from(apkArtifact)
		into(layout.buildDirectory.dir("resources/test"))
	}

tasks.processResources {
	dependsOn(generateVersionProperties)
}

tasks.test {
	useJUnitPlatform()
	dependsOn(copyApkToTestResources)
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

tasks.named("generateMetadataFileForShadowPublication") {
	mustRunAfter(tasks.named("jar"))
}
