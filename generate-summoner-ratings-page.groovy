@Grapes([
	@Grab(group="org.thymeleaf", module="thymeleaf", version="2.1.2.RELEASE"),
	@Grab(group="org.mongodb", module="mongo-java-driver", version="2.9.3")
])

import com.mongodb.*

import org.thymeleaf.*
import org.thymeleaf.context.*
import org.thymeleaf.templateresolver.*

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
fileTemplateResolver.setPrefix(templateDirectory)
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


def mongo = new Mongo()
try {
	def db = mongo.getDB("live")

	def collection = db.getCollection("summoner_ratings_${champion.key}")

	def data = []

	def ratingOrder = ordering.key == "best" ? -1 : 1
	collection.find().sort([rating: ratingOrder] as BasicDBObject).limit(100).eachWithIndex {
		row, i ->
			def datum = new HashMap(row)
			datum["rank"] = i + 1
			data.add(datum)
	}

	def lolapi = db.getCollection("lolapi")
	data.each {
		it.name = convertSummonerIdToName(lolapi, it.summonerId)
	}

	model["data"] = data.collect {
		it.subMap(["rank", "name", "won", "lost", "rating"])
	}

	def orderingPath = ordering.value.path
	def championPath = champion.value.path
	def regionPath = region.value.path
	def seasonPath = season.value.path
	def outputPath = new File("$outputDirectory/$orderingPath/$championPath/$regionPath/$seasonPath")
	outputPath.mkdirs()
	new File(outputPath, "index.html").withWriter {
		templateEngine.process("index", context, it)
	}
} finally {
	mongo.close()
}

def convertSummonerIdToName(lolapi, summonerId) {
	lolapi.findOne([path: "api/lol/{region}/v1.1/summoner/by-name/{name}", "data.id": summonerId] as BasicDBObject).data.name
}

