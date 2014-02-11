package com.github.best.champion.region.season_4

import com.github.best.champion.region.League
import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.github.concept.not.found.regulache.Regulache
import com.mongodb.BasicDBObject
import groovy.time.TimeDuration

def sinceHours = args.length >= 1 ? args[0] as int : 6
timeDuration = new TimeDuration(sinceHours, 0, 0, 0)

MongoUtils.connect {
	mongo ->
		def regulache = new Regulache("http://localhost:30080/", mongo.season_4.league_by_summoner_entry_2p3)
		def summonerIds = mongo.season_4.ranked_summoners.find([
				'$or': [[
						'$and': [[
								'$or': [[
										league: "challenger"
								], [
										league: [
												'$regex': ~/^diamond-\d$/
										]
								]]
						], [
								'$or': [[
										"league-last-retrieved": [
												'$lt': timeDuration.ago.time
										]
								], [
										"league-last-retrieved": [
												'$exists': false
										]
								]]
						]]
				], [
						'$nor': [[
								league: "challenger"
						], [
								league: [
										'$regex': ~/^diamond-\d$/
								]
						]],
						"played-in-high-elo": true
				]]
		] as BasicDBObject, [
				_id: 1
		] as BasicDBObject).limit(100_000).collect {
			it._id
		} as Set

		def done = 0
		def total = summonerIds.size()
		def start = System.currentTimeMillis()
		summonerIds.each {
			summonerId ->
				def previousPercentage = 100 * done / total as int
				try {
					def (json, cached) = regulache.executeGet(
							path: "api/lol/{region}/v2.3/league/by-summoner/{summonerId}/entry",
							"path-parameters": [
									region: "na",
									summonerId: summonerId as String
							],
							"ignore-cache-if-older-than": timeDuration.toMilliseconds()
					)

					if (json == null) {
						inactiveSummoner(mongo, summonerId)
					} else {
						json.findAll {
							it.queueType == "RANKED_SOLO_5x5"
						}.each {
							leagueEntry ->
								def summonerName = leagueEntry.playerOrTeamName
								def tier = leagueEntry.tier
								def rank = leagueEntry.rank
								def leaguePoints = leagueEntry.leaguePoints
								updateSummoner(mongo, summonerId, summonerName, League.getLeague(tier, rank), leaguePoints)
						}
					}
					done++
				} finally {
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
}

def inactiveSummoner(mongo, summonerId) {
	def entry = [
			[
					'$set': [
							league: null,
							"league-last-retrieved": System.currentTimeMillis()
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

def updateSummoner(mongo, summonerId, name, league, leaguePoints) {
	def entry = [
			[
					'$set': [
							name: name,
							"name-last-retrieved": System.currentTimeMillis(),
							league: league.path,
							leaguePoints: leaguePoints,
							"league-last-retrieved": System.currentTimeMillis()
					],
					'$unset': [
							"played-in-high-elo": ""
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