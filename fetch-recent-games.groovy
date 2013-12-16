@Grapes([
	@Grab(group="org.mongodb", module="mongo-java-driver", version="2.9.3"),
	@Grab(group="com.github.concept-not-found", module="regulache", version="1-SNAPSHOT"),
	@Grab(group="org.codehaus.groovy.modules.http-builder", module="http-builder", version="0.6")
])
import com.mongodb.*
import com.github.concept.not.found.regulache.Regulache
import groovyx.net.http.HttpResponseException

def mongo = new Mongo()

try {
	def db = mongo.getDB("live")
	def lolapi = db.getCollection("lolapi")

	def regulache = new Regulache("https://prod.api.pvp.net/", lolapi)
	lolapi.find([path: "api/lol/{region}/v1.1/stats/by-summoner/{summonerId}/ranked"] as BasicDBObject, ["data.summonerId": 1]  as BasicDBObject).each {
		def summonerId = it.data.summonerId
		def cached = fetch(regulache, summonerId)
		if (!cached) {
			println("done $summonerId")
			sleep(1200)
		} else {
			println("already have $summonerId, skipped")
		}
	}
} finally {
	mongo.close()
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

