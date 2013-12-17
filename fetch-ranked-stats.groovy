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

		def regulache = new Regulache("https://prod.api.pvp.net/", lolapi)
		lolapi.find([path : "api/lol/{region}/v1.1/summoner/by-name/{name}"] as BasicDBObject).each {
			def summonerId = it.data.id
			fetchRankedStats(regulache, summonerId)
			println("done $summonerId")
			sleep(1200)
		}
}

def fetchRankedStats(regulache, summonerId) {
	try {
		regulache.executeGet(
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
	} catch (HttpResponseException e) {
		println("failed to fetch stats for $summonerId")
		e.printStackTrace()
	}
}

