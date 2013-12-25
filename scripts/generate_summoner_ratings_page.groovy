@Grapes([
	@Grab("org.thymeleaf:thymeleaf:2.1.2.RELEASE"),
	@Grab("org.slf4j:slf4j-simple:1.7.5"),
	@Grab("org.mongodb:mongo-java-driver:2.11.3"),
	@Grab("com.github.concept-not-found:mongo-groovy-extension:1-SNAPSHOT"),
	@Grab("com.github.pyrolistical:best-champion-region-services:1-SNAPSHOT")
])

import com.mongodb.*
import com.github.best.champion.region.service.*

import org.thymeleaf.*
import org.thymeleaf.context.*
import org.thymeleaf.templateresolver.*

import com.github.concept.not.found.mongo.groovy.util.MongoUtils

def start = System.currentTimeMillis()

def templateDirectory = args[0]
if (!templateDirectory) {
	throw new IllegalArgumentException("required templateDirectory parameter")
}

def outputDirectory = args[1]
if (!outputDirectory) {
	throw new IllegalArgumentException("required outputDirectory parameter")
}

def ordering = Constants.orderings.find {
	it.key == "best"
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

Constants.champions.each {
	champion ->
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
			def summonerService = new SummonerService(mongo)
			def summoner_ratings = mongo.live.summoner_ratings

			def data = []

			def ratingOrder = ordering.key == "best" ? -1 : 1
			def resultSet = summoner_ratings.find([
					champion: champion.key
			] as BasicDBObject).sort([rating: ratingOrder] as BasicDBObject).limit(100)
			resultSet.eachWithIndex {
				row, i ->
					def datum = new HashMap(row)
					datum["rank"] = i + 1
					data.add(datum)
			}

			def summonerIds = data.collect {
				it.summonerId
			}
			def summoners = summonerService.getSummonersByIds(summonerIds)

			data.each {
				def summonerId = it.summonerId
				if (summoners[summonerId] == null) {
					throw new IllegalStateException("missing summoner name and league for $summonerId")
				}
				it.name = summoners[summonerId].name
				it.league = summoners[summonerId].league
			}

			model["data"] = data.collect {
				it.subMap(["rank", "league", "name", "won", "lost", "rating"])
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
			println("templated ${champion.value.name} ${(System.currentTimeMillis() - start)/1000d}")
	}
}
