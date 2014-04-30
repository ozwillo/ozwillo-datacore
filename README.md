=====================================================

OASIS - Datacore
http://www.oasis-eu.org/
https://github.com/pole-numerique/oasis-datacore
Copyright (c) 2013 Open Wide - http://www.openwide.fr

=====================================================


About OASIS Datacore
--------------------

OASIS Datacore is a Cloud of shared Open Linked Data.

Its goal is cross-business data collaboration and integration. By linking data from different business together, it allows to create value by developing new OASIS services on top of it.

To interact and scale up, it uses Web technologies (HTTP REST API, OAuth2) and architecture (sharding and replication, client-side caching).
To achieve data model flexibility, it follows Semantic Web principles ("almost" W3C JSON-LD compatible), but curbed to fit real world constraints.

Features
   * HTTP REST API for sharing data, with OAuth2 authentication, client-side caching and online playground
   * W3C JSON-LD-like data Resource representation
   * W3C LDP-inspired query filters
   * JSON Schema-like data models with Model (primary) and Mixin types. Models are the place where collaboration on data happens.
   * scalable MongoDB storage, Java server (Apache CXF / Spring)
   * Rights (readers, writers, owners) at Resource, Model and business (Scope) levels, with query optimization
   * Historization
   * Approvable changes, up to Contributions (merging Resources from other similar Models)
   * and more upcoming : see [Roadmap](https://github.com/pole-numerique/oasis-datacore/issues)

Team
   * Design & Development of v1 : Marc Dutoo, Open Wide - http://www.openwide.fr
   * Authentication, Historization, Rights API, Contributions : Aurélien Giraudon, Open Wide - http://www.openwide.fr

LGPL License


Getting Started with the server
-------------------------------

Requirements : [Java JDK 7](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html), [MongoDB](http://docs.mongodb.org/manual/installation/)

Build ([Maven 3](http://maven.apache.org/download.cgi) required) : at root, do : mvn clean install

Deployment : go in the oasis-datacore-web subproject and do : mvn jetty:run

Then go have a look at API documentation and playground at http://localhost:8080/swagger-ui/index.html . To try it out, for instance do a GET /dc/type/sample.city.city to see what cities are available. For more, do the [city & country Tutorial](https://github.com/pole-numerique/oasis-datacore/wiki/Tutorial---city-&-country).

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

See online API (ask URL to Pole Numerique or run your own local Datacore) and its packaged examples, [wiki](https://github.com/pole-numerique/oasis-datacore/wiki) and Java tests.

Documentation still can be improved. If something is missing, ask us about it. Especially missing is a more formal description of supported data resources : what field types are supported, how to model them, how they behave (especially Map, List and external vs embedded Resource fields types) ; and wider, documentation of the metamodel itself : field types, Mixin (light, reusable, multiple) types and inheritance, security and mediation configuration...


More
----
See [wiki](https://github.com/pole-numerique/oasis-datacore/wiki) for
* References
* Tutorials
* Samples
* FAQ
* Developer environment


Release Notes - 1.0
-------------------
* more examples, Mixin, Authentication, Historization, Rights checking and API, Contributions


Release Notes - 0.5
-------------------
* Prototype : REST API & Java client, server with Resource parsing and querying according to Model on top of MongoDB storage


