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
		def pending = (1..6).collect {
			hour ->
				def duration = new TimeDuration(hour, 0, 0, 0)
				mongo.season_4.ranked_summoners.count([
						'$or': [[
								league: "challenger"
						], [
								league: [
										'$regex': ~/^diamond-\d$/
								]
						]],
						"league-last-retrieved": [
								'$lt': duration.ago.time
						]
				] as BasicDBObject)
		}
		(6..1).each {
			hour ->
				def next = hour > 1 ? pending[6 - hour + 1] : 0
				println("\t\t$hour-${hour - 1} hour: ${pending[6 - hour] - next}")
		}


		def numberOfRankedGames = mongo.season_4.ranked_games.count()
		println("Number of Ranked Games (D5 up): $numberOfRankedGames")
		println("\tPending Update")
		pending = (1..6).collect {
			hour ->
				def duration = new TimeDuration(hour - 6, 0, 0, 0)
				mongo.season_4.ranked_summoners.count([
						'$or': [[
								league: "challenger"
						], [
								league: [
										'$regex': ~/^diamond-\d$/
								]
						]],
						"recent-games-last-retrieved": [
								'$lt': duration.ago.time,
						]
				] as BasicDBObject)
		}
		(6..1).each {
			hour ->
				def next = hour > 1 ? pending[6 - hour + 1] : 0
				println("\t\t$hour-${hour - 1} hour: ${pending[6 - hour] - next}")
		}


		def dataSize = mongo.season_4.stats.dataSize / 1024 / 1024 as int
		println("season_4 dataSize: $dataSize MB")
}
