package com.github.best.champion.region

import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.mongodb.BasicDBObject

def rankedSubTypes = [
		"RANKED_SOLO_5x5",
		"RANKED_TEAM_5x5"
]

MongoUtils.connect {
	mongo ->
		def recent_games = mongo.live.recent_games_by_summoner_1p2
		def ranked_games = mongo.live.ranked_games

		ranked_games.ensureIndex([
				summonerId: 1,
				gameId: 1
		] as BasicDBObject, [
				unique: true
		] as BasicDBObject)

		def done = 0
		def total = recent_games.count([
				"data.games.level": 30,
				"data.games.subType": [
						'$in': rankedSubTypes
				]
		] as BasicDBObject)
		def start = System.currentTimeMillis()
		recent_games.find([
				"data.games.level": 30,
				"data.games.subType": [
						'$in': rankedSubTypes
				]
		] as BasicDBObject).each {
			def summonerId = it.data.summonerId
			it.data.games.each {
				game ->
					if (!rankedSubTypes.contains(game.subType)) {
						return
					}

					game.summonerId = summonerId
					ranked_games.update(
							[
									summonerId: summonerId,
									gameId: game.gameId
							] as BasicDBObject,
							game as BasicDBObject,
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
}
