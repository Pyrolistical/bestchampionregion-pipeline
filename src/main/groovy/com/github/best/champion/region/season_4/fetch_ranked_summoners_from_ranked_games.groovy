package com.github.best.champion.region.season_4

import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.mongodb.BasicDBObject

MongoUtils.connect {
	mongo ->
		mongo.season_4.ranked_games.ensureIndex([
				"summoners-extracted": 1
		] as BasicDBObject)

		def done = 0
		def total = mongo.season_4.ranked_games.count([
				"summoners-extracted": [
						'$exists': false
				]
		] as BasicDBObject)
		def start = System.currentTimeMillis()
		mongo.season_4.ranked_games.find([
				"summoners-extracted": [
						'$exists': false
				]
		] as BasicDBObject, [
				"summonerId": 1,
				"fellowPlayers.summonerId": 1
		] as BasicDBObject).each {
			game ->
				def previousPercentage = 100 * done / total as int
				upsertSummoner(game.summonerId, mongo.season_4.ranked_summoners)
				game.fellowPlayers.each {
					fellowPlayer ->
						def summonerId = fellowPlayer.summonerId
						upsertSummoner(summonerId, mongo.season_4.ranked_summoners)
				}

				mongo.season_4.ranked_games.update(
						[
								_id: game._id
						] as BasicDBObject,
						[
								'$set': [
										"summoners-extracted": true
								]
						] as BasicDBObject,
						true,
						false
				)

				done++
				def currentPercentage = 100 * done / total as int
				if (previousPercentage != currentPercentage && currentPercentage % 10 == 0) {
					def timeRemaining = (System.currentTimeMillis() - start) * (total - done) / done as int
					def hours = timeRemaining / (1000 * 60 * 60) as int
					def minutes = (timeRemaining / (1000 * 60) as int) % 60
					def seconds = (timeRemaining / 1000 as int) % 60
					def duration = String.format("%02d:%02d:%02d", hours, minutes, seconds)
					println("done $currentPercentage% $done/$total - remaining $duration")
				}
		}
}

def upsertSummoner(summonerId, ranked_summoners) {
	ranked_summoners.update(
			[
					_id: summonerId
			] as BasicDBObject,
			[
					'$set': [
							"played-in-high-elo": true
					]
			] as BasicDBObject,
			true,
			false
	)
}