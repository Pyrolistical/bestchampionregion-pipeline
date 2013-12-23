@Grapes([
	@Grab("com.github.concept-not-found:mongo-groovy-extension:1-SNAPSHOT"),
	@Grab("org.mongodb:mongo-java-driver:2.11.3")
])
import com.mongodb.*

import com.github.concept.not.found.mongo.groovy.util.MongoUtils

MongoUtils.connect {
	mongo ->
		def ranked_stats = mongo.live.ranked_stats_by_summoner_1p2

		def summoner_ratings = mongo.live.summoner_ratings

		summoner_ratings.ensureIndex([
				champion: 1,
				summonerId: 1
		] as BasicDBObject, [
				unique: true
		] as BasicDBObject)

		summoner_ratings.ensureIndex([
				champion: 1,
				rating: -1
		] as BasicDBObject)

		def done = 0
		ranked_stats.find([
				data: ['$ne': null]
		] as BasicDBObject, [
		        "data.summonerId": 1,
				"data.champions.name": 1,
				"data.champions.stats.totalSessionsWon": 1,
				"data.champions.stats.totalSessionsLost": 1
		] as BasicDBObject).each {
			rankedStat ->
				def summonerId = rankedStat.data.getInt("summonerId")
				rankedStat.data.champions.each {
					champion ->
						if (champion.name == "Combined") {
							return
						}
						def won = champion.stats.totalSessionsWon
						def lost = champion.stats.totalSessionsLost
						def rating = won - lost
						def row = [
								summonerId: summonerId,
								champion: champion.name,
								won: won,
								lost: lost,
								rating: rating
						]
						summoner_ratings.update(
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

