# DEPRECATED 
This project was the original prototype

## Ozwillo Datacore Samples

This project contains various technical experimentations, tests and examples, as well as business-oriented examples.
Its goal is to help emergence of Datacore's API and implementation in a use case-driven manner.

Overview (not exhaustive) :

### Technical base :
* a maven project configuration for Spring Data on MongDB, with junit testing & sfl4j/log4j testing
* a working MongoDB Java driver configuration, with (indexed) query performance tests in MongoDBPerfTest
* a working Spring Data on MongoDB configuration, with generic entity / DAO (and some Spring Repository testing), tested by CrmTest (entities : Contact, with versioning & audit)

### Technical features :
* a sample Transaction algorithm in Spring Data on MongoDB (with reusable stuff like Transaction), tested in TransactionTest
* an example of using Spring Converters, Events and custom Mapping, tested by ConverterAndEventTest (entities : Document & Example)

### Business use case-oriented :
* an example of multi-master data sharing and SPARQL-like querying in MongoDB in SharedDataTest (native model : city & organizations). Note that however data will be shared in a single-master configurations (i.e. with a single, all-powerful root owner, rather than in a p2p model).
* an OpenElec-based example of introducing the use of a DatacoreClient in an existing business application, tested in OpenElecMongo(Ozwillo(Bureau)City)ImplTest : 1. listing datacore objects, 2. referencing them locally, 3. having local enriched versions of them
* an example of using a metamodel to maintain denormalized, embedded copies of attributes and resources for rich SPARQL-like querying (OpenElec-inspired model : country, city, bureauDeVote, inseeVille), tested in OpenelecDatacoreTest

### Others :
* examples of various RDF representations : rdf/xml, n3, json-ld... in src/test/resources/rdf


