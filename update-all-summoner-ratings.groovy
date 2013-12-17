Constants.champions.each {
	key, value ->
		println("start $key")
		run(new File("update-summoner-ratings.groovy"), [key] as String[])
		println("done ${value.name}")
}