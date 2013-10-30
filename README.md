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


Getting Started
---------------

Requirements : [Java JDK 7](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html), [MongoDB](http://docs.mongodb.org/manual/installation/)

Build ([Maven 3](http://maven.apache.org/download.cgi) required) : at root, do : mvn clean install

Deployment ([Tomcat 7](http://tomcat.apache.org/download-70.cgi) required) : put the war contents in a tomcat 7 root and start it :

    cd tomcat7/bin
    cp -rf ../../workspace/oasis-datacore/oasis-datacore-web/target/datacore/* ../webapps/ROOT/
    ./catalina.sh start

Then to have a look at API docs, open a browser at http://localhost:8080/swagger-ui/index.html , specify http://localhost:8080/api-docs and click "explore". To try it out, for instance do a GET /dc/type/city to see what cities are available.

NB. alternatively, there is an embedded Jetty container deploying at http://localhost:8180 : mvn -Pjetty:run clean install


Documentation
--------------

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

TODO not yet shown : champs map & list, types multiples / mixins, optimistic locking et cache client par ETag, range & list queries, méthodes tunnelées on GET, SPARQL, à quoi ça ressemble une fois stocké dans mongo...

FAQ
---


For developers
--------------


Release Notes - 0.1
-------------------
First release.


