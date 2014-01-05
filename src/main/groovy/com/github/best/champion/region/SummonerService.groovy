package com.github.best.champion.region

import com.mongodb.*
import com.github.concept.not.found.regulache.Regulache
import groovyx.net.http.*

def class SummonerService {

	def directApi = new RESTClient("http://localhost:30080/")
	def regulache
	def summonerCollection

	def SummonerService(mongo) {
		summonerCollection = mongo.live.ranked_summoners
		regulache = new Regulache("http://localhost:30080/", mongo.live.league_by_summoner_2p2)
	}

	def getSummonersByIds(summonerIds) {
		def summoners = [:]
		def existing = summonerCollection.find([
				_id: ['$in': summonerIds],
				name: ['$exists': true],
				league: ['$exists': true]
		] as BasicDBObject, [
				_id: 1
		]  as BasicDBObject).collect {
			it._id
		}
		def missing = summonerIds - existing

		if (!missing.empty) {
			getNameAndLeague(missing)
		}

		summonerCollection.find([_id: ['$in': summonerIds]] as BasicDBObject).each {
			def league = Constants.leagues.find {
				league ->
					league.key == it.league
			}
			summoners[it._id] = [
					name: it.name,
					league: league
			]
		}
		summoners
	}

	def getNameAndLeague(summonerIds) {
		def missingNames = getNames(summonerIds)
		def missingLeagues = getLeagues(summonerIds)

		summonerIds.each {
			def league = missingLeagues[it][0]
			def leaguePoints = missingLeagues[it][1]
			updateSummoner(it, missingNames[it], league, leaguePoints)
		}
	}

	def getNames(summonerIds) {
		def result = [:]
		def start = System.currentTimeMillis()
		def done = 0
		def total = summonerIds.size()
		summonerIds.collate(40).each {
			summonerIdsChunk ->
				try {

					def response = directApi.get(
							path: "api/lol/na/v1.2/summoner/${summonerIdsChunk.join(",")}/name"
					)
					response.data.summoners.each {
						result[it.id] = it.name
					}

					def previousPercentage = 100 * done / total as int
					done += 40
					def currentPercentage = 100 * done / total as int
					if (previousPercentage != currentPercentage && currentPercentage % 5 == 0) {
						def timeRemaining = (System.currentTimeMillis() - start) * (total - done) / done as int
						def hours = timeRemaining / (1000 * 60 * 60) as int
						def minutes = (timeRemaining / (1000 * 60) as int) % 60
						def seconds = (timeRemaining / 1000 as int) % 60
						def duration = String.format("%02d:%02d:%02d", hours, minutes, seconds)
						println("names: done $currentPercentage% $done/$total - remaining $duration")
					}
				} catch (HttpResponseException e) {
					throw new Exception("failed to resolve names for $summonerIdsChunk", e)
				}
		}
		result
	}

	def getLeagues(summonerIds) {
		def result = [:]
		def start = System.currentTimeMillis()
		def done = 0
		def total = summonerIds.size()
		summonerIds.each {
			result[it] = getLeague(it)

			def previousPercentage = 100 * done / total as int
			done++
			def currentPercentage = 100 * done / total as int
			if (previousPercentage != currentPercentage && currentPercentage % 5 == 0) {
				def timeRemaining = (System.currentTimeMillis() - start) * (total - done) / done as int
				def hours = timeRemaining / (1000 * 60 * 60) as int
				def minutes = (timeRemaining / (1000 * 60) as int) % 60
				def seconds = (timeRemaining / 1000 as int) % 60
				def duration = String.format("%02d:%02d:%02d", hours, minutes, seconds)
				println("leagues: done $currentPercentage% $done/$total - remaining $duration")
			}
		}
		result
	}

	def getLeague(summonerId) {
		try {
			def (json, cached) = regulache.executeGet(
					path: "api/lol/{region}/v2.2/league/by-summoner/{summonerId}",
					"path-parameters": [
							region: "na",
							summonerId: summonerId as String
					]
			)

			if (json == null || json."$summonerId" == null) {
				println("could not find league for $summonerId, defaulting to Bronze IV")
				return "bronze-5"
			}

			def tier = json."$summonerId".tier

			// default to V because can't fetch rank for some summoners due to ranked decay
			def rank = "V"
			def leaguePoints = 0
			def entry = json."$summonerId".entries.find {
				it.playerOrTeamId == summonerId as String
			}
			if (entry != null) {
				rank = entry.rank
				leaguePoints = entry.leaguePoints
			}

			def league = League.getLeague(tier, rank)

			return [league, leaguePoints]
		} catch (HttpResponseException e) {
			throw new Exception("failed to get league for $summonerId", e)
		}
	}

	def updateSummoner(summonerId, name, league, leaguePoints) {
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
		summonerCollection.update(
				[_id: summonerId] as BasicDBObject,
				entry as BasicDBObject,
				true,
				false
		)
	}
}
