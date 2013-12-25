@Grapes([
	@Grab("org.mongodb:mongo-java-driver:2.11.3"),
	@Grab("com.github.concept-not-found:mongo-groovy-extension:1-SNAPSHOT"),
])

import com.mongodb.*
import com.github.concept.not.found.mongo.groovy.util.MongoUtils

MongoUtils.connect {
	mongo ->
		def ranked_games = mongo.live.ranked_games
		def ranked_summoners = mongo.live.ranked_summoners

		def n = 0
		def gamesCount = ranked_games.count()
		ranked_games.find([
		] as BasicDBObject, [
				"summonerId": 1,
				"fellowPlayers.summonerId": 1
		] as BasicDBObject).each {
			game ->
				upsertSummoner(game.summonerId, ranked_summoners)
				game.fellowPlayers.each {
					fellowPlayer ->
						def summonerId = fellowPlayer.summonerId
						upsertSummoner(summonerId, ranked_summoners)
				}
				if (++n % 1000 == 0) {
					println("$n/$gamesCount done")
				}
		}
}

def upsertSummoner(summonerId, ranked_summoners) {
	def r = ranked_summoners.update(
			[
					_id: summonerId
			] as BasicDBObject,
			[
					'$set': [:]
			] as BasicDBObject,
			true,
			false
	)
}