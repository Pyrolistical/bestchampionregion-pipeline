Getting Started
===============

Tools
-----

* [Java JDK 7u45+](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Homebrew](http://brew.sh/)
* Git 1.8.3.4+ `brew install git`
* Groovy 2.2.1+ `brew install groovy`
* Maven 3.0.5+ `brew install maven`
* Mongo 2.4.8+ `brew install mongo`
* 7zip `brew install p7zip`
* [IntelliJ IDEA 13+ CE](http://www.jetbrains.com/idea/)
* [Brackets](http://brackets.io/)

Projects
--------

* [https://github.com/Pyrolistical/bestchampionregion](https://github.com/Pyrolistical/bestchampionregion)
* [https://github.com/concept-not-found/regulache](https://github.com/concept-not-found/regulache)
* [https://github.com/concept-not-found/http-regulator](https://github.com/concept-not-found/http-regulator)
* [https://github.com/concept-not-found/mongo-groovy-extension](https://github.com/concept-not-found/mongo-groovy-extension)
* [https://github.com/concept-not-found/bestchampionregion](https://github.com/concept-not-found/bestchampionregion)


Folder Structure
----------------

generate_site.groovy requires the template directory and output directories to relative to where it is run.

The following folder structure will work:

    ~/projects/github.com
      ↳ concept-not-found
          ↳ regulache
          ↳ http-regulator
          ↳ mongo-groovy-extension
          ↳ bestchampionregion (master branch)
          ↳ bestchampionregion-pages (gh-pages branch)
      ↳ pyrolistical
          ↳ bestchampionregion

Season 4
========

There is no website for Season 4 yet.  Just collecting diamond 5 and better data.

Download [season 4 development dump](https://www.dropbox.com/s/rxmdvf31p6j6gfp/dump-1390118531607.7z)

Load data with the command `mvn exec:java -Dexec.mainClass=com.github.best.champion.region.season_4.import_development_dump -Dexec.arguments=dump-1390118531607.7z`


Season 3
========

Generating the Site
-------------------

All projects are Maven.  Run `mvn clean install` on each one.  Each Maven project can be import a modules in the same IntelliJ project.

Download [season 3 development dump](https://www.dropbox.com/sh/vfin51td8flytg9/RMVDIynDdP)

Load data with the command `mvn exec:java -Dexec.mainClass=com.github.best.champion.region.season_3.import_development_dump -Dexec.arguments=dump-1388892960491.7z`

Regenerate site from `~/projects/github.com/pyrolistical/bestchampionregion` with command `mvn exec:java -Dexec.mainClass=com.github.best.champion.region.season_3.generate_site`

This will output to `~/projects/github.com/concept-not-found/bestchampionregion-pages`

Workflow for Developing New Pages
---------------------------------

1. create static html template under `~/projects/github.com/concept-not-found/bestchampionregion/template`
    * Brackets is good for html
    * define [Thymeleaf](http://www.thymeleaf.org/) attributes
    * see other *.html pages for examples
2. create Groovy script that fetches the data from Mongo and fills in the template under `~/projects/github.com/pyrolistical/bestchampionregion/src/main/groovy/com/github/best/champion/region`
    * see other generate_*.groovy files for examples
3. run Groovy script with `mvn exec:java -Dexec.mainClass=com.github.best.champion.region.season_3.<SCRIPT NAME> -Dexec.arguments=<TEMPLATE DIRECTORY>,<OUTPUT DIRECTORY>`

Tried and tested on Mac OS X, anything else at your peril
