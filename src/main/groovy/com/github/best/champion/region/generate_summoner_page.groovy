package com.github.best.champion.region

import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.mongodb.BasicDBObject
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templateresolver.FileTemplateResolver

def templateDirectory = "../../concept-not-found/bestchampionregion/template"

def outputDirectory = "../../concept-not-found/bestchampionregion-pages"

def region = Constants.regions.find {
	it.key == "NA"
}

def season = Constants.seasons.find {
	it.key == "season3"
}

MongoUtils.connect {
	mongo ->
		mongo.live.ranked_summoners.ensureIndex([
				active: 1,
				"page-last-generated": 1
		] as BasicDBObject)

		def leagueCount = mongo.live.command([
				aggregate: "ranked_summoners",
				pipeline: [
						[
								'$match': [
										active: true
								]
						],
						[
								'$group': [
										_id: '$league',
										count: [
												'$sum': 1
										]
								]
						]
				]
		] as BasicDBObject).result

		def totalRankedPlayers = leagueCount.collect {
			it.count
		}.sum()

		def summonerIds = mongo.live.ranked_summoners.find([
				active: true,
				"page-last-generated": [
						'$exists': false
				]
		] as BasicDBObject, [
				_id: 1
		] as BasicDBObject).limit(100_000).collect {
			it._id
		}

		def done = 0
		def total = summonerIds.size()
		def start = System.currentTimeMillis()
		summonerIds.each {
			summonerId ->
				def templateEngine = new TemplateEngine()
				def fileTemplateResolver = new FileTemplateResolver()
				fileTemplateResolver.setPrefix("$templateDirectory/summoner/region/name/")
				fileTemplateResolver.setSuffix(".html")
				templateEngine.setTemplateResolver(fileTemplateResolver)

				def context = new Context()
				def model = context.variables

				model["active"] = [
						region: region,
						season: season
				]

				def summoner = mongo.live.ranked_summoners.findOne([
						_id: summonerId
				] as BasicDBObject)

				model.name = summoner.name
				model.region = "NA"
				model.league = League.getLeagueByPath(summoner.league)
				model.leaguePoints = summoner.leaguePoints

				def rank = 0
				League.any {
					league ->
						if (model.league == league) {
							return true
						}

						rank += leagueCount.find {
							it._id == league.path
						}.count
						false
				}
				rank += mongo.live.ranked_summoners.count([
						active: true,
						league: model.league.path,
						leaguePoints: [
								'$gt': model.leaguePoints
						]
				] as BasicDBObject)
				rank++

				model.rank = rank

				def percentage = 100 * rank / totalRankedPlayers
				model.percentage = printSignificantFigures(percentage, 3)

				def uriSafeName = URLEncoder.encode(model.name, "UTF-8")
				def outputPath = new File(outputDirectory, "summoner/${region.value.path}/$uriSafeName")
				outputPath.mkdirs()
				new File(outputPath, "index.html").withWriter {
					templateEngine.process("index", context, it)
				}

				mongo.live.ranked_summoners.update(
						[_id: summonerId] as BasicDBObject,
						[
								[
										'$set': [
												"page-last-generated": System.currentTimeMillis()
										]
								]
						] as BasicDBObject,
						true,
						false
				)

				def previousPercentage = 100 * done / total as int
				done++
				def currentPercentage = 100 * done / total as int
				if (previousPercentage != currentPercentage && currentPercentage % 5 == 0) {
					def timeRemaining = (System.currentTimeMillis() - start) * (total - done) / done as int
					def hours = timeRemaining / (1000 * 60 * 60) as int
					def minutes = (timeRemaining / (1000 * 60) as int) % 60
					def seconds = (timeRemaining / 1000 as int) % 60
					def duration = String.format("%02d:%02d:%02d", hours, minutes, seconds)
					println("done $currentPercentage% $done/$total - remaining $duration")
				}
		}
}

def static printSignificantFigures(num, int n) {
	def rawString = new BigDecimal(num).toString()
	def started = false
	def end = -1
	for (int i = 0; i < rawString.length(); i++) {
		if (n == 0) {
			end = i
			break
		}

		def digit = rawString[i]
		if (digit == '.') {
			continue
		}
		if (!started && digit == '0') {
			continue
		}
		if (!started) {
			started = true
		}
		n--
	}
	if (end == -1) {
		end = rawString.length()
	}
	rawString.substring(0, end)
}