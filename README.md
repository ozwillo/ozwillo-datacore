=====================================================

OASIS - Datacore
http://www.oasis-eu.org/
https://github.com/pole-numerique/oasis-datacore
Copyright (c) 2013-2015 Open Wide - http://www.openwide.fr

=====================================================


About OASIS Datacore
--------------------

OASIS Datacore is a Cloud of shared Open Linked Data.

Its goal is cross-business data collaboration and integration. By linking data from different business together, it allows to create value by developing new OASIS services on top of it.

To interact and scale up, it uses Web technologies (HTTP REST API, OAuth2) and architecture (sharding and replication, client-side caching).
To achieve data model flexibility, it follows Semantic Web principles ("almost" W3C JSON-LD compatible), but curbed to fit real world constraints.

Features
   * HTTP REST API for sharing data, with OAuth2 authentication and client-side caching
   * W3C JSON-LD-like data Resource representation, as well as RDF (nquads, turtle)
   * W3C LDP-inspired query filters
   * JSON Schema-like data models with Model (primary) and Mixin (reusable) types. Models are the place where collaboration on data happens. Supported field types are string, boolean, int, float, long, double, date (ISO8601), map, list, i18n (optimized for search on value only), resource (i.e. link).
   * scalable MongoDB storage (sharded cluster ready), Java server (Apache CXF / Spring)
   * Rights (readers, writers, owners) at Resource, Model and business (Scope) levels, with query optimization
   * Historization, allowing a posteriori moderation
   * Approvable changes, up to Contributions (merging Resources from other similar Models)
   * Client libraries : CXF3/Spring3/Java, Spring4/Java (Portal's), Javascript (swagger.js), and all languages that Swagger generates to (see corresponding projects)
   * Online API Playground, documentation and data browsing tool
   * Online model & data Import tool
   * and more upcoming : see [Roadmap](https://github.com/pole-numerique/oasis-datacore/issues)

Team
   * Design & Development of v1 : Marc Dutoo, Open Wide - http://www.openwide.fr
   * Authentication, Historization, Rights API, Contributions : Aurélien Giraudon, Open Wide - http://www.openwide.fr

License : Affero GPL3, except for client libraries which are LGPL3


Getting Started with the server
-------------------------------

Requirements : [Java JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html), [MongoDB 2.6](http://docs.mongodb.org/manual/installation/)

Build ([Maven 3](http://maven.apache.org/download.cgi) required) : at root, do : mvn clean install

Deployment : go in the oasis-datacore-web subproject and do : mvn jetty:run

Then go have a look at API documentation and playground at [http://localhost:8080/dc-ui/index.html](http://localhost:8080/dc-ui/index.html). To try it out, for instance do a GET ``/dc/type/geo:Area_0`` to see what geographical areas (cities...) are available. To learn more about available operations, follow the [wiki Playground tutorial](https://github.com/pole-numerique/oasis-datacore/wiki/Playground-&-Import-UI-demo-scenario---Provto-&-OpenElec) and do the [city & country Tutorial](https://github.com/pole-numerique/oasis-datacore/wiki/Tutorial---city-&-country). To learn about out to use them, have a look at the detailed API doc below.

Alternatively, to deploy it in production ([Tomcat 7](http://tomcat.apache.org/download-70.cgi) required) : put the war contents in a tomcat 7 root and start it :

    cd tomcat7/bin
    cp -rf ../../workspace/oasis-datacore/oasis-datacore-web/target/datacore/* ../webapps/ROOT/
    ./catalina.sh start


Adding Business Configuration
-----------------------------

To use Datacore for your own collaborative data use case, you must define the appropriate business-specific Models.

The preferred way is to use the Datacore online Import tool at [http://localhost:8080/dc-ui/import/index.html](http://localhost:8080/dc-ui/import/index.html). Follow the steps described in the [wiki Import tutorial](https://github.com/pole-numerique/oasis-datacore/wiki/Playground-&-Import-UI-demo-scenario---Provto-&-OpenElec).

This can also be done in Java. Do it in a new class on the model of [MarkaInvestModel](https://github.com/pole-numerique/oasis-datacore/blob/master/oasis-datacore-core/src/main/java/org/oasis/datacore/core/sample/MarkaInvestModel.java) or [CityCountrySample](https://github.com/pole-numerique/oasis-datacore/blob/master/oasis-datacore-core/src/main/java/org/oasis/datacore/core/sample/CityCountrySample.java) (meaning a server-side class, auto initialized by annotating it by @Component, using Spring-injected DataModelServiceImpl and DatacoreApi or DCEntityService, or if they are not enough yet MongoOperations). Sample data for these models can be added using the Datacore REST API obviously, or again using Java in a new class on the model of [MarkaInvestData](https://github.com/pole-numerique/oasis-datacore/blob/master/oasis-datacore-core/src/main/java/org/oasis/datacore/core/sample/MarkaInvestData.java).

For more samples and Model reference documentation, have a look at the [wiki](https://github.com/pole-numerique/oasis-datacore/wiki).


Using it from a client business application
-------------------------------------------

Use the JSON/HTTP client of your own business application's platform and / or of your choice to call the DatacoreApi server using REST JSON/HTTP calls. Learn about all available operations and their parameters in the API documentation below the playground at [http://localhost:8080/dc-ui/index.html](http://localhost:8080/dc-ui/index.html), in the dedicated detailed panels such as the one that opens when clicking on [http://localhost:8080/dc-ui/index.html#!/dc/findDataInType_get_2](http://localhost:8080/dc-ui/index.html#!/dc/findDataInType_get_2).

Here are such clients that might help you :

- A **Java proxy-like cached client built on the CXF service engine** is provided by the oasis-datacore-rest-cxf subproject. Use it by [loading its Spring](https://github.com/pole-numerique/oasis-datacore/blob/master/oasis-datacore-rest-cxf/src/main/resources/oasis-datacore-rest-client-context.xml) and injecting DatacoreCachedClient using ```@Autowired private DatacoreCachedClient datacoreApi;``` like done in [this test](https://github.com/pole-numerique/oasis-datacore/blob/master/oasis-datacore-rest-cxf/src/test/java/org/oasis/datacore/rest/api/client/DatacoreApiCXFClientTest.java).

- A **simpler Java client built on Spring 4 with REST Template** is provided, along with a similar Kernel client, by the [oasis-spring-integration](https://github.com/pole-numerique/oasis-spring-integration) top-level project. See how the portal [uses it to query geographical Resource](https://github.com/pole-numerique/oasis-portal/blob/master/portal-parent/oasis-portal-front/src/main/java/org/oasis_eu/portal/core/mongo/dao/geo/GeographicalAreaCache.java#L226) and [how he configures it](https://github.com/pole-numerique/oasis-portal/blob/master/portal-parent/oasis-portal-front/src/main/resources/application.yml#L88).

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


Tips for integrating it in a client business application, from a functional point of view
-----------------------------------------------------------------------------------------
- **painless data replacement** - put static data in the Datacore : all static data, such as a list of region names contained in a property file, or authorized values for a field in the database that are hardcoded in business code, can easily and without further impact be replaced by a heavily (each week, sync unless performance problems) globally cached call to Datacore.
- **painless data linking** - to integrate free form data (such as common information input forms) with Datacore data : use a suggestion UI component (such as select2 in Javascript), so that the user will be inclined to choose one already existing (in the Datacore !) item, rather than typing it all out with the risks of typing mistakes or adding a redundant but slightly differently worded value. This way, we are reusing the user's knowledge to link data, rather than having to reprocess it all afterwards. And the business database has not to be structured or constrained any further than adding a column for linked Datacore Resources.
- **easy data dynamic upload** - best practices to upload your data from your application : beyond initial manual data import using ex. Datacore online Import tool while designing models, when your application has to upload data, be it in a startup / management phase or during a user request, do for each piece of data in your database :
 - 1. if it already has a value in its newly added Datacore Resource URI column, GET it, if version is up to date you're ready to upload (don't forget to upgrade your local Datacore Resource version column afterwards), otherwise handle conflict (see later).
 - 2. else try to find a corresponding Resource that already exists in the Datacore, either by building its id (if not blackbox) out of fields (using indexInId model property) and doing a GET on its URI, or by looking it up using one or more query on deduplicating fields (defined in queryNames model property). If found, store URI and handle conflict (see later), else build a new id if not done yet, store URI and upload (don't forget to set your local Datacore Resource version column to 0 afterwards).
- handling conflict : online tools are being developed for that. Until then, or if they don't fit for you, you have to develop your own, at worst by adding below your data form two links : "view local version", "view remote version", and a button "choose this one".


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


