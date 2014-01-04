package com.github.best.champion.region

import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.mongodb.BasicDBObject

MongoUtils.connect {
	mongo ->
		def ranked_stats = mongo.live.ranked_stats_by_summoner_1p2

		def summoner_ratings = mongo.live.summoner_ratings

		summoner_ratings.ensureIndex([
				champion: 1,
				summonerId: 1
		] as BasicDBObject, [
				unique: true
		] as BasicDBObject)

		summoner_ratings.ensureIndex([
				champion: 1,
				rating: -1
		] as BasicDBObject)

		def done = 0

		def lastRun = mongo.live.process.findOne([
				name: "update summoner ratings"
		] as BasicDBObject)."last-run"

		def total = ranked_stats.count([
				data: ['$ne': null],
				"last-retrieved": ['$gt': lastRun]
		] as BasicDBObject)

		def start = System.currentTimeMillis()

		ranked_stats.find([
				data: ['$ne': null],
				"last-retrieved": ['$gt': lastRun]
		] as BasicDBObject, [
				"data.summonerId": 1,
				"data.champions.name": 1,
				"data.champions.stats.totalSessionsWon": 1,
				"data.champions.stats.totalSessionsLost": 1
		] as BasicDBObject).each {
			rankedStat ->
				def summonerId = rankedStat.data.summonerId
				rankedStat.data.champions.each {
					champion ->
						if (champion.name == "Combined") {
							return
						}
						def won = champion.stats.totalSessionsWon
						def lost = champion.stats.totalSessionsLost
						def rating = won - lost
						def row = [
								summonerId: summonerId,
								champion: champion.name,
								won: won,
								lost: lost,
								rating: rating
						]
						summoner_ratings.update(
								[summonerId: summonerId, champion: champion.name] as BasicDBObject,
								row as BasicDBObject,
								true,
								false
						)
				}
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
		mongo.live.process.update([
				name: "update summoner ratings"
		] as BasicDBObject, [
				name: "update summoner ratings",
				"last-run": start
		] as BasicDBObject,
				true,
				false)
}

