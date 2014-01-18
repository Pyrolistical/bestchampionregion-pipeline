package com.github.best.champion.region.season_3

import com.github.best.champion.region.Champion
import com.github.best.champion.region.Constants
import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.mongodb.BasicDBObject
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

def region = Constants.regions.find {
	it.key == "NA"
}

def season = Constants.seasons.find {
	it.key == "season3"
}

def templateEngine = new TemplateEngine()
def fileTemplateResolver = new FileTemplateResolver()
fileTemplateResolver.setPrefix("$templateDirectory/top/summoner/region/season/")
fileTemplateResolver.setSuffix(".html")
templateEngine.setTemplateResolver(fileTemplateResolver)

def context = new Context()
def model = context.variables

MongoUtils.connect {
	mongo ->
		def summonerService = new SummonerService(mongo)
		model["data"] = []
		Champion.each {
			champion ->
				def summoner_ratings = mongo.season_3.summoner_ratings

				model.active = [
						region: region,
						season: season
				]

				def data = [
						champion: champion,
				]
				def table = []

				def ratingOrder = -1
				summoner_ratings.find([
						champion: champion.name()
				] as BasicDBObject).sort([rating: ratingOrder] as BasicDBObject).limit(5).eachWithIndex {
					row, i ->
						def datum = new HashMap(row)
						datum["rank"] = i + 1
						table.add(datum)
				}

				def summonerIds = table.collect {
					it.summonerId
				}
				def summoners = summonerService.getSummonersByIds(summonerIds)

				table.each {
					def summonerId = it.summonerId
					if (summoners[summonerId] == null) {
						throw new IllegalStateException("missing summoner name and league for $summonerId")
					}
					it.name = summoners[summonerId].name
					it.league = summoners[summonerId].league
				}

				data.table = table.collect {
					it.subMap(["rank", "league", "name", "rating"])
				}

				model.data.add(data)
		}
		def outputPath = new File(outputDirectory, "top/summoner/${region.value.path}/${season.value.path}")
		outputPath.mkdirs()
		new File(outputPath, "index.html").withWriter {
			templateEngine.process("index", context, it)
		}
}

