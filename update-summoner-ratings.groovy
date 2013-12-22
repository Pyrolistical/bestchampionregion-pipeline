@Grapes([
	@Grab("com.github.concept-not-found:mongo-groovy-extension:1-SNAPSHOT"),
	@Grab("org.mongodb:mongo-java-driver:2.11.3")
])
import com.mongodb.*

import com.github.concept.not.found.mongo.groovy.util.MongoUtils

MongoUtils.connect {
	mongo ->
		def lolapi = mongo.live.lolapi

		def table = mongo.live."summoner_ratings"

		table.ensureIndex([
				champion: 1,
				summonerId: 1
		] as BasicDBObject, [
				unique: true
		] as BasicDBObject)

		table.ensureIndex([
				champion: 1,
				rating: -1
		] as BasicDBObject)

		def done = 0
		lolapi.find([
				path: "api/lol/{region}/v1.1/stats/by-summoner/{summonerId}/ranked",
				data: ['$ne': null]
		] as BasicDBObject).each {
			rankedStats ->
				def summonerId = rankedStats.data.getInt("summonerId")
				rankedStats.data.champions.each {
					champion ->
						if (champion.name == null) {
							return
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
								summonerId: summonerId,
								champion: champion.name,
								won: won,
								lost: lost,
								rating: rating
						]
						table.update(
								[summonerId: summonerId, champion: champion.name] as BasicDBObject,
								row as BasicDBObject,
								true,
								false
						)
				}
				if (++done % 1000 == 0) {
					println("done $done")
				}
		}
}

