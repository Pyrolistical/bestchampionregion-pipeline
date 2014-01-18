package com.github.best.champion.region.season_3

import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.mongodb.BasicDBObject

MongoUtils.connect {
	mongo ->
		def ranked_games = mongo.season_3.ranked_games
		def ranked_summoners = mongo.season_3.ranked_summoners

		def done = 0
		def total = ranked_games.count()
		def start = System.currentTimeMillis()
		ranked_games.find([
		] as BasicDBObject, [
				"summonerId": 1,
				"fellowPlayers.summonerId": 1
		] as BasicDBObject).each {
			game ->
				upsertSummoner(game.summonerId, ranked_summoners)
				game.fellowPlayers.each {
					fellowPlayer ->
						def summonerId = fellowPlayer.summonerId
						upsertSummoner(summonerId, ranked_summoners)
				}
				def previousPercentage = 100 * done / total as int
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
					'$set': [:]
			] as BasicDBObject,
			true,
			false
	)
}