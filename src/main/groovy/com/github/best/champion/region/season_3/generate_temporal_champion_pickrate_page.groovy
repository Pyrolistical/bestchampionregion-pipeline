package com.github.best.champion.region.season_3

import com.github.best.champion.region.Champion
import com.github.best.champion.region.Constants
import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.mongodb.BasicDBObject
import de.erichseifert.gral.data.DataTable
import de.erichseifert.gral.io.plots.DrawableWriterFactory
import de.erichseifert.gral.plots.XYPlot
import de.erichseifert.gral.plots.lines.DefaultLineRenderer2D
import de.erichseifert.gral.plots.lines.LineRenderer
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templateresolver.FileTemplateResolver

import java.awt.Color
import java.util.concurrent.TimeUnit


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
fileTemplateResolver.setPrefix("$templateDirectory/top/champion/region/pickrate/")
fileTemplateResolver.setSuffix(".html")
templateEngine.setTemplateResolver(fileTemplateResolver)

def context = new Context()
def model = context.variables

def region = Constants.regions.find {
	it.key == "NA"
}

def season = Constants.seasons.find {
	it.key == "season3"
}

def outputPath = new File(outputDirectory, "top/champion/${region.value.path}/pickrate")
outputPath.mkdirs()

// Since the day isn't over yet we can't compute the actual pick rates for today
// So start the window from yesterday
def yesterday = new Date(1388161923394 - (1388161923394 % TimeUnit.DAYS.toMillis(1))) // For testing
def minLimit = new Date(yesterday.time - TimeUnit.DAYS.toMillis(14))

MongoUtils.connect {
	mongo ->
		mongo.season_3.ranked_games.ensureIndex([
				createDate: 1
		] as BasicDBObject)

		def gamesPlayed = [:]
		mongo.season_3.command([
				aggregate: "ranked_games",
				pipeline: [
						// Match to restrict games to a x time view
						[
								'$match': [
										'$and': [[
												createDate: [
														'$gt': minLimit.time
												]], [
												createDate: [
														'$lt': yesterday.time
												]]
										]
								]
						],
						[
								'$project': [
										_id: 0,
										createDate: 1,
										gameId: 1
								]
						],
						[
								'$group': [
										_id: [
												createDate: [
														'$subtract': ['$createDate', [
																'$mod': [
																		'$createDate', TimeUnit.DAYS.toMillis(1)
																]
														]]
												],
												gameId: '$gameId'
										]
								]
						],
						[
								'$group': [
										_id: [
												createDate: '$_id.createDate'
										],
										count: [
												'$sum': 1
										]
								]
						]
				]
		] as BasicDBObject).result.each {
			def gameDate = new Date(it._id.createDate)
			gamesPlayed[gameDate] = it.count
		}

		def graphData = [:]
		mongo.season_3.command([
				aggregate: "ranked_games",
				pipeline: [
						// Match to restrict games to a x time view
						[
								'$match': [
										'$and': [[
												createDate: [
														'$gt': minLimit.time
												]], [
												createDate: [
														'$lt': yesterday.time
												]]
										]
								]
						],
						[
								'$project': [
										_id: 0,
										createDate: 1,
										"fellowPlayers.championId": 1,
										gameId: 1
								]
						],
						[
								'$unwind': '$fellowPlayers'
						],
						[
								'$group': [
										_id: [
												createDate: [
														'$subtract': ['$createDate', [
																'$mod': [
																		'$createDate', TimeUnit.DAYS.toMillis(1)
																]
														]]
												],
												championId: '$fellowPlayers.championId',
												gameId: '$gameId'
										]
								]
						],
						[
								'$group': [
										_id: [
												createDate: '$_id.createDate',
												championId: '$_id.championId',
										],
										count: [
												'$sum': 1
										]
								]
						]
				]
		] as BasicDBObject).result.each {
			def gameDate = new Date(it._id.createDate)
			def championId = it._id.championId
			if (!graphData.containsKey(championId)) {
				graphData[championId] = [:]
			}
			graphData[championId][gameDate] = it.count
		}

		model.active = [
				region: region,
				season: season
		]

		// For each champion generate the svg
		graphData.each {
			key, value ->
				def champion = Champion.find {
					champion ->
						champion.id == key
				}

				generateLinePlotSvg(champion, minLimit, yesterday, value, gamesPlayed, outputPath)
		}

		model.data = Champion.collect {
			champion ->
				[champion: champion]
		}

		new File(outputPath, "index.html").withWriter {
			templateEngine.process("index", context, it)
		}
}

def generateLinePlotSvg(champion, minDate, maxDate, championData, gamesPlayed, outputPath) {
	def data = new DataTable(Integer, BigDecimal)
	def x = 0
	def date = minDate
	def dates = []

	// Fill the data table
	while (date < maxDate) {
		def count = championData[date] == null ? 0 : championData[date]
		def games = gamesPlayed[date]

		data.add(x++, count / games)
		dates.add(date)
		date = date.next()
	}

	// Customize the plot graph
	def plot = new XYPlot(data)
	plot.plotArea.borderColor = Color.WHITE
	plot.getAxisRenderer(XYPlot.AXIS_X).setMinorTicksVisible(false)
	plot.getAxisRenderer(XYPlot.AXIS_Y).setMinorTicksVisible(false)
	plot.getAxis(XYPlot.AXIS_Y).setRange(0, 1)
	//plot.getAxisRenderer(XYPlot.AXIS_X).setLabel("Date")
	//plot.getAxisRenderer(XYPlot.AXIS_Y).setLabel("# Picks")
	//plot.getPointRenderer(data).setValueVisible(true)
	//plot.getPointRenderer(data).setValueColor(Color.BLUE)

	LineRenderer lines = new DefaultLineRenderer2D();
	plot.setLineRenderer(data, lines);

	def destination = new File(outputPath, "${champion.name()}_pickrate.svg")
	destination.withOutputStream {
		DrawableWriterFactory.instance."image/svg+xml".write(plot, it, 250, 75)
	}
}
