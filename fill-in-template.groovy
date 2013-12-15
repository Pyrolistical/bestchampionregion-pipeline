@Grapes([
	@Grab(group="org.thymeleaf", module="thymeleaf", version="2.1.2.RELEASE"),
	@Grab(group="org.mongodb", module="mongo-java-driver", version="2.9.3")
])

import com.mongodb.*

import org.thymeleaf.*
import org.thymeleaf.context.*
import org.thymeleaf.templateresolver.*

def templateEngine = new TemplateEngine()
def fileTemplateResolver = new FileTemplateResolver()
fileTemplateResolver.setPrefix("/Users/rchen/dev/projects/github.com/concept-not-found/bestchampionregion/template/ordering/champion/region/season/")
fileTemplateResolver.setSuffix(".html")
templateEngine.setTemplateResolver(fileTemplateResolver)

def context = new Context()
def model = context.variables

model["active"] = [
	ordering: Constants.orderings.find {
		it.value == "Best"
	},
	champion: Constants.champions.find {
		it.value == "Annie"
	},
	region: Constants.regions.find {
		it.value == "NA"
	},
	season: Constants.seasons.find {
		it.value == "Season 3"
	}
]

model["orderings"] = Constants.orderings
model["champions"] = Constants.champions
model["regions"] = Constants.regions
model["seasons"] = Constants.seasons


def mongo = new Mongo()
try {
	def db = mongo.getDB("live")

	def collection = db.getCollection("table_annie")

	def data = []

	collection.find().sort([rating: -1] as BasicDBObject).limit(20).eachWithIndex {
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

	new File("/Users/rchen/dev/projects/github.com/concept-not-found/bestchampionregion/template/ordering/champion/region/season/output.html").withWriter {
		templateEngine.process("index", context, it)
	}
} finally {
	mongo.close()
}

def convertSummonerIdToName(lolapi, summonerId) {
	lolapi.findOne([path: "api/lol/{region}/v1.1/summoner/by-name/{name}", "data.id": summonerId] as BasicDBObject).data.name
}

