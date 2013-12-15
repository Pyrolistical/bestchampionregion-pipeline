Constants.champions.each {
	key, value ->
		run(new File("update-summoner-ratings.groovy"), [key] as String[])
}