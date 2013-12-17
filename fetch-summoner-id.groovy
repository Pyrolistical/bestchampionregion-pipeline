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
		def filename = args[0]
		new File(filename).eachLine {
			def name = it.replace(" ", "")
			def id = findByName(regulache, name)
			println("$name ==> $id")
			sleep(1000)
		}
}

def findByName(regulache, name) {
	try {
		def (json, cached) = regulache.executeGet(
				path: "/api/lol/{region}/v1.1/summoner/by-name/{name}",
				"path-parameters": [
						region: "na",
						name: name
				],
				"transient-queries": [
						api_key: Api.key()
				]
		)
		json.id
	} catch (HttpResponseException e) {
		println("failed to fetch $name")
		e.printStackTrace()
		null
	}
}

