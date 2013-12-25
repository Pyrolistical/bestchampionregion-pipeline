@Grapes([
	@Grab("org.mongodb:mongo-java-driver:2.11.3"),
	@Grab("com.github.concept-not-found:mongo-groovy-extension:1-SNAPSHOT")
])

import com.mongodb.*
import com.github.concept.not.found.mongo.groovy.util.MongoUtils

def rankedSubTypes = [
		"RANKED_SOLO_5x5",
		"RANKED_TEAM_5x5"
]

MongoUtils.connect {
	mongo ->
		def recent_games = mongo.live.recent_games_by_summoner_1p2
		def ranked_games = mongo.live.ranked_games

		ranked_games.ensureIndex([
				summonerId: 1,
				gameId: 1
		] as BasicDBObject, [
				unique: true
		] as BasicDBObject)

		def n = 0
		def summonerCount = recent_games.count([
				"data.games.level": 30,
				"data.games.subType": [
						'$in': rankedSubTypes
				]
		] as BasicDBObject)
		recent_games.find([
				"data.games.level": 30,
				"data.games.subType": [
						'$in': rankedSubTypes
				]
		] as BasicDBObject).each {
			def summonerId = it.data.summonerId
			it.data.games.each {
				game ->
					if (!rankedSubTypes.contains(game.subType)) {
						return
					}

					game.summonerId = summonerId
					ranked_games.update(
							[
									summonerId: summonerId,
									gameId: game.gameId
							] as BasicDBObject,
							game as BasicDBObject,
							true,
							false
					)
			}
			println("${++n}/$summonerCount done $summonerId")
		}
}
