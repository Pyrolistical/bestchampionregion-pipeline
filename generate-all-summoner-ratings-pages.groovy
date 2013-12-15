def templateDirectory = "/Users/rchen/dev/projects/github.com/concept-not-found/bestchampionregion/template/ordering/champion/region/season/"
def outputDirectory = "/Users/rchen/dev/projects/github.com/concept-not-found/bestchampionregion/template"
Constants.orderings.each {
	ordering ->
		Constants.champions.each {
			champion ->
				run(new File("generate-summoner-ratings-page.groovy"), [ordering.key, champion.key, templateDirectory, outputDirectory] as String[])
		}
}