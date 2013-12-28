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
		def ranked_summoners = mongo.live.ranked_summoners
		def ranked_stats = mongo.live.ranked_stats_by_summoner_1p2

		def done = 0
		def summonerIds = ranked_summoners.find(
		).collect {
			it._id
		} as Set

		def finishedSummonerIds = ranked_stats.find([
				data: [
						'$ne': null
				]
		] as BasicDBObject, [
				"data.summonerId": 1
		] as BasicDBObject).collect {
			it.data.summonerId
		} as Set
		summonerIds.removeAll(finishedSummonerIds)
		def total = summonerIds.size()
		def start = System.currentTimeMillis()
		def regulache = new Regulache("http://localhost:30080/", ranked_stats)
		summonerIds.each {
			summonerId ->
				fetchRankedStats(regulache, summonerId)
				def previousPercentage = 100*done/total as int
				done++
				def currentPercentage = 100*done/total as int
				if (previousPercentage != currentPercentage && currentPercentage % 1 == 0) {
					def timeRemaining = (System.currentTimeMillis() - start)*(total - done)/done as int
					def hours = timeRemaining / (1000 * 60 * 60) as int
					def minutes = (timeRemaining / (1000 * 60) as int) % 60
					def seconds = (timeRemaining / 1000 as int) % 60
					def duration = String.format("%02d:%02d:%02d", hours, minutes, seconds)
					println("done $currentPercentage% $done/$total - remaining $duration")
				}
		}
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
		cached
	} catch (HttpResponseException e) {
		throw new Exception("failed to fetch stats for $summonerId", e)
	}
}

