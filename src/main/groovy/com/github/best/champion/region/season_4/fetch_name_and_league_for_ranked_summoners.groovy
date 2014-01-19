package com.github.best.champion.region.season_4

import com.github.best.champion.region.League
import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.github.concept.not.found.regulache.Regulache
import com.mongodb.BasicDBObject
import groovy.time.TimeDuration

SIX_HOURS = new TimeDuration(6, 0, 0, 0)

MongoUtils.connect {
	mongo ->
		def regulache = new Regulache("http://localhost:30080/", mongo.season_4.league_by_summoner_2p2)
		def summonerIds = mongo.season_4.ranked_summoners.find([
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
										'$lt': SIX_HOURS.ago.time
								]
						], [
								"league-last-retrieved": [
										'$exists': false
								]
						]]
				]]
		] as BasicDBObject, [
				_id: 1
		] as BasicDBObject).limit(100_000).collect {
			it._id
		} as Set

		def done = [] as Set
		def bonus = [] as Set
		def saved = 0
		def total = summonerIds.size()
		def start = System.currentTimeMillis()
		summonerIds.each {
			summonerId ->
				def previousPercentage = 100 * done.size() / total as int
				try {
					if (done.contains(summonerId)) {
						saved++
						return
					}
					def (json, cached) = regulache.executeGet(
							path: "api/lol/{region}/v2.2/league/by-summoner/{summonerId}",
							"path-parameters": [
									region: "na",
									summonerId: summonerId as String
							],
							"ignore-cache-if-older-than": SIX_HOURS.ago.time
					)

					if (json == null || json."$summonerId" == null) {
						inactiveSummoner(mongo, summonerId)
						done.add(summonerId)
						return
					}

					json."$summonerId".entries.each {
						leagueEntry ->
							def foundSummonerId = leagueEntry.playerOrTeamId as int
							def summonerName = leagueEntry.playerOrTeamName
							def tier = leagueEntry.tier
							def rank = leagueEntry.rank
							def leaguePoints = leagueEntry.leaguePoints
							updateSummoner(mongo, foundSummonerId, summonerName, League.getLeague(tier, rank), leaguePoints)
							if (summonerIds.contains(foundSummonerId)) {
								done.add(foundSummonerId)
							} else {
								bonus.add(foundSummonerId)
							}
					}
					if (!done.contains(summonerId)) {
						inactiveSummoner(mongo, summonerId)
						done.add(summonerId)
						return
					}

				} finally {
					def currentPercentage = 100 * done.size() / total as int
					if (previousPercentage != currentPercentage && currentPercentage % 1 == 0) {
						def timeRemaining = (System.currentTimeMillis() - start) * (total - done.size()) / done.size() as int
						def hours = timeRemaining / (1000 * 60 * 60) as int
						def minutes = (timeRemaining / (1000 * 60) as int) % 60
						def seconds = (timeRemaining / 1000 as int) % 60
						def duration = String.format("%02d:%02d:%02d", hours, minutes, seconds)
						println("done $currentPercentage% ${done.size()}/$total - remaining $duration saved thus far $saved")
					}
				}
		}
		if (done.size() > 0) {
			def bonusPercentage = 100 * bonus.size() / done.size() as int
			println("bonus of $bonusPercentage% ${bonus.size()}/${done.size()}")
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