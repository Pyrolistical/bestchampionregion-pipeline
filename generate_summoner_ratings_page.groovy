@Grapes([
	@Grab("org.thymeleaf:thymeleaf:2.1.2.RELEASE"),
	@Grab("org.slf4j:slf4j-simple:1.7.5"),
	@Grab("org.mongodb:mongo-java-driver:2.11.3"),
	@Grab("com.github.concept-not-found:mongo-groovy-extension:1-SNAPSHOT")
])

import com.mongodb.*

import org.thymeleaf.*
import org.thymeleaf.context.*
import org.thymeleaf.templateresolver.*

import com.github.concept.not.found.mongo.groovy.util.MongoUtils

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
def start = System.currentTimeMillis()
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
println("init ${(System.currentTimeMillis() - start)/1000d}")

MongoUtils.connect {
	mongo ->
		println("connected ${(System.currentTimeMillis() - start)/1000d}")
		def summoner = mongo.live.summoner
		def summoner_ratings = mongo.live.summoner_ratings

		def data = []

		def ratingOrder = ordering.key == "best" ? -1 : 1
		def resultSet = summoner_ratings.find([
				champion: champion.key
		] as BasicDBObject).sort([rating: ratingOrder] as BasicDBObject).limit(100)
		println("queried ${(System.currentTimeMillis() - start)/1000d}")
		resultSet.eachWithIndex {
			row, i ->
				def datum = new HashMap(row)
				datum["rank"] = i + 1
				data.add(datum)
		}
		println("pulled ${(System.currentTimeMillis() - start)/1000d}")

		def summonerIds = data.collect {
			it.summonerId
		}
		def summoners = getSummoner(summoner, summonerIds)

		data.each {
			def summonerId = it.summonerId
			if (summoners[summonerId] == null) {
				throw new IllegalStateException("missing summoner name and league for $summonerId")
			}
			it.name = summoners[summonerId].name
			it.league = summoners[summonerId].league
		}
		println("summonered ${(System.currentTimeMillis() - start)/1000d}")

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
		println("templated ${(System.currentTimeMillis() - start)/1000d}")
}

def getSummoner(summoner, summonerIds) {
	def summoners = [:]
	summoner.find([_id: ['$in': summonerIds]] as BasicDBObject).each {
		def league = Constants.leagues.find {
			league ->
				league.key == it.league
		}
		summoners[it."_id"] = [
				name: it.name,
				league: league
		]
	}
	summoners
}