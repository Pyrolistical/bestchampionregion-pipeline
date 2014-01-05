package com.github.best.champion.region

def enum League {
	CHALLENGER("Challenger"),
	DIAMOND_1("Diamond I"),
	DIAMOND_2("Diamond II"),
	DIAMOND_3("Diamond III"),
	DIAMOND_4("Diamond IV"),
	DIAMOND_5("Diamond V"),
	PLATINUM_1("Platinum I"),
	PLATINUM_2("Platinum II"),
	PLATINUM_3("Platinum III"),
	PLATINUM_4("Platinum IV"),
	PLATINUM_5("Platinum V"),
	GOLD_1("Gold I"),
	GOLD_2("Gold II"),
	GOLD_3("Gold III"),
	GOLD_4("Gold IV"),
	GOLD_5("Gold V"),
	SILVER_1("Silver I"),
	SILVER_2("Silver II"),
	SILVER_3("Silver III"),
	SILVER_4("Silver IV"),
	SILVER_5("Silver V"),
	BRONZE_1("Bronze I"),
	BRONZE_2("Bronze II"),
	BRONZE_3("Bronze III"),
	BRONZE_4("Bronze IV"),
	BRONZE_5("Bronze V")

	def label
	def path

	def League(label) {
		this.label = label
		this.path = name().toLowerCase().replace("_", "-")
	}

	def static League getLeague(tier, rank) {
		if (tier.equalsIgnoreCase(CHALLENGER.label)) {
			return CHALLENGER
		}
		def leagueString = "$tier $rank"
		values().find {
			it.label.equalsIgnoreCase(leagueString)
		}
	}

	def static League getLeagueByPath(path) {
		values().find {
			it.path.equals(path)
		}
	}
}
