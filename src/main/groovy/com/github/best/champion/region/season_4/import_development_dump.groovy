package com.github.best.champion.region.season_4

import com.github.concept.not.found.mongo.groovy.util.MongoUtils

def dumpFile = args[0]

new ProcessBuilder(
		"7za",
		"x",
		dumpFile
)
		.inheritIO()
		.start()
		.waitFor()

MongoUtils.connect {
	mongo ->
		new ProcessBuilder(
				"mongorestore",
				"-v"
		)
				.inheritIO()
				.start()
				.waitFor()
}
