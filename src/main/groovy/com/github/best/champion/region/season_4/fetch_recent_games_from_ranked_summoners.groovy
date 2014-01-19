package com.github.best.champion.region.season_4

import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.github.concept.not.found.regulache.Regulache
import com.mongodb.BasicDBObject
import groovy.time.TimeDuration
import groovyx.net.http.HttpResponseException

SIX_HOURS = new TimeDuration(6, 0, 0, 0)

MongoUtils.connect {
	mongo ->
		mongo.season_4.ranked_summoners.ensureIndex([
				active: 1,
				league: 1,
				"recent-games-last-retrieved": 1
		] as BasicDBObject)

		def summonerIds = mongo.season_4.ranked_summoners.find([
				'$and': [[
				        active: true
				],[
						'$or': [[
								league: "challenger"
						], [
								league: [
										'$regex': ~/^diamond-\d$/
								]
						]]
				], [
						'$or': [[
								"recent-games-last-retrieved": [
										'$lt': SIX_HOURS.ago.time
								]
						], [
								"recent-games-last-retrieved": [
										'$exists': false
								]
						]]
				]]
		] as BasicDBObject).collect {
			it._id
		} as Set

		def regulache = new Regulache("http://localhost:30080/", mongo.season_4.recent_games_by_summoner_1p3)
		def done = 0
		def total = summonerIds.size()

		def start = System.currentTimeMillis()
		summonerIds.each {
			summonerId ->
				def previousPercentage = 100 * done / total as int

				fetchRecentGames(regulache, summonerId)
				updateTimestamp(mongo, summonerId)

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

def fetchRecentGames(regulache, summonerId) {
	try {
		def (json, cached) = regulache.executeGet(
				path: "api/lol/{region}/v1.2/game/by-summoner/{summonerId}/recent",
				"path-parameters": [
						region: "na",
						summonerId: summonerId as String
				],
				"ignore-cache-if-older-than": SIX_HOURS.ago.time
		)
		cached
	} catch (HttpResponseException e) {
		throw new Exception("failed to recent games for $summonerId", e)
	}
}

def updateTimestamp(mongo, summonerId) {
	def entry = [
			[
					'$set': [
							"recent-games-last-retrieved": System.currentTimeMillis()
					]
			]
	]
	mongo.season_4.ranked_summoners.update(
			[_id: summonerId] as BasicDBObject,
			entry as BasicDBObject,
			true,
			false
	)
}
