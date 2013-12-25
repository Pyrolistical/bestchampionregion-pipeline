package com.github.best.champion.region.service

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
			def missingNames = getNames(missing)
			def missingLeagues = getLeagues(missing)

			missing.each {
				updateSummoner(it, missingNames[it], missingLeagues[it])
			}
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

	def getNames(summonerIds) {
		def result = [:]
		summonerIds.collate(40).each {
			summonerIdsChunk ->
				try {

					def response = directApi.get(
							path: "api/lol/na/v1.2/summoner/${summonerIdsChunk.join(",")}/name"
					)
					response.data.summoners.each {
						result[it.id] = it.name
					}
					println("fetch ${summonerIdsChunk.size()} summoner names")
				} catch (HttpResponseException e) {
					throw new Exception("failed to resolve names for $summonerIdsChunk", e)
				}
		}
		result
	}

	def getLeagues(summonerIds) {
		def result = [:]
		summonerIds.each {
			result[it] = getLeague(it)
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
			def entry = json."$summonerId".entries.find {
				it.playerOrTeamId == summonerId as String
			}
			if (entry != null) {
				rank = entry.rank
			}

			def league = Constants.leagues.find {
				def name
				if (it.key == "challenger") {
					name = tier
				} else {
					name = "$tier $rank"
				}
				it.value.name.equalsIgnoreCase(name)
			}

			if (!cached) {
				println("$summonerId is ${league.value.name}")
			}
			return league.key
		} catch (HttpResponseException e) {
			throw new Exception("failed to get league for $summonerId", e)
		}
	}

	def updateSummoner(summonerId, name, league) {
		def entry = [
				[
						'$set': [
								name: name,
								"name-last-retrieved": System.currentTimeMillis(),
								league: league
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
