@Grab(group="org.thymeleaf", module="thymeleaf", version="2.1.2.RELEASE")

import org.thymeleaf.*
import org.thymeleaf.context.*
import org.thymeleaf.templateresolver.*

def templateEngine = new TemplateEngine()
def fileTemplateResolver = new FileTemplateResolver()
fileTemplateResolver.setPrefix("/Users/rchen/dev/projects/github.com/concept-not-found/bestchampionregion/template/ordering/champion/region/season/")
fileTemplateResolver.setSuffix(".html")
templateEngine.setTemplateResolver(fileTemplateResolver)

def context = new Context()
def model = context.variables

model["active"] = [
	ordering: Constants.orderings.find {
		it.value == "Best"
	},
	champion: Constants.champions.find {
		it.value == "Annie"
	},
	region: Constants.regions.find {
		it.value == "NA"
	},
	season: Constants.seasons.find {
		it.value == "Season 3"
	}
]

model["orderings"] = Constants.orderings
model["champions"] = Constants.champions
model["regions"] = Constants.regions
model["seasons"] = Constants.seasons

new File("output.html").withWriter {
	writer ->
		templateEngine.process("index", context, writer)
}
