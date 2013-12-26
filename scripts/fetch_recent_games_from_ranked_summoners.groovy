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
		def recent_games = mongo.live.recent_games_by_summoner_1p2

		def summonerIds = ranked_summoners.find(
		).collect {
			it._id
		} as Set

		def finishedSummonerIds = recent_games.find([
		] as BasicDBObject, [
				"data.summonerId": 1
		] as BasicDBObject).collect {
			it.data.summonerId
		} as Set
		summonerIds.removeAll(finishedSummonerIds)

		def regulache = new Regulache("http://localhost:30080/", recent_games)
		def done = 0
		def total = summonerIds.size()

		def start = System.currentTimeMillis()
		summonerIds.each {
			summonerId ->
				fetchRecentGames(regulache, summonerId)
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

def fetchRecentGames(regulache, summonerId) {
	try {
		def (json, cached) = regulache.executeGet(
				path: "api/lol/{region}/v1.2/game/by-summoner/{summonerId}/recent",
				"path-parameters": [
						region: "na",
						summonerId: summonerId as String
				]
		)
		if (json == null) {
			println("$summonerId doesn't have any recent games")
		}
		cached
	} catch (HttpResponseException e) {
		throw new Exception("failed to recent games for $summonerId", e)
	}
}

