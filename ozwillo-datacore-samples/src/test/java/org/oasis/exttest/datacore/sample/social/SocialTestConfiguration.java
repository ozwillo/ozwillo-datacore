package org.oasis.exttest.datacore.sample.social; // to avoid being scanned by XML-only junit Spring tests

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.mongodb.core.MongoFactoryBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.mongodb.Mongo;
import com.mongodb.WriteConcern;

/**
 * Spring configuration for testing Neo4j container impl
 */
@Configuration
@ComponentScan(
        basePackages = {"org.oasis.social.data"}
)
@EnableNeo4jRepositories(basePackages = {"org.oasis.social.data"})
@EnableTransactionManagement
@ImportResource("classpath:oasis-datacore-crm-test-context.xml")
public class SocialTestConfiguration extends Neo4jConfiguration {

   // duplicate with XML conf
    /*public @Bean
    MongoTemplate mongoTemplate(Mongo mongo) {
        MongoTemplate template = new MongoTemplate(mongo, "test");
        template.setWriteConcern(WriteConcern.JOURNALED);
        return template;
    }

    public @Bean
    MongoFactoryBean mongo() {
        MongoFactoryBean mongo = new MongoFactoryBean();
        mongo.setHost("localhost");
        return mongo;
    }*/


    public @Bean
    GraphDatabaseService graphDatabaseService() {
        GraphDatabaseFactory gdf = new GraphDatabaseFactory();

        Map<String,String> config = new HashMap<String,String>();
        config.put("enable_remote_shell", "port=1234");
        GraphDatabaseService gds =
                gdf.newEmbeddedDatabaseBuilder("target/neo4j")
                        .setConfig(config)
                        .newGraphDatabase();

        return gds;
    }

}
