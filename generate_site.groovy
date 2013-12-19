@Grab("commons-io:commons-io:2.4")

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
	FileUtils.copyFile(
			new File(templateDirectory, "index.html"),
			new File(outputDirectory, "index.html"))
}

def about(templateDirectory, outputDirectory) {
	run(new File("generate-about-page.groovy"), [
			templateDirectory,
			outputDirectory
	] as String[])
}

def champions(templateDirectory, outputDirectory) {
	champions(templateDirectory, outputDirectory, Constants.champions)
}

def champions(templateDirectory, outputDirectory, championList) {
	if (championList.empty) {
		return
	}
	def doOver = []
	championList.each {
		champion ->
			def classloader = new GroovyClassLoader()
			def shell = new GroovyShell(classloader, getBinding())
			try {
				println("start $champion.value.name")
				shell.run(new File("generate_summoner_ratings_page.groovy"), [
						"best",
						champion.key,
						templateDirectory,
						outputDirectory
				] as String[])
				println("done $champion.value.name")
			} catch (DoOverException e) {
				println("doing over: ${e.message}")
				doOver.add(champion)
			}
	}
	champions(templateDirectory, outputDirectory, doOver)
}

def top(templateDirectory, outputDirectory) {
	run(new File("generate_top_page.groovy"), [
			templateDirectory,
			outputDirectory
	] as String[])
}
