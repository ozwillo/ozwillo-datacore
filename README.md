===============================
OASIS - Datacore
http://www.oasis-eu.org/
Copyright (c) 2013 Open Wide SA
===============================


About OASIS Datacore
--------------------

OASIS Datacore's business is collaboration on linked data.

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

Then go have a look at API docs at http://localhost:8080/swagger-ui/index.html . To try it out, for instance do a GET /dc/type/sample.city.city to see what cities are available. For more, see the [wiki](https://github.com/pole-numerique/oasis-datacore/wiki).

Alternatively, to deploy it in production ([Tomcat 7](http://tomcat.apache.org/download-70.cgi) required) : put the war contents in a tomcat 7 root and start it :

    cd tomcat7/bin
    cp -rf ../../workspace/oasis-datacore/oasis-datacore-web/target/datacore/* ../webapps/ROOT/
    ./catalina.sh start


Adding Business Configuration
-----------------------------

To use Datacore for your own collaborative data use case, you must define the appropriate business-specific Models.

There will be soon a Model REST API allowing to do it in JSON / HTTP.

For now this can only be done in Java. Do it in a new class on the model of [MarkaInvestModel](https://github.com/pole-numerique/oasis-datacore/blob/master/oasis-datacore-core/src/main/java/org/oasis/datacore/core/sample/MarkaInvestModel.java) or [CityCountrySample](https://github.com/pole-numerique/oasis-datacore/blob/master/oasis-datacore-core/src/main/java/org/oasis/datacore/core/sample/CityCountrySample.java) (meaning a server-side class, auto initialized by annotating it by @Component, using Spring-injected DataModelServiceImpl and DatacoreApi or DCEntityService, or if they are not enough yet MongoOperations).

Sample data for these models can be added using the Datacore REST API obviously, or again using Java in a new class on the model of [MarkaInvestData](https://github.com/pole-numerique/oasis-datacore/blob/master/oasis-datacore-core/src/main/java/org/oasis/datacore/core/sample/MarkaInvestData.java).

For more samples and Model reference documentation, have a look at the [wiki](https://github.com/pole-numerique/oasis-datacore/wiki).


Using it from a client business application
-------------------------------------------

Use the JSON/HTTP client of your own business application's platform and / or of your choice to call the DatacoreApi server using REST JSON/HTTP calls. Here are such clients that might help you :

- A **Java proxy-like cached client built on the CXF service engine** is provided by the oasis-datacore-rest-cxf subproject. Use it by [loading its Spring](https://github.com/pole-numerique/oasis-datacore/blob/master/oasis-datacore-rest-cxf/src/main/resources/oasis-datacore-rest-client-context.xml) and injecting DatacoreCachedClient using ```@Autowired private DatacoreCachedClient datacoreApi;``` like done in [this test](https://github.com/pole-numerique/oasis-datacore/blob/master/oasis-datacore-rest-cxf/src/test/java/org/oasis/datacore/rest/api/client/DatacoreApiCXFClientTest.java).

- If it doesn't suit you, **other Java service engines** (such as Jersey, RESTEasy) may be used in similar fashion, though some features (HTTP ETag-based caching, generic query request) require developing interceptors / handlers / filters. Otherwise, the **Java JAXRS web client** works well with the DatacoreApi and allows to do everything, though it is a bit more "barebone".

- in Javascript, you can use either the [swagger.js library](https://github.com/wordnik/swagger-js) (works also in node.js) :

    window.datacoreSwaggerApi = new SwaggerApi({url: "http://localhost:8080/api-docs"});
    datacoreSwaggerApi.build();
    successCallback = function(data) { alert("received " + data.content.data); }
    ...
    datacoreSwaggerApi.apis.city.findDataInType({type:"sample.city.city", name:"London"},  successCallback);

or jQuery [like Citizen Kin does](https://github.com/pole-numerique/oasis-gru/blob/master/oasis-gru-parent/oasis-gru-webapps/oasis-gru-webapps-back/src/main/webapp/WEB-INF/jsp/permissions.jsp) :

    $.get(datacoreRestClientBaseurl + "dc/type/sample.citizenkin.user" + "?lastName=" + encodeURIComponent("Smith"), callback))

- In other languages, you can use [Swagger codegen](https://github.com/wordnik/swagger-codegen) to generate DatacoreApi clients to various languages : **php, ruby, python, Objective C, Flash...**

At worst, you can talk to DatacoreApi server by writing the right JSON/HTTP requests, sending them to it and handling their responses.


Documentation
--------------

See online API (ask URL to Pole Numerique) and [wiki](https://github.com/pole-numerique/oasis-datacore/wiki).

Still missing :

* a more formal description of (the metamodel of) supported data resources : what field types are supported, how to model them, how they behave (especially Map, List and external vs embedded Resource fields types) ; and wider, documentation of the metamodel itself : its fields, Mixin types (soon to be supported)...
* more samples (a single one for now)


Goals :

* RDF compatible
to allow optimal data modeling flexibility (i.e. be able to model any business and support any change), data compatibility (i.e. data homogenization)

 - thanks to an “almost” JSON-LD representation which is natively RDF-compatible,

 - W3C LDP-like REST query filters. For instance, to get all cities of France sorted by name with a population of more than 500000 (with a default maximum of 50 results) :

http://data.oasis-eu.org/dc/type/sample.city.city?inCountry=http://data.oasis-eu.org/dc/type/sample.city.country/France&name=+&population=>500000

 - but also a “mini-SPARQL” built on these last ones


* But at the same time, to optimally support the collaborative linked data business, it has to handle the following differences from a native RDF implementation :

 - all data are not equal : each data Resource has a “primary”, “Model”, “collaboration” type (the first “rdf:type” of JSON-LD resources) which corresponds to its collaborative data business use. Such a “Model” type is born each time a community of service providers agrees on collaborating together on a given kind of data, and will follow the specific governance rules decided by this community. If a new service provider wants to contribute to the same kind of data, either it subscribes to the existing community and rules, possibly even only partly (such as its contributions having to undergo approval before being accepted), or manages to get the community to change them, or it has to do it all separately, which OASIS discourages though. 

 - granularity of the RDF triple is too fine to be efficient, easily handled, modeled or understood by service providers. Therefore an intermediate “Mixin” / aspect-like granularity is promoted, which roughly corresponds to a an RDF predicate's namespace.

 - allow complex properties (lists and maps)


* In order to allow use cases shown at the Valence workshop :

 - URI as id
 - predicate attribute / property values of the required types : string / text (including textual format such as Well-Known Text (WKT) for geolocalization info), boolean, int, float, long, double, date, map (allowing internationalized text), list, resource / reference (including cross container ; allowing classification in categories...)
 - multiple typing, shared types (ex. address), data layers (ex. Cityhalls patching INSEE / IGN data) (allowing having different sources providing different values)
 - per Resource-authorization : each having its own readers, writers, owners Access Control Lists (ACLs) (allowing for personal / confidential data, and granting rights management right)
 - data quality...


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


