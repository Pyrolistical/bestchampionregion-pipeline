package com.github.best.champion.region.season_4

import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.mongodb.BasicDBObject

def rankedSubTypes = [
		"RANKED_SOLO_5x5",
		"RANKED_TEAM_5x5"
]

MongoUtils.connect {
	mongo ->
		mongo.season_4.ranked_games.ensureIndex([
				summonerId: 1,
				gameId: 1
		] as BasicDBObject, [
				unique: true
		] as BasicDBObject)

		mongo.season_4.recent_games_by_summoner_1p3.ensureIndex([
				"data.games.level": 1,
				"data.games.subType": 1,
				"ranked-games-extracted": 1
		] as BasicDBObject)

		def done = 0
		def total = mongo.season_4.recent_games_by_summoner_1p3.count([
				"data.games.level": 30,
				"data.games.subType": [
						'$in': rankedSubTypes
				],
				"ranked-games-extracted": [
						'$exists': false
				]
		] as BasicDBObject)
		def start = System.currentTimeMillis()
		mongo.season_4.recent_games_by_summoner_1p3.find([
				"data.games.level": 30,
				"data.games.subType": [
						'$in': rankedSubTypes
				],
				,
				"ranked-games-extracted": [
						'$exists': false
				]
		] as BasicDBObject).each {
			def previousPercentage = 100 * done / total as int
			try {
				def summonerId = it.data.summonerId
				it.data.games.each {
					game ->
						game.summonerId = summonerId
						mongo.season_4.ranked_games.update(
								[
										summonerId: summonerId,
										gameId: game.gameId
								] as BasicDBObject,
								game as BasicDBObject,
								true,
								false
						)
				}
				mongo.season_4.recent_games_by_summoner_1p3.update(
						[
								_id: it._id
						] as BasicDBObject,
						[
								'$set': [
										"ranked-games-extracted": true
								]
						] as BasicDBObject,
						true,
						false
				)

				done++
			} finally {
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
}
