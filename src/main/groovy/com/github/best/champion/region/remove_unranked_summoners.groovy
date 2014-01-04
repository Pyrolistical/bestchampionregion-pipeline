package com.github.best.champion.region

import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.mongodb.BasicDBObject

MongoUtils.connect {
	mongo ->
		def summoner_league = mongo.live.league_by_summoner_2p2
		def ranked_summoners = mongo.live.ranked_summoners
		def recent_games = mongo.live.recent_games_by_summoner_1p2
		def ranked_games = mongo.live.ranked_games
		def ranked_stats = mongo.live.ranked_stats_by_summoner_1p2
		def summoner_ratings = mongo.live.summoner_ratings

		summoner_ratings.ensureIndex([
				summonerId: 1
		] as BasicDBObject)

		def unrankedSummonerIds = summoner_league.find([
				data: null
		] as BasicDBObject, [
				"path-parameters.summonerId": 1
		] as BasicDBObject).collect {
			it."path-parameters".summonerId
		}

		unrankedSummonerIds.each {
			summonerId ->
				def n
				n = ranked_summoners.remove([
						_id: summonerId
				] as BasicDBObject).n
				println("removed $n ranked_summoners for $summonerId")

				n = recent_games.remove([
						"data.summonerId": summonerId
				] as BasicDBObject).n
				println("removed $n recent_games for $summonerId")

				n = ranked_games.remove([
						summonerId: summonerId
				] as BasicDBObject).n
				println("removed $n ranked_games for $summonerId")

				n = ranked_stats.remove([
						"data.summonerId": summonerId
				] as BasicDBObject).n
				println("removed $n ranked_stats for $summonerId")

				n = summoner_ratings.remove([
						summonerId: summonerId
				] as BasicDBObject).n
				println("removed $n summoner_ratings for $summonerId")

				n = summoner_league.remove([
						data: null,
						"path-parameters.summonerId": summonerId
				] as BasicDBObject).n
				println("removed $n summoner_league for $summonerId")
		}

}
