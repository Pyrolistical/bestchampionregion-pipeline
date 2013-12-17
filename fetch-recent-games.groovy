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


def summonerIds = args

MongoUtils.connect {
	mongo ->
		def lolapi = mongo.live.lolapi

		if (!summonerIds) {
			summonerIds = lolapi.find([path: "api/lol/{region}/v1.1/stats/by-summoner/{summonerId}/ranked"] as BasicDBObject, ["data.summonerId": 1] as BasicDBObject).collect {
				it.data.summonerId
			}
		}

		def regulache = new Regulache("https://prod.api.pvp.net/", lolapi)
		summonerIds.each {
			summonerId ->
				def cached = fetch(regulache, summonerId)
				if (!cached) {
					println("done $summonerId")
					sleep(1200)
				} else {
					println("already have $summonerId, skipped")
				}
		}
}

def fetch(regulache, summonerId) {
	try {
		def (json, cached) = regulache.executeGet(
				path: "/api/lol/{region}/v1.1/game/by-summoner/{summonerId}/recent",
				"path-parameters": [
						region: "na",
						summonerId: summonerId as String
				],
				"transient-queries": [
						api_key: Api.key()
				]
		)
		if (json == null) {
			println("could not get recent games for $summonerId")
		}
		cached
	} catch (HttpResponseException e) {
		println("failed to fetch recent games for $summonerId")
		e.printStackTrace()
	}
}

