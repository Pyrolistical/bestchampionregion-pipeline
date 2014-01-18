package com.github.best.champion.region.season_3

import com.github.best.champion.region.League
import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.mongodb.BasicDBObject

MongoUtils.connect {
	mongo ->

		def existingSummonerIds = mongo.season_3.league_by_summoner_2p2.find([
				data: ['$ne': null]
		] as BasicDBObject, [
				"path-parameters.summonerId": 1
		] as BasicDBObject).collect {
			it."path-parameters".summonerId as int
		}

		def done = 0
		def total = existingSummonerIds.size()
		def start = System.currentTimeMillis()
		def summonerService = new SummonerService(mongo)
		existingSummonerIds.each {
			summonerId ->
				def summonerIdPath = "data.$summonerId" as String
				def query = [:]
				query[summonerIdPath] = ['$exists': true]
				def projection = [:]
				projection[summonerIdPath] = 1
				mongo.season_3.league_by_summoner_2p2.find(
						query as BasicDBObject,
						projection as BasicDBObject).each {
					league_by_summoner ->
						def league = league_by_summoner.data."$summonerId"
						league.entries.each {
							leagueEntry ->
								def foundSummonerId = leagueEntry.playerOrTeamId as int
								def summonerName = leagueEntry.playerOrTeamName
								def tier = leagueEntry.tier
								def rank =  leagueEntry.rank
								def leaguePoints = leagueEntry.leaguePoints
								summonerService.updateSummoner(foundSummonerId, summonerName, League.getLeague(tier, rank), leaguePoints)
						}
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
