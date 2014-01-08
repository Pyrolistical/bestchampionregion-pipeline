package com.github.best.champion.region

def enum Champion {
	Aatrox(266, "Aatrox"),
	Ahri(103, "Ahri"),
	Akali(84, "Akali"),
	Alistar(12, "Alistar"),
	Amumu(32, "Amumu"),
	Anivia(34, "Anivia"),
	Annie(1, "Annie"),
	Ashe(22, "Ashe"),
	Blitzcrank(53, "Blitzcrank"),
	Brand(63, "Brand"),
	Caitlyn(51, "Caitlyn"),
	Cassiopeia(69, "Cassiopeia"),
	Chogath(31, "Cho'Gath"),
	Corki(42, "Corki"),
	Darius(122, "Darius"),
	Diana(131, "Diana"),
	DrMundo(36, "Dr. Mundo"),
	Draven(119, "Draven"),
	Elise(60, "Elise"),
	Evelynn(28, "Evelynn"),
	Ezreal(81, "Ezreal"),
	FiddleSticks(9, "Fiddlesticks"),
	Fiora(114, "Fiora"),
	Fizz(105, "Fizz"),
	Galio(3, "Galio"),
	Gangplank(41, "Gangplank"),
	Garen(86, "Garen"),
	Gragas(79, "Gragas"),
	Graves(104, "Graves"),
	Hecarim(120, "Hecarim"),
	Heimerdinger(74, "Heimerdinger"),
	Irelia(39, "Irelia"),
	Janna(40, "Janna"),
	JarvanIV(59, "Jarvan IV"),
	Jax(24, "Jax"),
	Jayce(126, "Jayce"),
	Jinx(222, "Jinx"),
	Karma(43, "Karma"),
	Karthus(30, "Karthus"),
	Kassadin(38, "Kassadin"),
	Katarina(55, "Katarina"),
	Kayle(10, "Kayle"),
	Kennen(85, "Kennen"),
	Khazix(121, "Kha'Zix"),
	KogMaw(96, "Kog'Maw"),
	Leblanc(7, "LeBlanc"),
	LeeSin(64, "Lee Sin"),
	Leona(89, "Leona"),
	Lissandra(127, "Lissandra"),
	Lucian(236, "Lucian"),
	Lulu(117, "Lulu"),
	Lux(99, "Lux"),
	Malphite(54, "Malphite"),
	Malzahar(90, "Malzahar"),
	Maokai(57, "Maokai"),
	MasterYi(11, "Master Yi"),
	MissFortune(21, "Miss Fortune"),
	Mordekaiser(82, "Mordekaiser"),
	Morgana(25, "Morgana"),
	Nami(267, "Nami"),
	Nasus(75, "Nasus"),
	Nautilus(111, "Nautilus"),
	Nidalee(76, "Nidalee"),
	Nocturne(56, "Nocturne"),
	Nunu(20, "Nunu"),
	Olaf(2, "Olaf"),
	Orianna(61, "Orianna"),
	Pantheon(80, "Pantheon"),
	Poppy(78, "Poppy"),
	Quinn(133, "Quinn"),
	Rammus(33, "Rammus"),
	Renekton(58, "Renekton"),
	Rengar(107, "Rengar"),
	Riven(92, "Riven"),
	Rumble(68, "Rumble"),
	Ryze(13, "Ryze"),
	Sejuani(113, "Sejuani"),
	Shaco(35, "Shaco"),
	Shen(98, "Shen"),
	Shyvana(102, "Shyvana"),
	Singed(27, "Singed"),
	Sion(14, "Sion"),
	Sivir(15, "Sivir"),
	Skarner(72, "Skarner"),
	Sona(37, "Sona"),
	Soraka(16, "Soraka"),
	Swain(50, "Swain"),
	Syndra(134, "Syndra"),
	Talon(91, "Talon"),
	Taric(44, "Taric"),
	Teemo(17, "Teemo"),
	Thresh(412, "Thresh"),
	Tristana(18, "Tristana"),
	Trundle(48, "Trundle"),
	Tryndamere(23, "Tryndamere"),
	TwistedFate(4, "Twisted Fate"),
	Twitch(29, "Twitch"),
	Udyr(77, "Udyr"),
	Urgot(6, "Urgot"),
	Varus(110, "Varus"),
	Vayne(67, "Vayne"),
	Veigar(45, "Veigar"),
	Vi(254, "Vi"),
	Viktor(112, "Viktor"),
	Vladimir(8, "Vladimir"),
	Volibear(106, "Volibear"),
	Warwick(19, "Warwick"),
	MonkeyKing(62, "Wukong"),
	Xerath(101, "Xerath"),
	XinZhao(5, "Xin Zhao"),
	Yasuo(157, "Yasuo"),
	Yorick(83, "Yorick"),
	Zac(154, "Zac"),
	Zed(238, "Zed"),
	Ziggs(115, "Ziggs"),
	Zilean(26, "Zilean"),
	Zyra(143, "Zyra")
	
	def id
	def label
	def path

	def Champion(id, label) {
		this.id = id
		this.label = label
		this.path = label.toLowerCase().replace("'", "-").replace(" ", "-").replace(".", "")
	}

}
