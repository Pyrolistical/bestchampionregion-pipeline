package com.github.best.champion.region.season_3

import com.github.best.champion.region.League
import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.mongodb.BasicDBObject
import groovy.json.JsonOutput

def outputDirectory = args.length >= 1 ? args[0] : "."
def database = "season_3"

MongoUtils.connect {
	mongo ->
		def summonerIds = []
		League.each {
			league ->
				mongo."$database".ranked_summoners.find([
						league: league.path
				] as BasicDBObject, [
						_id: 1
				] as BasicDBObject).sort([_id: -1] as BasicDBObject).limit(100).each {
					summonerIds.add(it._id)
				}
		}
		println("exporting ${summonerIds.size()} summoners")
		new ProcessBuilder(
				"mongodump",
				"-v",
				"--db",
				database,
				"--collection",
				"ranked_summoners",
				"--query",
				'{_id: {$in: [' + summonerIds.join(",") + ']}}'
		)
				.directory(new File(outputDirectory))
				.inheritIO()
				.start()
				.waitFor()

		[
				"league_by_summoner_2p2": "api/lol/{region}/v2.2/league/by-summoner/{summonerId}",
				"recent_games_by_summoner_1p2": "api/lol/{region}/v1.2/game/by-summoner/{summonerId}/recent",
				"ranked_stats_by_summoner_1p2": "api/lol/{region}/v1.2/stats/by-summoner/{summonerId}/ranked"
		].each {
			collection, api ->
				def query = [
						headers: [:],
						base: "http://localhost:30080/",
						path: api,
						"path-parameters": ['$in': summonerIds.collect {
							[
									region: "na",
									summonerId: it as String
							]
						}]
				]
				new ProcessBuilder(
						"mongodump",
						"-v",
						"--db",
						database,
						"--collection",
						collection,
						"--query",
						JsonOutput.toJson(query)
				)
						.directory(new File(outputDirectory))
						.inheritIO()
						.start()
						.waitFor()
		}
}
new ProcessBuilder(
		"7za",
		"a",
		"dump-${System.currentTimeMillis()}.7z",
		"dump"
)
		.directory(new File(outputDirectory))
		.inheritIO()
		.start()
		.waitFor()
new File(outputDirectory, "dump").deleteDir()
