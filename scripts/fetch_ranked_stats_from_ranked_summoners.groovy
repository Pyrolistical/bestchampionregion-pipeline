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

def start = System.currentTimeMillis()
MongoUtils.connect {
	mongo ->
		def ranked_summoners = mongo.live.ranked_summoners
		def ranked_stats = mongo.live.ranked_stats_by_summoner_1p2

		def n = 0
		println("connected ${(System.currentTimeMillis() - start)/1000d}")
		def summonerIds = ranked_summoners.find(
		).collect {
			it._id
		} as Set
		println("ranked summoners ${(System.currentTimeMillis() - start)/1000d}")

		def finishedSummonerIds = ranked_stats.find([] as BasicDBObject, ["data.summonerId": 1] as BasicDBObject).collect {
			it.data.summonerId
		} as Set
		println("finished summoners ${(System.currentTimeMillis() - start)/1000d}")
		summonerIds.removeAll(finishedSummonerIds)
		println("remaining summoners ${(System.currentTimeMillis() - start)/1000d}")

		def regulache = new Regulache("http://localhost:30080/", ranked_stats)
		summonerIds.each {
			summonerId ->
				def cached = fetchRankedStats(regulache, summonerId)
				if (!cached) {
					println("${++n}/${summonerIds.size()} done $summonerId")
				} else {
					println("already did $summonerId, skipped")
				}
		}
		println("done ${(System.currentTimeMillis() - start)/1000d}")
}

def fetchRankedStats(regulache, summonerId) {
	try {
		def (json, cached) = regulache.executeGet(
				path: "api/lol/{region}/v1.2/stats/by-summoner/{summonerId}/ranked",
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
		throw new Exception("failed to fetch stats for $summonerId", e)
	}
}

