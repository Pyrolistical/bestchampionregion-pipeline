@Grapes([
	@Grab("org.thymeleaf:thymeleaf:2.1.2.RELEASE"),
	@Grab("org.slf4j:slf4j-simple:1.7.5"),
	@Grab("org.mongodb:mongo-java-driver:2.11.3"),
	@Grab("com.github.concept-not-found:mongo-groovy-extension:1-SNAPSHOT"),
	@Grab("com.github.pyrolistical:best-champion-region-services:1-SNAPSHOT")
])

import com.mongodb.*

import org.thymeleaf.*
import org.thymeleaf.context.*
import org.thymeleaf.templateresolver.*

import com.github.concept.not.found.mongo.groovy.util.MongoUtils

import com.github.best.champion.region.service.*

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
		def summonerService = new SummonerService(mongo)
		model["data"] = []
		Constants.champions.each {
			champion ->
				def summoner_ratings = mongo.live.summoner_ratings

				def data = [
					ordering: ordering,
					champion: champion,
					region: region,
					season: season
				]
				def table = []

				def ratingOrder = ordering.key == "best" ? -1 : 1
				summoner_ratings.find([
						champion: champion.key
				] as BasicDBObject).sort([rating: ratingOrder] as BasicDBObject).limit(5).eachWithIndex {
					row, i ->
						def datum = new HashMap(row)
						datum["rank"] = i + 1
						table.add(datum)
				}

				def summonerIds = table.collect {
					it.summonerId
				}
				def summoners = summonerService.getSummonersByIds(summonerIds)

				table.each {
					def summonerId = it.summonerId
					if (summoners[summonerId] == null) {
						throw new IllegalStateException("missing summoner name and league for $summonerId")
					}
					it.name = summoners[summonerId].name
					it.league = summoners[summonerId].league
				}

				data.table = table.collect {
					it.subMap(["rank", "league", "name", "rating"])
				}

				model["data"].add(data)
		}
		def outputPath = new File(outputDirectory, "top")
		outputPath.mkdirs()
		new File(outputPath, "index.html").withWriter {
			templateEngine.process("index", context, it)
		}
}

