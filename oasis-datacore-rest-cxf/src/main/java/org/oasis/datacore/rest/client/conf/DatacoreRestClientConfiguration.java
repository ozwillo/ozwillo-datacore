package org.oasis.datacore.rest.client.conf;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import java.util.ArrayList;
import java.util.List;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.oasis.datacore.rest.client.DatacoreClientApi;
import org.oasis.datacore.rest.client.cxf.ETagClientOutInterceptor;
import org.oasis.datacore.rest.client.cxf.QueryParametersClientOutInterceptor;
import org.oasis.datacore.rest.client.cxf.TextResponseExceptionMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

/**
 * Created 9/23/13 6:34 PM
 *
 * @author jguillemotte
 */
@Configuration
@EnableCaching //See http://stackoverflow.com/questions/8418126/non-xml-version-of-cacheannotation-driven
@ComponentScan(basePackages = {"org.oasis.datacore.rest.client"}, excludeFilters = @ComponentScan.Filter(Configuration.class))
@ImportResource("classpath:META-INF/cxf/cxf.xml")
@PropertySource("classpath:oasis-datacore-rest-client.properties") //See http://www.javacodegeeks.com/2013/07/spring-bean-and-propertyplaceholderconfigurer.html
public class DatacoreRestClientConfiguration {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        return configurer;
    }

    @Bean
    public static EhCacheManagerFactoryBean ehCacheManagerFactoryBean() throws Exception {
        EhCacheManagerFactoryBean ehCacheManagerFactoryBean = new EhCacheManagerFactoryBean();
        ehCacheManagerFactoryBean.setConfigLocation(new ClassPathResource("ehcache.xml"));
        ehCacheManagerFactoryBean.setShared(true);
        ehCacheManagerFactoryBean.setCacheManagerName("datacore.rest.client.cache.rest.api.DCResource");
        ehCacheManagerFactoryBean.afterPropertiesSet();
        return ehCacheManagerFactoryBean;
    }

    @Bean
    public static EhCacheCacheManager cacheManager() throws Exception {
        EhCacheCacheManager cacheManager = new EhCacheCacheManager();
        cacheManager.setCacheManager(ehCacheManagerFactoryBean().getObject());
        return cacheManager;
    }

    @Bean(name = "datacore.rest.client.cache.rest.api.DCResource")
    public static Cache dcResource() throws Exception {
        return cacheManager().getCache("org.oasis.datacore.rest.api.DCResource");
    }

    @Bean(name = "datacoreApiClient")
    public static DatacoreClientApi delegate(
            @Value("${datacoreApiClient.baseUrl}") String baseAddress, //:http://localhost:10080/
            @Value("${datacoreApiClient.containerUrl}") String containerUrl //:http://data-test.oasis-eu.org/
            ) throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        Bus bus = bf.createBus();
        //.createBus("classpath:/org/apache/cxf/systest/jaxrs/security/jaxrs-https.xml");
        BusFactory.setDefaultBus(bus);

        // Set providers and dreate the client
        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider(datacoreApiClientObjectMapper()));
        providers.add(new TextResponseExceptionMapper());
        DatacoreClientApi proxy = JAXRSClientFactory.create(baseAddress, DatacoreClientApi.class, providers);
        // Set interceptors
        ClientConfiguration config = WebClient.getConfig(proxy);
        config.getOutInterceptors().add(new QueryParametersClientOutInterceptor());
        config.getOutInterceptors().add(new ETagClientOutInterceptor(dcResource(), containerUrl));

        return proxy;
    }

    @Bean(name="datacoreApiClient.objectMapper")
    public static ObjectMapper datacoreApiClientObjectMapper(){
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true);
        return mapper;
    }

}
