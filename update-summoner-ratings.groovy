@Grab(group = "org.mongodb", module = "mongo-java-driver", version = "2.9.3")
import com.mongodb.*

def championName = args[0]
if (!championName) {
	throw new IllegalArgumentException("required champion parameter")
}

if (!Constants.champions[championName]) {
	throw new IllegalArgumentException("No such champion named $championName")
}
def mongo = new Mongo()
try {
	def db = mongo.getDB("live")
	def lolapi = db.getCollection("lolapi")

	def table = db.getCollection("summoner_ratings_$championName")

	lolapi.find(["data.champions.name": championName] as BasicDBObject).each {
		rankedStats ->
			def summonerId = rankedStats.data.getInt("summonerId")
			def champion = rankedStats.data.champions.find {
				champion ->
					champion.name == championName
			}
			def won = champion.stats.find {
				stat ->
					stat.name == "TOTAL_SESSIONS_WON"
			}.getInt("value")
			def lost = champion.stats.find {
				stat ->
					stat.name == "TOTAL_SESSIONS_LOST"
			}.getInt("value")
			def rating = won - lost
			def row = [
					_id: summonerId,
					summonerId: summonerId,
					won: won,
					lost: lost,
					rating: rating
			]
			table.update(
					[_id: summonerId] as BasicDBObject,
					row as BasicDBObject,
					true,
					false
			)
	}

} finally {
	mongo.close()
}

