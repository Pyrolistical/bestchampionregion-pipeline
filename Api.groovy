class Api {
	def static key() {
		def lol_api_key = System.getProperty("lol_api_key") ?: System.getenv("lol_api_key")

		if (!lol_api_key) {
			throw new IllegalStateException("missing lol_api_key property")
		}

		lol_api_key
	}
}
