@Grapes([
	@Grab("org.thymeleaf:thymeleaf:2.1.2.RELEASE"),
	@Grab("org.slf4j:slf4j-simple:1.7.5"),
	@Grab("org.mongodb:mongo-java-driver:2.11.3"),
	@Grab("com.github.concept-not-found:regulache:1-SNAPSHOT"),
	@Grab("com.github.concept-not-found:mongo-groovy-extension:1-SNAPSHOT"),
	@Grab("org.codehaus.groovy.modules.http-builder:http-builder:0.6")
])

import com.mongodb.*

import org.thymeleaf.*
import org.thymeleaf.context.*
import org.thymeleaf.templateresolver.*

import com.github.concept.not.found.regulache.Regulache
import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import groovyx.net.http.HttpResponseException

def ordering = Constants.orderings.find {
	it.key == "best"
}

def templateDirectory = args[0]
if (!templateDirectory) {
	throw new IllegalArgumentException("required templateDirectory parameter")
}

def outputDirectory = args[1]
if (!outputDirectory) {
	throw new IllegalArgumentException("required outputDirectory parameter")
}

def region = Constants.regions.find {
	it.key == "NA"
}

def season = Constants.seasons.find {
	it.key == "season3"
}

def templateEngine = new TemplateEngine()
def fileTemplateResolver = new FileTemplateResolver()
fileTemplateResolver.setPrefix("$templateDirectory/top/")
fileTemplateResolver.setSuffix(".html")
templateEngine.setTemplateResolver(fileTemplateResolver)

def context = new Context()
def model = context.variables

MongoUtils.connect {
	mongo ->
		model["data"] = []
		Constants.champions.each {
			champion ->
				def collection = mongo.live."summoner_ratings_${champion.key}"

				def data = [
					ordering: ordering,
					champion: champion,
					region: region,
					season: season
				]
				def table = []

				def ratingOrder = ordering.key == "best" ? -1 : 1
				collection.find().sort([rating: ratingOrder] as BasicDBObject).limit(5).eachWithIndex {
					row, i ->
						def datum = new HashMap(row)
						datum["rank"] = i + 1
						table.add(datum)
				}

				def lolapi = mongo.live.lolapi
				def summonerIds = table.collect {
					it.summonerId
				}
				def nameBySummonerId = convertSummonerIdToName(lolapi, summonerIds)

				table.each {
					it.name = nameBySummonerId[it.summonerId]
				}

				data.table = table.collect {
					it.subMap(["rank", "name", "won", "lost", "rating"])
				}

				model["data"].add(data)
				println("done $champion.value.name")
		}
		def outputPath = new File(outputDirectory, "top")
		outputPath.mkdirs()
		new File(outputPath, "index.html").withWriter {
			templateEngine.process("index", context, it)
		}
}

def convertSummonerIdToName(lolapi, summonerIds) {
	def result = [:]
	def remaining = []
	summonerIds.each {
		summonerId ->
			def existing = lolapi.findOne([path: "api/lol/{region}/v1.1/summoner/by-name/{name}", "data.id": summonerId] as BasicDBObject)
			if (existing) {
				result[summonerId] = existing.data.name
			} else {
				existing = lolapi.findOne([path: "api/lol/{region}/v1.1/summoner/{summonerIds}/name", "data.summoners.id": summonerId] as BasicDBObject)
				if (existing) {
					def name = existing.data.summoners.find {
						it.id == summonerId
					}.name
					result[summonerId] = name
				} else {
					remaining.add(summonerId)
				}
			}
	}
	if (!remaining.empty) {
		def regulache = new Regulache("https://prod.api.pvp.net/", lolapi)
		remaining.collate(40).each {
			remaining40 ->
				try {
					def (json, cached) = regulache.executeGet(
							path: "/api/lol/{region}/v1.1/summoner/{summonerIds}/name",
							"path-parameters": [
									region: "na",
									summonerIds: remaining40.join(",")
							],
							"transient-queries": [
									api_key: Api.key()
							]
					)
					remaining40.each {
						summonerId ->
							def name = json.summoners.find {
								it.id == summonerId
							}.name
							result[summonerId] = name
					}
					println("Resolved an additional ${remaining40.size()} summoner ids")
				} catch (HttpResponseException e) {
					println("failed to resolve names for $remaining40")
					e.printStackTrace()
				}
				sleep(1200)
		}
	}
	result
}

