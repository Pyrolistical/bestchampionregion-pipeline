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

def ordering = args[0]
if (!ordering) {
	throw new IllegalArgumentException("required ordering parameter")
}
if (!Constants.orderings[ordering]) {
	throw new IllegalArgumentException("No such ordering named $ordering")
}
ordering = Constants.orderings.find {
	it.key == ordering
}

def champion = args[1]
if (!champion) {
	throw new IllegalArgumentException("required champion parameter")
}
if (!Constants.champions[champion]) {
	throw new IllegalArgumentException("No such champion named $champion")
}
champion = Constants.champions.find {
	it.key == champion
}

def templateDirectory = args[2]
if (!templateDirectory) {
	throw new IllegalArgumentException("required templateDirectory parameter")
}

def outputDirectory = args[3]
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
fileTemplateResolver.setPrefix("$templateDirectory/ordering/champion/region/season/")
fileTemplateResolver.setSuffix(".html")
templateEngine.setTemplateResolver(fileTemplateResolver)

def context = new Context()
def model = context.variables

model["active"] = [
	ordering: ordering,
	champion: champion,
	region: region,
	season: season
]

model["orderings"] = Constants.orderings
model["champions"] = Constants.champions
model["regions"] = Constants.regions
model["seasons"] = Constants.seasons


MongoUtils.connect {
	mongo ->
		def collection = mongo.live."summoner_ratings_${champion.key}"

		def data = []

		def ratingOrder = ordering.key == "best" ? -1 : 1
		collection.find().sort([rating: ratingOrder] as BasicDBObject).limit(100).eachWithIndex {
			row, i ->
				def datum = new HashMap(row)
				datum["rank"] = i + 1
				data.add(datum)
		}

		def lolapi = mongo.live.lolapi
		def summonerIds = data.collect {
			it.summonerId
		}
		def nameBySummonerId = convertSummonerIdToName(lolapi, summonerIds)

		data.each {
			it.name = nameBySummonerId[it.summonerId]
		}

		model["data"] = data.collect {
			it.subMap(["rank", "name", "won", "lost", "rating"])
		}

		def orderingPath = ordering.value.path
		def championPath = champion.value.path
		def regionPath = region.value.path
		def seasonPath = season.value.path
		def outputPath = new File(outputDirectory, "$orderingPath/$championPath/$regionPath/$seasonPath")
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

