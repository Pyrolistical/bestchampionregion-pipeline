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
				path: "api/{region}/v2.1/league/by-summoner/{summonerId}",
				data: ['$ne': null]
		] as BasicDBObject).each {
			leagueBySummoner ->
				if (leagueBySummoner.data.isEmpty()) {
					return
				}
				def summonerId = leagueBySummoner."path-parameters".summonerId as int
				if (leagueBySummoner.data."$summonerId" == null) {
					return
				}
				def tier = leagueBySummoner.data."$summonerId".tier
				// default to V because can't fetch rank for some summoners due to ranked decay
				def rank = "V"
				def entry = leagueBySummoner.data."$summonerId".entries.find {
					it.playerOrTeamId == summonerId as String
				}
				if (entry != null) {
					rank = entry.rank
				}
				def league = Constants.leagues.find {
					if (it.key == "challenger") {
						it.value.name.toLowerCase() == tier.toLowerCase()
					} else {
						it.value.name.toLowerCase() == "$tier $rank".toLowerCase()
					}
				}
				updateSummoner(summonerId, league.key, table)
				if (++done % 1000 == 0) {
					println("done $done")
				}
		}
}

def updateSummoner(summonerId, league, table) {
	def entry = [
			[
					'$set': [
							league: league
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

