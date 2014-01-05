package com.github.best.champion.region

def class Constants {

	def static regions = asText(
			["NA"],
			["EUW"],
			["EUNE"],
			["BR"],
			["TR"],
			["RU"],
			["LAN"],
			["LAS"],
			["OCE"]
	)

	def static seasons = asText(
			["season3", "Season 3"],
			["season4", "Seaons 4"]
	)

	def static asText(Object... values) {
		def result = [:]
		values.each {
			value ->
				def key = value[0]
				def name = value.size() == 2 ? value[1] : key.capitalize()
				result[key] = [
						name: name,
						path: name.toLowerCase().replace("'", "-").replace(" ", "-")
				]
		}
		result
	}
}
