package com.github.best.champion.region

import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templateresolver.FileTemplateResolver

def templateDirectory = args[0]
if (!templateDirectory) {
	throw new IllegalArgumentException("required templateDirectory parameter")
}

def outputDirectory = args[1]
if (!outputDirectory) {
	throw new IllegalArgumentException("required outputDirectory parameter")
}

def templateEngine = new TemplateEngine()
def fileTemplateResolver = new FileTemplateResolver()
fileTemplateResolver.setPrefix("$templateDirectory/summoner/region/")
fileTemplateResolver.setSuffix(".html")
templateEngine.setTemplateResolver(fileTemplateResolver)

def context = new Context()
def model = context.variables

def region = Constants.regions.find {
	it.key == "NA"
}
def season = Constants.seasons.find {
	it.key == "season3"
}

MongoUtils.connect {
	mongo ->

		model.active = [
				region: region,
				season: season
		]
		model.numberOfSummoners = mongo.live.ranked_summoners.count()
		model.numberOfGames = mongo.live.ranked_games.count()

		def outputPath = new File(outputDirectory, "summoner/${region.value.path}")
		outputPath.mkdirs()
		new File(outputPath, "index.html").withWriter {
			templateEngine.process("index", context, it)
		}
}
