package com.github.best.champion.region

import org.apache.commons.io.FileUtils

def templateDirectory = "../../concept-not-found/bestchampionregion/template"
def outputDirectory = "../../concept-not-found/bestchampionregion-pages"

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
	summoner_page(templateDirectory, outputDirectory)
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

def run(scriptName, String... args) {
	def script = Class.forName("com.github.best.champion.region.$scriptName").newInstance()
	script.binding = new Binding(args)
	script.run()
}

def about(templateDirectory, outputDirectory) {
	run("generate_about_page", templateDirectory, outputDirectory)
}

def summoner_page(templateDirectory, outputDirectory) {
	run("generate_summoner_page", templateDirectory, outputDirectory)
}

def summoner_ratings(templateDirectory, outputDirectory) {
	run("generate_summoner_ratings_page", templateDirectory, outputDirectory)
}

def top_summoner(templateDirectory, outputDirectory) {
	run("generate_top_summoner_page", templateDirectory, outputDirectory)
}

def top_champion(templateDirectory, outputDirectory) {
	run("generate_top_champion_page", templateDirectory, outputDirectory)
}
