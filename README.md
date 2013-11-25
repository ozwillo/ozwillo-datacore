===============================
OASIS - Datacore
http://www.oasis-eu.org/
Copyright (c) 2013 Open Wide SA
===============================


About OASIS Datacore
--------------------

Features
   * 

Team
   * Design & Development : Marc Dutoo, Open Wide SA - http://www.openwide.fr

License
   * LGPL


Getting Started with the server
-------------------------------

Requirements : [Java JDK 7](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html), [MongoDB](http://docs.mongodb.org/manual/installation/)

Build ([Maven 3](http://maven.apache.org/download.cgi) required) : at root, do : mvn clean install

Deployment : go in the oasis-datacore-web subproject and do : mvn jetty:run

Then go have a look at API docs at http://localhost:8080/swagger-ui/index.html . To try it out, for instance do a GET /dc/type/city to see what cities are available. For more, see the [wiki](https://github.com/pole-numerique/oasis-datacore/wiki).

Alternatively, to deploy it in production ([Tomcat 7](http://tomcat.apache.org/download-70.cgi) required) : put the war contents in a tomcat 7 root and start it :

    cd tomcat7/bin
    cp -rf ../../workspace/oasis-datacore/oasis-datacore-web/target/datacore/* ../webapps/ROOT/
    ./catalina.sh start


Adding Business Configuration
-----------------------------

To use Datacore for your own collaborative data use case, you must define the appropriate business-specific Models.

There will be soon a Model REST API allowing to do it in JSON / HTTP.

For now this can only be done in Java. Do it in a new class on the model of [MarkaInvestModel](https://github.com/pole-numerique/oasis-datacore/blob/master/oasis-datacore-core/src/main/java/org/oasis/datacore/core/sample/MarkaInvestModel.java) or [CityCountrySample](https://github.com/pole-numerique/oasis-datacore/blob/master/oasis-datacore-core/src/main/java/org/oasis/datacore/core/sample/CityCountrySample.java) (meaning a server-side class, auto initialized by annotating it by @Component, using Spring-injected DataModelServiceImpl and DatacoreApi or DCEntityService, or if they are not enough yet MongoOperations).

Sample data for these models can be added using the Datacore REST API obviously, or again using Java n a new class on the model of [MarkaInvestData](https://github.com/pole-numerique/oasis-datacore/blob/master/oasis-datacore-core/src/main/java/org/oasis/datacore/core/sample/MarkaInvestData.java).

For more samples and Model reference documentation, have a look at the [wiki](https://github.com/pole-numerique/oasis-datacore/wiki).


Using it from a client business application
-------------------------------------------

Use the JSON/HTTP client of your own business application's platform and / or of your choice to call the DatacoreApi server using REST JSON/HTTP calls. Here are such clients that might help you :

- A **Java proxy-like cached client built on the CXF service engine** is provided by the oasis-datacore-rest-cxf subproject. Use it by loading its Spring (https://github.com/pole-numerique/oasis-datacore/blob/master/oasis-datacore-rest-cxf/src/main/resources/oasis-datacore-rest-client-context.xml) and injecting DatacoreClientApi using ```@Autowired @Qualifier("datacoreApiCachedClient") private DatacoreClientApi datacoreApiClient;``` like done in https://github.com/pole-numerique/oasis-datacore/blob/master/oasis-datacore-rest-cxf/src/test/java/org/oasis/datacore/rest/api/client/DatacoreApiCXFClientTest.java .

- If it doesn't suit you, **other Java service engines** (such as Jersey, RESTEasy) may be used in similar fashion, though some features (HTTP ETag-based caching, generic query request) require developing interceptors / handlers / filters. Otherwise, the **Java JAXRS web client** works well with the DatacoreApi and allows to do everything, though it is a bit more "barebone".

- In other languages, you can use [Swagger codegen](https://github.com/wordnik/swagger-codegen) to generate DatacoreApi clients to various languages : **php, ruby, python, Objective C, Flash...**

At worst, you can talk to DatacoreApi server by writing the right JSON/HTTP requests, sending them to it and handling their responses.


Documentation
--------------

See online API (ask URL to Pole Numerique) and [wiki](https://github.com/pole-numerique/oasis-datacore/wiki).

Still missing :

* a more formal description of (the metamodel of) supported data resources : what field types are supported, how to model them, how they behave (especially Map, List and external vs embedded Resource fields types) ; and wider, documentation of the metamodel itself : its fields, Mixin types (soon to be supported)...
* more samples (a single one for now)


Goals :

* une API compatible RDF
 - grâce à une représentation "presque" JSON-LD qui est nativement compatible RDF,
 - des requêtes d'interrogation de type W3C LDP. Par exemple pour toutes les viles de france triées par nom et de population supérieure à 500000 (avec par défaut un maximum de 50 résultats) :
http://data.oasis-eu.org/city?inCountry=http://data.oasis-eu.org/country/France&name=+&population=>500000
 - mais aussi un mini-SPARQL bâti sur ces dernières)
* qui permette les exemples des cas d'usage des slides du workshop de Valence.

Cependant, elle est orientée pour tenir compte des différences suivantes par rapport à une implémentation native de RDF :

* toutes les données ne sont pas égales : le "type" de données (rdf:type des ressources JSON-LD) devient "citoyen de première class", correspondant à un usage mais aussi à un choix de gouvernance (et en effet il ne faut surtout pas mélanger des données qui diffèrent sur ces points)
* la granularité du triplet RDF est trop fine pour être performante, aisément manipulable et modélisable, et compréhensible par les providers => granularité de type "aspect" / "mixin", correspondant à un namespace de prédicat RDF
* permettre des propriétés complexes (listes et maps)

Samples :

* [city & country](https://github.com/pole-numerique/oasis-datacore/wiki/Tutorial---city-&-country)

TODO not yet shown : Mixin (light, reusable, multiple) types, client-side cache using version as ETag, range & list queries, OPTIONAL tunneled methods on GET, LATER SPARQL, details on how it is stored in MongoDB...


FAQ
---
See [wiki](https://github.com/pole-numerique/oasis-datacore/wiki).


For developers
--------------
See [wiki](https://github.com/pole-numerique/oasis-datacore/wiki).


Release Notes - 0.5
-------------------
First release.


