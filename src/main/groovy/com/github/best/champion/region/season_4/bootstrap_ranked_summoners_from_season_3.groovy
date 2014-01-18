package com.github.best.champion.region.season_4
import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.mongodb.BasicDBObject

MongoUtils.connect {
	mongo ->
		mongo.season_3.ranked_summoners.find([
				'$or': [[
						league: "challenger"
				], [
						league: [
								'$regex': ~/^diamond-\d$/
						]
				]]
		] as BasicDBObject).each {
			mongo.season_4.ranked_summoners.insert(it)
		}
}