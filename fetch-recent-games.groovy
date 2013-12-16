@Grapes([
	@Grab(group="org.mongodb", module="mongo-java-driver", version="2.9.3"),
	@Grab(group="com.github.concept-not-found", module="regulache", version="1-SNAPSHOT"),
	@Grab(group="org.codehaus.groovy.modules.http-builder", module="http-builder", version="0.6")
])
import com.mongodb.*
import com.github.concept.not.found.regulache.Regulache
import groovyx.net.http.HttpResponseException

def getApiKey() {
	def lol_api_key = System.getProperty("lol_api_key") ?: System.getenv("lol_api_key")

	if (!lol_api_key) {
		throw new IllegalArgumentException("missing lol_api_key property")
	}

	lol_api_key
}

def mongo = new Mongo()

try {
	def db = mongo.getDB("live")
	def lolapi = db.getCollection("lolapi")

	def regulache = new Regulache("https://prod.api.pvp.net/", lolapi)
	lolapi.find([path : "api/lol/{region}/v1.1/summoner/by-name/{name}"] as BasicDBObject).each {
		def summonerId = it.data.id
		fetch(regulache, summonerId)
		println("done $summonerId")
		sleep(1000)
	}
} finally {
	mongo.close()
}

def fetch(regulache, summonerId) {
	try {
		regulache.executeGet(
				path: "/api/lol/{region}/v1.1/game/by-summoner/{summonerId}/recent",
				"path-parameters": [
						region: "na",
						summonerId: summonerId as String
				],
				"transient-queries": [
						api_key: getApiKey()
				]
		)
	} catch (HttpResponseException e) {
		println("failed to fetch stats for $summonerId")
		e.printStackTrace()
	}
}

