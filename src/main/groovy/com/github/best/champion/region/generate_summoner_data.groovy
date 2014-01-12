package com.github.best.champion.region
import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.mongodb.BasicDBObject

def outputDirectory = "../../concept-not-found/bestchampionregion-data"

def region = Constants.regions.find {
	it.key == "NA"
}

MongoUtils.connect {
	mongo ->
		mongo.live.ranked_summoners.ensureIndex([
				active: 1,
				"pipe-last-generated": 1
		] as BasicDBObject)

		mongo.live.ranked_summoners.ensureIndex([
				active: 1,
				league: 1,
				leaguePoints: 1
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
		def rank = 0
		def leagueRank = [:]
		League.each {
			league ->
				leagueRank[league] = rank

				rank += leagueCount.find {
					it._id == league.path
				}.count
		}

		def totalRankedPlayers = leagueCount.collect {
			it.count
		}.sum()

		def summonerIds = mongo.live.ranked_summoners.find([
				active: true,
				"pipe-last-generated": [
						'$exists': false
				]
		] as BasicDBObject, [
				_id: 1
		] as BasicDBObject).limit(500_000).collect {
			it._id
		}

		def done = 0
		def total = summonerIds.size()
		def start = System.currentTimeMillis()
		summonerIds.each {
			summonerId ->

				def summoner = mongo.live.ranked_summoners.findOne([
						_id: summonerId
				] as BasicDBObject)
				def name = summoner.name
				def league = League.getLeagueByPath(summoner.league)
				def leaguePoints = summoner.leaguePoints

				rank = leagueRank[league]
				rank += mongo.live.ranked_summoners.count([
						active: true,
						league: league.path,
						leaguePoints: [
								'$gt': leaguePoints
						]
				] as BasicDBObject)
				rank++

				def percentage = 100 * rank / totalRankedPlayers
				percentage = printSignificantFigures(percentage, 3)

				def pipeLastGenerated = System.currentTimeMillis()

				def outputPath = new File(outputDirectory, "summoner/${region.value.path}")
				outputPath.mkdirs()
				new File(outputPath, "${name}.pipe").withWriter {
					it << rank
					it << "\n"
					it << percentage
					it << "\n"
					it << league.code
					it << "\n"
					it << leaguePoints
					it << "\n"
					it << pipeLastGenerated
					it << "\n"
				}

				mongo.live.ranked_summoners.update(
						[_id: summonerId] as BasicDBObject,
						[
								[
										'$set': [
												"pipe-last-generated": pipeLastGenerated
										]
								]
						] as BasicDBObject,
						true,
						false
				)

				def previousPercentage = 100 * done / total as int
				done++
				def currentPercentage = 100 * done / total as int
				if (previousPercentage != currentPercentage && currentPercentage % 1 == 0) {
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