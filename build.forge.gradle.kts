plugins {
	id("mod-platform")
	id("net.neoforged.moddev.legacyforge")
}

stonecutter {
	replacements.string(current.parsed < "1.21") {
		replace("ResourceLocation.fromNamespaceAndPath(", "new ResourceLocation(")
		replace("ResourceLocation.parse(", "new ResourceLocation(")
		replace("ResourceLocation.withDefaultNamespace(", "new ResourceLocation(")
		replace("net.neoforged.neoforge.common.ModConfigSpec", "net.minecraftforge.common.ForgeConfigSpec")
		replace("ModConfigSpec", "ForgeConfigSpec")
		replace("Properties.ofFullCopy(", "Properties.copy(")
		replace(".openPath(RiverbankPalettes.userFolder())", ".openFile(RiverbankPalettes.userFolder().toFile())")
		replace(".openPath(WaterPlantSets.userFolder())", ".openFile(WaterPlantSets.userFolder().toFile())")
	}
}

platform {
	loader = "forge"
	dependencies {
		required("minecraft") {
			forgeLikeVersionRange = prop("deps.minecraft")
		}
		required("forge") {
			forgeLikeVersionRange.set("[${prop("deps.forge")},)")
		}
	}
}

legacyForge {
	version = "${prop("deps.minecraft")}-${prop("deps.forge")}"

	validateAccessTransformers = true

	accessTransformers.from(
		rootProject.file("src/main/resources/aw/${sc.current.version}.cfg")
	)

	val devJvmArgs = propsList("mod", "dev_jvm_args") +
		propsList("mod", "dev_jvm_args_mixin_debug")
	runs {
		register("client") {
			client()
			gameDirectory = file(providers.gradleProperty("runDir").getOrElse("run"))
			ideName = "Forge Client (${sc.current.version})"
			programArgument("--username=Dev")
			jvmArguments.addAll(devJvmArgs)

			providers.gradleProperty("quickPlay").orNull?.let {
				programArgument("--quickPlaySingleplayer")
				programArgument(it)
			}
		}
		register("server") {
			server()

			gameDirectory = file(providers.gradleProperty("runDir").getOrElse("run"))
			ideName = "Forge Server (${sc.current.version})"
			jvmArguments.addAll(devJvmArgs)
		}
	}

	mods {
		register(prop("mod.id")) {
			sourceSet(sourceSets["main"])
		}
	}
}

mixin {
	add(sourceSets.main.get(), "${prop("mod.id")}.refmap.json")
	config("${prop("mod.id")}.mixins.json")
}

repositories {
	mavenCentral()
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
}

dependencies {
	annotationProcessor("org.spongepowered:mixin:${libs.versions.mixin.get()}:processor")

	if (providers.gradleProperty("withVs").isPresent) {
		modRuntimeOnly("maven.modrinth:valkyrien-skies:1.20.1-forge-2.4.11")
		modRuntimeOnly("maven.modrinth:kotlin-for-forge:4.12.0")
	}
}

sourceSets {
	main {
		resources.srcDir(
			"${rootDir}/versions/datagen/${sc.current.version}/src/main/generated"
		)
	}
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}
