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
* [IntelliJ IDEA 13+ CE](http://www.jetbrains.com/idea/)

Projects
--------

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

Generating the Site
-------------------

All projects are Maven.  Run `mvn clean install` on each one.

Load data (get latest dump from production) into Mongo using `mongorestore --db live`

Regenerate site from `~/projects/github.com/pyrolistical/bestchampionregion` with command `mvn exec:java -Dexec.mainClass=com.github.best.champion.region.generate_site`

This will output to `~/projects/github.com/concept-not-found/bestchampionregion-pages`

Workflow for Developing New Pages
---------------------------------

1. create static html template under `~/projects/github.com/concept-not-found/bestchampionregion/template`
    * define [Thymeleaf](http://www.thymeleaf.org/) attributes
    * see other *.html pages for examples
2. create Groovy script that fetches the data from Mongo and fills in the template under `~/projects/github.com/pyrolistical/bestchampionregion/src/main/groovy/com/github/best/champion/region`
    * see other generate_*.groovy files for examples
3. run Groovy script with `mvn exec:java -Dexec.mainClass=com.github.best.champion.region.<SCRIPT NAME> -Dexec.arguments=<TEMPLATE DIRECTORY>,<OUTPUT DIRECTORY>`



Tried and tested on Mac OS X, anything else at your peril
