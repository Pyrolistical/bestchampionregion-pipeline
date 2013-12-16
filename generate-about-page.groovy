@Grapes([
	@Grab(group="org.thymeleaf", module="thymeleaf", version="2.1.2.RELEASE"),
	@Grab(group="org.mongodb", module="mongo-java-driver", version="2.9.3")
])

import com.mongodb.*

import org.thymeleaf.*
import org.thymeleaf.context.*
import org.thymeleaf.templateresolver.*

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

def mongo = new Mongo()
try {
	def db = mongo.getDB("live")

	def lolapi = db.getCollection("lolapi")

	def numberOfSummoners = lolapi.count([path: "api/lol/{region}/v1.1/stats/by-summoner/{summonerId}/ranked"] as BasicDBObject)
	model["numberOfSummoners"] = numberOfSummoners

	def outputPath = new File(outputDirectory, "about")
	outputPath.mkdirs()
	new File(outputPath, "index.html").withWriter {
		templateEngine.process("index", context, it)
	}
} finally {
	mongo.close()
}
