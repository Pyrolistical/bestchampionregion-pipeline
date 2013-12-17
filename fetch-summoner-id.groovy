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

