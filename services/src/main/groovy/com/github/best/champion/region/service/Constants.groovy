package com.github.best.champion.region.service

def class Constants {
	def static orderings = asText(
			["best"],
			["worst"]
	)

	def static champions = asText(
			["Aatrox"],
			["Ahri"],
			["Akali"],
			["Alistar"],
			["Amumu"],
			["Anivia"],
			["Annie"],
			["Ashe"],
			["Blitzcrank"],
			["Brand"],
			["Caitlyn"],
			["Cassiopeia"],
			["Chogath", "Cho'Gath"],
			["Corki"],
			["Darius"],
			["Diana"],
			["Draven"],
			["DrMundo", "Dr. Mundo"],
			["Elise"],
			["Evelynn"],
			["Ezreal"],
			["FiddleSticks", "Fiddlesticks"],
			["Fiora"],
			["Fizz"],
			["Galio"],
			["Gangplank"],
			["Garen"],
			["Gragas"],
			["Graves"],
			["Hecarim"],
			["Heimerdinger"],
			["Irelia"],
			["Janna"],
			["JarvanIV", "Jarvan IV"],
			["Jax"],
			["Jayce"],
			["Jinx"],
			["Karma"],
			["Karthus"],
			["Kassadin"],
			["Katarina"],
			["Kayle"],
			["Kennen"],
			["Khazix", "Kha'Zix"],
			["KogMaw", "Kog'Maw"],
			["Leblanc", "LeBlanc"],
			["LeeSin", "Lee Sin"],
			["Leona"],
			["Lissandra"],
			["Lucian"],
			["Lulu"],
			["Lux"],
			["Malphite"],
			["Malzahar"],
			["Maokai"],
			["MasterYi", "Master Yi"],
			["MissFortune", "Miss Fortune"],
			["MonkeyKing", "Wukong"],
			["Mordekaiser"],
			["Morgana"],
			["Nami"],
			["Nasus"],
			["Nautilus"],
			["Nidalee"],
			["Nocturne"],
			["Nunu"],
			["Olaf"],
			["Orianna"],
			["Pantheon"],
			["Poppy"],
			["Quinn"],
			["Rammus"],
			["Renekton"],
			["Rengar"],
			["Riven"],
			["Rumble"],
			["Ryze"],
			["Sejuani"],
			["Shaco"],
			["Shen"],
			["Shyvana"],
			["Singed"],
			["Sion"],
			["Sivir"],
			["Skarner"],
			["Sona"],
			["Soraka"],
			["Swain"],
			["Syndra"],
			["Talon"],
			["Taric"],
			["Teemo"],
			["Thresh"],
			["Tristana"],
			["Trundle"],
			["Tryndamere"],
			["TwistedFate", "Twisted Fate"],
			["Twitch"],
			["Udyr"],
			["Urgot"],
			["Varus"],
			["Vayne"],
			["Veigar"],
			["Vi"],
			["Viktor"],
			["Vladimir"],
			["Volibear"],
			["Warwick"],
			["Xerath"],
			["XinZhao", "Xin Zhao"],
			["Yasuo"],
			["Yorick"],
			["Zac"],
			["Zed"],
			["Ziggs"],
			["Zilean"],
			["Zyra"]
	).sort {
		it.value.name
	}

	def static regions = asText(
			["NA"],
			["EUW"],
			["EUNE"],
			["BR"],
			["TR"],
			["RU"],
			["LAN"],
			["LAS"],
			["OCE"]
	)

	def static seasons = asText(
			["season3", "Season 3"],
			["season4", "Seaons 4"]
	)

	def static leagues = asText(
			["challenger"],
			["diamond-1", "Diamond I"],
			["diamond-2", "Diamond II"],
			["diamond-3", "Diamond III"],
			["diamond-4", "Diamond IV"],
			["diamond-5", "Diamond V"],
			["platinum-1", "Platinum I"],
			["platinum-2", "Platinum II"],
			["platinum-3", "Platinum III"],
			["platinum-4", "Platinum IV"],
			["platinum-5", "Platinum V"],
			["gold-1", "Gold I"],
			["gold-2", "Gold II"],
			["gold-3", "Gold III"],
			["gold-4", "Gold IV"],
			["gold-5", "Gold V"],
			["silver-1", "Silver I"],
			["silver-2", "Silver II"],
			["silver-3", "Silver III"],
			["silver-4", "Silver IV"],
			["silver-5", "Silver V"],
			["Bronze-1", "Bronze I"],
			["bronze-2", "Bronze II"],
			["bronze-3", "Bronze III"],
			["bronze-4", "Bronze IV"],
			["bronze-5", "Bronze V"]
	)

	def static asText(Object... values) {
		def result = [:]
		values.each {
			value ->
				def key = value[0]
				def name = value.size() == 2 ? value[1] : key.capitalize()
				result[key] = [
						name: name,
						path: name.toLowerCase().replace("'", "-").replace(" ", "-")
				]
		}
		result
	}
}
