package com.github.best.champion.region.season_3

import com.github.best.champion.region.Champion
import com.github.best.champion.region.Constants
import com.github.concept.not.found.mongo.groovy.util.MongoUtils
import com.mongodb.BasicDBObject
import de.erichseifert.gral.data.DataTable
import de.erichseifert.gral.data.EnumeratedData
import de.erichseifert.gral.io.plots.DrawableWriterFactory
import de.erichseifert.gral.plots.BarPlot
import de.erichseifert.gral.util.Insets2D
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templateresolver.FileTemplateResolver

import java.awt.*
import java.text.FieldPosition
import java.text.Format
import java.text.ParsePosition

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
fileTemplateResolver.setPrefix("$templateDirectory/top/champion/region/season/")
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

def outputPath = new File(outputDirectory, "top/champion/${region.value.path}/${season.value.path}")
outputPath.mkdirs()

MongoUtils.connect {
	mongo ->
		def result = mongo.season_3.command([
				aggregate: "ranked_games",
				pipeline: [
//						[
//								'$limit': 100000
//						],
						[
								'$project': [
										_id: 0,
										championId: 1,
										statistics: 1
								]
						],
						[
								'$unwind': '$statistics'
						],
						[
								'$match': [
										"statistics.name": "TOTAL_DAMAGE_DEALT_TO_CHAMPIONS"
								]
						],
						[
								'$project': [
										championId: 1,
										bin: [
												'$divide': [
														[
																'$subtract': [
																		'$statistics.value',
																		[
																				'$mod': [
																						'$statistics.value',
																						5000
																				]
																		]
																]
														],
														5000
												]
										]
								]
						],
						[
								'$group': [
										_id: [
												championId: '$championId',
												bin: '$bin'
										],
										count: [
												'$sum': 1
										]
								]
						],
						[
								'$project': [
										_id: 0,
										championId: '$_id.championId',
										histogram: [
												bin: '$_id.bin',
												count: '$count'
										]
								]
						],
						[
								'$group': [
										_id: [
												championId: '$championId'
										],
										histogram: [
												'$addToSet': '$histogram'
										]
								]
						]
				]
		] as BasicDBObject).result


		model.active = [
				region: region,
				season: season
		]

		model.data = result.collect {
			def champion = Champion.find {
				champion ->
					champion.id == it._id.championId
			}

			def histogramByBin = [:]
			it.histogram.each {
				bin ->
					def key = bin.bin as int
					if (key >= 11) {
						def existing = histogramByBin[11] ?: 0
						histogramByBin[11] = existing + bin.count
					} else {
						histogramByBin[key] = bin.count
					}
			}
			histogramByBin = histogramByBin.withDefault {
				0
			}
			def countHistogram = (0..11).collect {
				bin ->
					histogramByBin[bin]
			}
			def total = countHistogram.inject {
				left, right ->
					left + right
			}
			def percentageHistogram = countHistogram.collect {
				it / total
			}
			generateHistogramSvg(percentageHistogram, new File(outputPath, "${champion.name()}_damage_histogram_5k.svg"), 1)
			generateHistogramSvg(percentageHistogram, new File(outputPath, "${champion.name()}_damage_histogram_10k.svg"), 0.5)
			[
					champion: champion,
					histogram: percentageHistogram
			]
		}

		model.data.sort {
			champion ->
				def binContribution = []
				champion.histogram.eachWithIndex {
					percentage, bin ->
						binContribution.add(percentage * bin)
				}
				-binContribution.sum()
		}

		new File(outputPath, "index.html").withWriter {
			templateEngine.process("index", context, it)
		}
}

def generateHistogramSvg(percentages, destination, width) {
	def histogramCount = new DataTable(Double)
	def values = percentages
	values.collate(1 / width as int).each {
		histogramCount.add(Math.min(it.sum() as double, 0.3000001))
	}

	def histogram = new EnumeratedData(histogramCount, 2500 / width, 5000 / width)

	def plot = new BarPlot(histogram)

// Format plot
	plot.insets = new Insets2D.Double(0, 2, 5, -15 * width)
	plot.barWidth = 4750 / width

	plot.plotArea.borderColor = Color.WHITE
	plot.plotArea.majorGridY = false

	plot.getAxis(BarPlot.AXIS_X).setRange(0, 65000)
	plot.getAxisRenderer(BarPlot.AXIS_X).minorTicksVisible = false
	plot.getAxisRenderer(BarPlot.AXIS_X).shapeColor = Color.LIGHT_GRAY
	plot.getAxisRenderer(BarPlot.AXIS_X).shapeStroke = new BasicStroke(0.2)
	plot.getAxisRenderer(BarPlot.AXIS_X).tickStroke = new BasicStroke(0)
	plot.getAxisRenderer(BarPlot.AXIS_X).tickFont = new Font("Helvetica Neue", Font.PLAIN, 3)
	plot.getAxisRenderer(BarPlot.AXIS_X).tickSpacing = 5000 / width
	plot.getAxisRenderer(BarPlot.AXIS_X).tickLabelDistance = 0.1
	plot.getAxisRenderer(BarPlot.AXIS_X).tickLabelFormat = new Format() {
		@Override
		public StringBuffer format(Object value, StringBuffer toAppendTo, FieldPosition pos) {
			def thousandthLabel = Math.round(value / 1000)
			if (thousandthLabel == 60) {
				toAppendTo << "∞"
			} else {
				toAppendTo << thousandthLabel
				toAppendTo << "k"
			}
			return toAppendTo
		}

		@Override
		public Object parseObject(String source, ParsePosition pos) {
			throw new UnsupportedOperationException()
		}
	}

	plot.getAxis(BarPlot.AXIS_Y).setRange(0, 0.4)
	plot.getAxisRenderer(BarPlot.AXIS_Y).ticksVisible = false
	plot.getAxisRenderer(BarPlot.AXIS_Y).minorTicksVisible = false
	plot.getAxisRenderer(BarPlot.AXIS_Y).shapeVisible = false

	plot.getPointRenderer(histogram).setColor(new Color(51, 102, 204))
	plot.getPointRenderer(histogram).valueVisible = true
	plot.getPointRenderer(histogram).valueFont = new Font("Helvetica Neue", Font.PLAIN, 3)
	plot.getPointRenderer(histogram).valueDistance = 0.1
	plot.getPointRenderer(histogram).valueFormat = new Format() {
		@Override
		public StringBuffer format(Object value, StringBuffer toAppendTo, FieldPosition pos) {
			def thousandthLabel = Math.round(100 * value)
			if (thousandthLabel != 0) {
				if (value > 0.3) {
					toAppendTo << ">30%"
				} else {
					toAppendTo << thousandthLabel
					toAppendTo << "%"
				}
			}
			return toAppendTo
		}

		@Override
		public Object parseObject(String source, ParsePosition pos) {
			throw new UnsupportedOperationException()
		}
	}


	destination.withOutputStream {
		def pixels_per_inch = 90
		def millimeters_per_inch = 25.4
		DrawableWriterFactory.instance."image/svg+xml".write(plot, it,
				width * millimeters_per_inch * 800 / pixels_per_inch,
				millimeters_per_inch * 100 / pixels_per_inch)
	}
}