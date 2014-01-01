@Grapes([
	@Grab("commons-io:commons-io:2.4"),
	@Grab("com.github.pyrolistical:best-champion-region-services:1-SNAPSHOT")
])

import org.apache.commons.io.*

def templateDirectory = "/Users/rchen/dev/projects/github.com/concept-not-found/bestchampionregion/template"
def outputDirectory = "/Users/rchen/dev/projects/github.com/concept-not-found/bestchampionregion-pages"

def tasks = args

if (!tasks) {
	tasks = ["all"]
}

tasks.each {
	"$it"(templateDirectory, outputDirectory)
}

def all(templateDirectory, outputDirectory) {
	clean(templateDirectory, outputDirectory)
	statics(templateDirectory, outputDirectory)
	about(templateDirectory, outputDirectory)
	summoner_ratings(templateDirectory, outputDirectory)
	top_summoner(templateDirectory, outputDirectory)
	top_champion(templateDirectory, outputDirectory)
}

def clean(templateDirectory, outputDirectory) {
	new File(outputDirectory).eachFile {
		if (it.name.startsWith(".")) {
			// ignore hidden files such as .git
			return
		}
		if (it.isDirectory()) {
			it.deleteDir()
		} else {
			it.delete()
		}
	}
}

def statics(templateDirectory, outputDirectory) {
	[
			"css",
			"img",
			"js"
	].each {
		FileUtils.copyDirectory(
				new File(templateDirectory, it),
				new File(outputDirectory, it))
	}

	[
			"CNAME",
			"index.html"
	].each {
		FileUtils.copyFile(
			new File(templateDirectory, it),
			new File(outputDirectory, it))
	}
}

def about(templateDirectory, outputDirectory) {
	run(new File("generate-about-page.groovy"), [
			templateDirectory,
			outputDirectory
	] as String[])
}

def summoner_ratings(templateDirectory, outputDirectory) {
	def classloader = new GroovyClassLoader()
	def shell = new GroovyShell(classloader, getBinding())
	shell.run(new File("generate_summoner_ratings_page.groovy"), [
			templateDirectory,
			outputDirectory
	] as String[])
}

def top_summoner(templateDirectory, outputDirectory) {
	run(new File("generate_top_summoner_page.groovy"), [
			templateDirectory,
			outputDirectory
	] as String[])
}

def top_champion(templateDirectory, outputDirectory) {
	run(new File("generate_top_champion_page.groovy"), [
			templateDirectory,
			outputDirectory
	] as String[])
}
