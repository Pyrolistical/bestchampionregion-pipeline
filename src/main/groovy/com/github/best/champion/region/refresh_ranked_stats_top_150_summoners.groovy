package com.github.best.champion.region

import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.github.concept.not.found.regulache.Regulache
import com.mongodb.BasicDBObject
import groovyx.net.http.HttpResponseException

import java.util.concurrent.TimeUnit

MongoUtils.connect {
	mongo ->
		def ranked_stats = mongo.season_3.ranked_stats_by_summoner_1p2
		def summoner_ratings = mongo.season_3.summoner_ratings

		def summonerIds = [] as Set
		Champion.each {
			champion ->
				def ratingOrder = -1
				summoner_ratings.find([
						champion: champion.name()
				] as BasicDBObject, [
						summonerId: 1
				] as BasicDBObject).sort([rating: ratingOrder] as BasicDBObject).limit(150).each {
					summonerIds.add(it.summonerId)
				}
		}
		def done = 0
		def total = summonerIds.size()
		def start = System.currentTimeMillis()
		def regulache = new Regulache("http://localhost:30080/", ranked_stats)
		summonerIds.each {
			summonerId ->
				refreshRankedStats(regulache, summonerId)
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

def refreshRankedStats(regulache, summonerId) {
	try {
		def (json, cached) = regulache.executeGet(
				path: "api/lol/{region}/v1.2/stats/by-summoner/{summonerId}/ranked",
				"path-parameters": [
						region: "na",
						summonerId: summonerId as String
				],
				queries: [
						season: "SEASON3"
				],
				"ignore-cache-if-older-than": TimeUnit.DAYS.toMillis(1)
		)
		cached
	} catch (HttpResponseException e) {
		throw new Exception("failed to fetch stats for $summonerId", e)
	}
}

