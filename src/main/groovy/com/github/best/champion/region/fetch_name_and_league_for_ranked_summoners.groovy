package com.github.best.champion.region

import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.mongodb.BasicDBObject

MongoUtils.connect {
	mongo ->

		def missing = mongo.live.ranked_summoners.find([
				'$or': [
						[name: ['$exists': false]],
						[league: ['$exists': false]]
				]
		] as BasicDBObject, [
				_id: 1
		] as BasicDBObject).limit(100_000).collect {
			it._id
		}

		def summonerService = new SummonerService(mongo)
		summonerService.getNameAndLeague(missing)
}
