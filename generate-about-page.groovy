@Grapes([
	@Grab(group = "org.thymeleaf", module = "thymeleaf", version = "2.1.2.RELEASE"),
	@Grab(group = "org.slf4j", module = "slf4j-simple", version = "1.7.5"),
	@Grab(group="com.github.concept-not-found", module="mongo-groovy-extension", version="1-SNAPSHOT"),
	@Grab(group = "org.mongodb", module = "mongo-java-driver", version = "2.11.3")
])

import com.mongodb.*
import org.thymeleaf.*
import org.thymeleaf.context.*
import org.thymeleaf.templateresolver.*

import com.github.concept.not.found.mongo.groovy.util.MongoUtils

def templateDirectory = args[0]
if (!templateDirectory) {
	throw new IllegalArgumentException("required templateDirectory parameter")
}

def outputDirectory = args[1]
if (!outputDirectory) {
	throw new IllegalArgumentException("required outputDirectory parameter")
}

def templateEngine = new TemplateEngine()
def fileTemplateResolver = new FileTemplateResolver()
fileTemplateResolver.setPrefix("$templateDirectory/about/")
fileTemplateResolver.setSuffix(".html")
templateEngine.setTemplateResolver(fileTemplateResolver)

def context = new Context()
def model = context.variables

MongoUtils.connect {
	mongo ->
		def lolapi = mongo.live.lolapi

		def numberOfSummoners = lolapi.count([path: "api/lol/{region}/v1.1/stats/by-summoner/{summonerId}/ranked"] as BasicDBObject)
		model["numberOfSummoners"] = numberOfSummoners

		def outputPath = new File(outputDirectory, "about")
		outputPath.mkdirs()
		new File(outputPath, "index.html").withWriter {
			templateEngine.process("index", context, it)
		}
}
