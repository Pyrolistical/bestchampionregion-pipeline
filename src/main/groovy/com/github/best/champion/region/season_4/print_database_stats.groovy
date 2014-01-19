package com.github.best.champion.region.season_4

import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.mongodb.BasicDBObject
import groovy.time.TimeDuration

MongoUtils.connect {

	mongo ->
		def numberOfSummoners = mongo.season_4.ranked_summoners.count([
				'$or': [[
						league: "challenger"
				], [
						league: [
								'$regex': ~/^diamond-\d$/
						]
				]]
		] as BasicDBObject)
		println("Number of Summoners (D5 up): $numberOfSummoners")
		println("\tPending Update")
		(1..6).each {
			hour ->
				def duration = new TimeDuration(hour, 0, 0, 0)
				def numberOfSummonersToUpdate = mongo.season_4.ranked_summoners.count([
						'$and': [[
								'$or': [[
										league: "challenger"
								], [
										league: [
												'$regex': ~/^diamond-\d$/
										]
								]]
						], [
								'$or': [[
										"league-last-retrieved": [
												'$lt': duration.ago.time
										]
								], [
										"league-last-retrieved": [
												'$exists': false
										]
								]]
						]]
				] as BasicDBObject)
				println("\t\t$hour hour: $numberOfSummonersToUpdate")
		}

		def numberOfRankedGames = mongo.season_4.ranked_games.count()
		println("Number of Ranked Games (D5 up): $numberOfRankedGames")
		println("\tPending Update")
		(1..6).each {
			hour ->
				def duration = new TimeDuration(hour, 0, 0, 0)
				def numberOfSummonersToUpdate = mongo.season_4.ranked_summoners.count([
						'$and': [[
								'$or': [[
										league: "challenger"
								], [
										league: [
												'$regex': ~/^diamond-\d$/
										]
								]]
						], [
								'$or': [[
										"recent-games-last-retrieved": [
												'$lt': duration.ago.time
										]
								], [
										"recent-games-last-retrieved": [
												'$exists': false
										]
								]]
						]]
				] as BasicDBObject)
				println("\t\t$hour hour: $numberOfSummonersToUpdate")
		}

		def dataSize = mongo.season_4.stats.dataSize / 1024 / 1024 as int
		println("season_4 dataSize: $dataSize MB")
}
