@Grab(group = "commons-io", module = "commons-io", version = "2.4")

import org.apache.commons.io.*

def templateDirectory = "/Users/rchen/dev/projects/github.com/concept-not-found/bestchampionregion/template"
def outputDirectory = "/Users/rchen/dev/projects/github.com/concept-not-found/bestchampionregion-pages"

// clear output directory
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

// copy statics
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

// generate pages
run(new File("generate-about-page.groovy"), [
		templateDirectory,
		outputDirectory
] as String[])

Constants.champions.each {
	champion ->
		run(new File("generate_summoner_ratings_page.groovy"), [
				"best",
				champion.key,
				templateDirectory,
				outputDirectory
		] as String[])
		println("Done $champion.value.name")
}

run(new File("generate_top_page.groovy"), [
		templateDirectory,
		outputDirectory
] as String[])
