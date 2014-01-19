package com.github.best.champion.region.season_4

import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.mongodb.BasicDBObject
import groovy.json.JsonOutput

def outputDirectory = args.length >= 1 ? args[0] : "."
def database = "season_4"

MongoUtils.connect {
	mongo ->
		def summonerIds = mongo."$database".ranked_summoners.find([
				'$or': [[
						league: "challenger"
				], [
						league: [
								'$regex': ~/^diamond-\d$/
						]
				]]
		] as BasicDBObject, [
				_id: 1
		] as BasicDBObject).sort([_id: -1] as BasicDBObject).collect {
			it._id
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
				"recent_games_by_summoner_1p3": "api/lol/{region}/v1.3/game/by-summoner/{summonerId}/recent"
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
