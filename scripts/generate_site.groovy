@Grapes([
	@Grab("commons-io:commons-io:2.4"),
	@Grab("com.github.pyrolistical:best-champion-region-services:1-SNAPSHOT")
])

import org.apache.commons.io.*
import com.github.best.champion.region.service.*

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
	champions(templateDirectory, outputDirectory)
	top(templateDirectory, outputDirectory)
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

def champions(templateDirectory, outputDirectory) {
	def classloader = new GroovyClassLoader()
	def shell = new GroovyShell(classloader, getBinding())
	shell.run(new File("generate_summoner_ratings_page.groovy"), [
			templateDirectory,
			outputDirectory
	] as String[])
}

def top(templateDirectory, outputDirectory) {
	run(new File("generate_top_page.groovy"), [
			templateDirectory,
			outputDirectory
	] as String[])
}
