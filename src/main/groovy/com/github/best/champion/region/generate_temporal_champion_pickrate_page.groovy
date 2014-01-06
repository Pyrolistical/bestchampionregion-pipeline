package com.github.best.champion.region

import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.mongodb.BasicDBObject
import de.erichseifert.gral.data.DataTable
import de.erichseifert.gral.data.EnumeratedData
import de.erichseifert.gral.io.plots.DrawableWriterFactory
import de.erichseifert.gral.plots.BarPlot
import de.erichseifert.gral.plots.XYPlot
import de.erichseifert.gral.util.Insets2D
import de.erichseifert.gral.plots.lines.LineRenderer
import de.erichseifert.gral.plots.lines.DefaultLineRenderer2D
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templateresolver.FileTemplateResolver

import java.awt.*
import java.text.FieldPosition
import java.text.Format
import java.text.ParsePosition

def TIME_FRAME_DAYS = 14

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
//def yesterday = new Date().minus(1)
def yesterday = new Date(1388161923394).clearTime() // For testing
def minLimit = yesterday.minus(TIME_FRAME_DAYS)

MongoUtils.connect {
    mongo ->
        def result = mongo.live.command([
                aggregate: "ranked_games",
                pipeline: [
                        // Match to restrict games to a x time view
//                      [
//                              '$match': { '$gt/$lt': ?? }
//                      ],
                        [
                                '$project': [
                                        _id: 0,
                                        createDate: 1,
                                        championId: 1,
                                        fellowPlayers: 1
                                ]
                        ],
                        [
                                '$unwind': '$fellowPlayers'
                        ],
                        [
                                '$group': [
                                        _id: [
                                                createDate: '$createDate',
                                                championId: '$championId'
                                        ],
                                        fellowPlayers: [
                                                '$addToSet': '$fellowPlayers.championId'
                                        ]
                                ]
                        ]
                ]
        ] as BasicDBObject).result

        model.active = [
                region: region,
                season: season
        ]

        def gamesPlayed = [:]
        def graphData = [:]
        result.each {
            def gameDate = new Date(it._id.createDate).clearTime()
            if (!gamesPlayed.containsKey(gameDate)) {
                gamesPlayed[gameDate] = 0
            }
            gamesPlayed[gameDate]++

            // Add champions to the data map and count
            it.fellowPlayers.add(it._id.championId)
            it.fellowPlayers.each {
                championId ->
                    if (!graphData.containsKey(championId)) {
                        graphData[championId] = [:]
                    }
                    if (!graphData[championId].containsKey(gameDate)) {
                        graphData[championId][gameDate] = 0
                    }
                    graphData[championId][gameDate]++
            }
        }

        // For each champion generate the svg
        graphData.keySet().each {
            key ->
                def champion = Champion.find {
                    champion ->
                        champion.id == key
                }

                generateLinePlotSvg(champion, minLimit, yesterday, graphData[key], gamesPlayed, outputPath)
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
        while (date <= maxDate) {
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