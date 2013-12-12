@Grab(group="org.mongodb", module="mongo-java-driver", version="2.9.3")
import com.mongodb.*

def mongo = new Mongo()
def db = mongo.getDB("sandbox")
def lolapi = db.getCollection("lolapi")

def annie = db.getCollection("annie")

lolapi.find(["data.champions.name": "Annie"] as BasicDBObject).each {
	rankedStats ->
		def summonerId = rankedStats.data.getInt("summonerId")
		def champion = rankedStats.data.champions.find {
			champion ->
				champion.name == "Annie"
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
		annie.update(
			[_id: summonerId] as BasicDBObject,
			row as BasicDBObject,
			true,
			false
		)
}

annie.find().each {println(it)}
