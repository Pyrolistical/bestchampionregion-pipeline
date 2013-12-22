@Grapes([
	@Grab("com.github.concept-not-found:mongo-groovy-extension:1-SNAPSHOT"),
	@Grab("org.mongodb:mongo-java-driver:2.11.3")
])
import com.mongodb.*

import com.github.concept.not.found.mongo.groovy.util.MongoUtils

MongoUtils.connect {
	mongo ->
		def lolapi = mongo.live.lolapi

		def table = mongo.live.summoner

		def done = 0
		lolapi.find([
				path: "api/lol/{region}/v1.1/summoner/by-name/{name}"
		] as BasicDBObject).each {
			summonerByName ->
				def summonerId = summonerByName.data.id
				def name = summonerByName.data.name
				updateSummoner(summonerId, name, table)
				if (++done % 1000 == 0) {
					println("done $done")
				}
		}
		lolapi.find([
				path: "api/lol/{region}/v1.1/summoner/{summonerIds}/name"
		] as BasicDBObject).each {
			nameBySummonerIds ->
				nameBySummonerIds.data.summoners.each {
					summoner ->
						def summonerId = summoner.id
						def name = summoner.name
						updateSummoner(summonerId, name, table)
						if (++done % 1000 == 0) {
							println("done $done")
						}
				}
		}
}

def updateSummoner(summonerId, name, table) {
	def entry = [
			[
					'$set': [
							name: name
					]
			]
	]
	table.update(
			[_id: summonerId] as BasicDBObject,
			entry as BasicDBObject,
			true,
			false
	)
}

