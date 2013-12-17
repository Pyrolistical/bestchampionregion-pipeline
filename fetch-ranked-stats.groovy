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

		def regulache = new Regulache("https://prod.api.pvp.net/", lolapi)
		lolapi.find([path : "api/lol/{region}/v1.1/summoner/by-name/{name}"] as BasicDBObject).each {
			def summonerId = it.data.id
			fetchRankedStats(regulache, summonerId)
			println("done $summonerId")
			sleep(1000)
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

