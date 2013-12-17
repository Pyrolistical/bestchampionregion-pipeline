@Grapes([
	@Grab(group="org.mongodb", module="mongo-java-driver", version="2.9.3"),
	@Grab(group="com.github.concept-not-found", module="regulache", version="1-SNAPSHOT"),
	@Grab(group="org.codehaus.groovy.modules.http-builder", module="http-builder", version="0.6"),
	@GrabConfig(systemClassLoader = true)
])
import com.mongodb.*
import com.github.concept.not.found.regulache.Regulache
import groovyx.net.http.HttpResponseException

MongoUtils.connect {
	mongo ->
		def lolapi = mongo.live.lolapi

		def n = 0
		def summonerIds = lolapi.find([path : "api/lol/{region}/v1.1/game/by-summoner/{summonerId}/recent"] as BasicDBObject).collect {
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

		def regulache = new Regulache("https://prod.api.pvp.net/", lolapi)
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
				],
				"transient-queries": [
						api_key: Api.key()
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

