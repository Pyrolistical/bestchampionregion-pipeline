@Grapes([
	@Grab("org.mongodb:mongo-java-driver:2.11.3"),
	@Grab("com.github.concept-not-found:regulache:1-SNAPSHOT"),
	@Grab("com.github.concept-not-found:mongo-groovy-extension:1-SNAPSHOT"),
	@Grab("org.codehaus.groovy.modules.http-builder:http-builder:0.6")
])
import com.mongodb.*
import com.github.concept.not.found.regulache.Regulache
import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import groovyx.net.http.HttpResponseException

MongoUtils.connect {
	mongo ->
		def lolapi = mongo.live.lolapi

		def n = 0
		def summonerIds = lolapi.find(
				[path : "api/lol/{region}/v1.1/game/by-summoner/{summonerId}/recent"] as BasicDBObject,
				["data.summonerId": 1, "data.games.fellowPlayers.summonerId": 1, "data.games.level": 1] as BasicDBObject
		).collect {
			def level = data.games.collect {
				game ->
					game.level
			}.max()
			if (level != 30) {
				return []
			}
			def ids = [it.data.summonerId]
			it.data.games.each {
				game ->
					game.fellowPlayers.each {
						fellowPlayer ->
							ids.add(fellowPlayer.summonerId)
					}
			}
			ids
		}.flatten().unique(true)

		def regulache = new Regulache("http://localhost:30080/", lolapi)
		summonerIds.each {
			summonerId ->
				def cached = fetchRankedStats(regulache, summonerId)
				if (!cached) {
					println("${++n} done $summonerId")
					sleep(1200)
				} else {
					println("already did $summonerId, skipped")
				}
		}
}

def fetchRankedStats(regulache, summonerId) {
	try {
		def (json, cached) = regulache.executeGet(
				path: "/api/lol/{region}/v1.1/stats/by-summoner/{summonerId}/ranked",
				"path-parameters": [
						region: "na",
						summonerId: summonerId as String
				],
				queries: [
						season: "SEASON3"
				]
		)
		if (json == null) {
			println("$summonerId doesn't play ranked")
		}
		cached
	} catch (HttpResponseException e) {
		println("failed to fetch stats for $summonerId")
		if (e.response.statusLine.statusCode == 503) {
			println("service unavailable for $summonerId")
		} else {
			e.printStackTrace()
		}
	}
}

