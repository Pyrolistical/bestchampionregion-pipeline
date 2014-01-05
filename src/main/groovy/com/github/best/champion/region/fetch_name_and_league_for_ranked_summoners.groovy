package com.github.best.champion.region

import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.github.concept.not.found.regulache.Regulache
import com.mongodb.BasicDBObject


MongoUtils.connect {
	mongo ->
		def regulache = new Regulache("http://localhost:30080/", mongo.live.league_by_summoner_2p2)
		def summonerIds = mongo.live.ranked_summoners.find([
				leaguePoints: ['$exists': false]
		] as BasicDBObject, [
				_id: 1
		] as BasicDBObject).limit(100_000).collect {
			it._id
		} as Set

		def done = [] as Set
		def bonus = [] as Set
		def total = summonerIds.size()
		def start = System.currentTimeMillis()
		def summonerService = new SummonerService(mongo)
		summonerIds.each {
			summonerId ->
				def (json, cached) = regulache.executeGet(
						path: "api/lol/{region}/v2.2/league/by-summoner/{summonerId}",
						"path-parameters": [
								region: "na",
								summonerId: summonerId as String
						]
				)

				if (json == null || json."$summonerId" == null) {
					done.add(summonerId)
					return
				}
				def previousPercentage = 100 * done.size() / total as int
				json."$summonerId".entries.each {
					leagueEntry ->
						def foundSummonerId = leagueEntry.playerOrTeamId as int
						def summonerName = leagueEntry.playerOrTeamName
						def tier = leagueEntry.tier
						def rank =  leagueEntry.rank
						def leaguePoints = leagueEntry.leaguePoints
						summonerService.updateSummoner(foundSummonerId, summonerName, League.getLeague(tier, rank), leaguePoints)
						if (summonerIds.contains(foundSummonerId)) {
							done.add(foundSummonerId)
						} else {
							bonus.add(foundSummonerId)
						}
				}

				def currentPercentage = 100 * done.size() / total as int
				if (previousPercentage != currentPercentage && currentPercentage % 1 == 0) {
					def timeRemaining = (System.currentTimeMillis() - start) * (total - done.size()) / done.size() as int
					def hours = timeRemaining / (1000 * 60 * 60) as int
					def minutes = (timeRemaining / (1000 * 60) as int) % 60
					def seconds = (timeRemaining / 1000 as int) % 60
					def duration = String.format("%02d:%02d:%02d", hours, minutes, seconds)
					println("done $currentPercentage% ${done.size()}/$total - remaining $duration")
				}
		}
		def bonusPercentage = 100 * bonus.size()/done.size() as int
		println("bonus of $bonusPercentage% ${bonus.size()}/${done.size()}")
}
