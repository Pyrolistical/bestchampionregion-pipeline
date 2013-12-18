Constants.champions.each {
	key, value ->
		println("start $key")
		def classloader = new GroovyClassLoader()
		def shell = new GroovyShell(classloader, getBinding())
		shell.run(new File("update-summoner-ratings.groovy"), [key] as String[])
		println("done ${value.name}")
}