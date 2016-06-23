package org.oasis.datacore.rest.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis.datacore.common.context.SimpleRequestContextProvider;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.DatacoreApi;
import org.oasis.datacore.rest.client.DatacoreCachedClient;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.sample.CityCountrySample;
import org.oasis.datacore.server.uri.BadUriException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.ws.rs.NotFoundException;
import java.util.Arrays;
import java.util.Collections;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertNotNull;

/**
 * User: schambon
 * Date: 2/17/16
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:oasis-datacore-rest-server-test-context.xml" })
public class AliasingBehaviourTest {

    @Autowired
    private CityCountrySample cityCountrySample;

    @Autowired
    @Qualifier("datacoreApiCachedJsonClient")
    private DatacoreCachedClient datacoreApiClient;

    @Value("${datacoreApiClient.containerUrl}")
    private String containerUrl;

    @Before
    public void before() {
        // set sample project :
        SimpleRequestContextProvider.setSimpleRequestContext(new ImmutableMap.Builder<String, Object>()
                .put(DatacoreApi.PROJECT_HEADER, DCProject.OASIS_SAMPLE).build());
    }

    @Test
    public void createAlias() throws ResourceException, BadUriException {
        cityCountrySample.initData();

        String prefix = containerUrl + "/dc/type/" + CityCountrySample.CITY_MODEL_NAME;

        DCResource resource = datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME, "France/Lyon");
        assertNotNull(resource);

        assertEquals(Collections.singletonList(prefix + "/France/Lyon"),
                datacoreApiClient.getAliases(CityCountrySample.CITY_MODEL_NAME, "France/Lyon"));

        // let's try updating the resource
        resource.set("city:i18nname", DCResource.listBuilder()
            .add(DCResource.propertiesBuilder().put("@language", "fr").put("@value", "Lugdunum").build())
            .build());
        datacoreApiClient.putDataInType(resource, CityCountrySample.CITY_MODEL_NAME, "France/Lyon");

        DCResource updatedResource = datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME, "France/Lyon");
        assertEquals(prefix + "/France/Lugdunum", updatedResource.getUri());
        Long version = updatedResource.getVersion();

        // check that the "new" uri points to the same resource, too, with the same version number
        updatedResource = datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME, "France/Lugdunum");
        assertEquals(prefix + "/France/Lugdunum", updatedResource.getUri());
        assertEquals(version, updatedResource.getVersion());

        assertEquals(Arrays.asList(prefix + "/France/Lugdunum", prefix + "/France/Lyon"),
                datacoreApiClient.getAliases(CityCountrySample.CITY_MODEL_NAME, "France/Lyon"));
        assertEquals(Arrays.asList(prefix + "/France/Lugdunum", prefix + "/France/Lyon"),
                datacoreApiClient.getAliases(CityCountrySample.CITY_MODEL_NAME, "France/Lugdunum"));

        // update again!
        updatedResource.set("city:i18nname", DCResource.listBuilder()
            .add(DCResource.propertiesBuilder().put("@language", "fr").put("@value", "Leo").build())
            .build());
        datacoreApiClient.putDataInType(updatedResource, CityCountrySample.CITY_MODEL_NAME, "France/Lyon");// notice that we use /Lyon: it's all the same really'
        updatedResource = datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME, "France/Lyon");

        // make sure the version has been incemented
        assertEquals(new Long(version + 1), updatedResource.getVersion());
        assertEquals(prefix + "/France/Leo", updatedResource.getUri());

        assertEquals(Arrays.asList(prefix + "/France/Leo", prefix + "/France/Lugdunum", prefix + "/France/Lyon"),
                datacoreApiClient.getAliases(CityCountrySample.CITY_MODEL_NAME, "France/Lyon"));
        assertEquals(Arrays.asList(prefix + "/France/Leo", prefix + "/France/Lugdunum", prefix + "/France/Lyon"),
                datacoreApiClient.getAliases(CityCountrySample.CITY_MODEL_NAME, "France/Lugdunum"));
        assertEquals(Arrays.asList(prefix + "/France/Leo", prefix + "/France/Lugdunum", prefix + "/France/Lyon"),
                datacoreApiClient.getAliases(CityCountrySample.CITY_MODEL_NAME, "France/Leo"));

        // try looping back to Lyon, make sure that all aliases are still there
        updatedResource.set("city:i18nname", DCResource.listBuilder()
            .add(DCResource.propertiesBuilder().put("@language", "fr").put("@value", "Lyon").build())
            .build());
        datacoreApiClient.putDataInType(updatedResource, CityCountrySample.CITY_MODEL_NAME, "France/Leo");
        updatedResource = datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME, "France/Leo");

        assertEquals(new Long(version + 2), updatedResource.getVersion());
        assertEquals(prefix + "/France/Lyon", updatedResource.getUri());

        // check that all the uris we have used are still valid
        assertEquals(prefix + "/France/Lyon", datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME, "France/Lyon").getUri());
        assertEquals(prefix + "/France/Lyon", datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME, "France/Lugdunum").getUri());
        assertEquals(prefix + "/France/Lyon", datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME, "France/Leo").getUri());

        // finally try an update on country field
        updatedResource.set("city:inCountry", "UK");
        datacoreApiClient.putDataInType(updatedResource, CityCountrySample.CITY_MODEL_NAME, "France/Leo");
        updatedResource = datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME, "France/Leo");

        assertEquals(new Long(version + 3), updatedResource.getVersion());
        assertEquals(prefix + "/UK/Lyon", updatedResource.getUri());

        assertEquals(Arrays.asList(prefix + "/UK/Lyon", prefix + "/France/Lyon", prefix + "/France/Leo", prefix + "/France/Lugdunum"),
                datacoreApiClient.getAliases(CityCountrySample.CITY_MODEL_NAME, "France/Lyon"));
        assertEquals(Arrays.asList(prefix + "/UK/Lyon", prefix + "/France/Lyon", prefix + "/France/Leo", prefix + "/France/Lugdunum"),
                datacoreApiClient.getAliases(CityCountrySample.CITY_MODEL_NAME, "France/Lugdunum"));
        assertEquals(Arrays.asList(prefix + "/UK/Lyon", prefix + "/France/Lyon", prefix + "/France/Leo", prefix + "/France/Lugdunum"),
                datacoreApiClient.getAliases(CityCountrySample.CITY_MODEL_NAME, "France/Leo"));
        assertEquals(Arrays.asList(prefix + "/UK/Lyon", prefix + "/France/Lyon", prefix + "/France/Leo", prefix + "/France/Lugdunum"),
            datacoreApiClient.getAliases(CityCountrySample.CITY_MODEL_NAME, "UK/Lyon"));

        // delete the resource
        datacoreApiClient.deleteData(updatedResource);

        // check that all aliases have been deleted too
        ImmutableList.of("France/Lyon", "France/Lugdunum", "France/Leo", "UK/Lyon").stream()
                .forEach(iri -> {
                    try {
                        datacoreApiClient.getData(CityCountrySample.CITY_MODEL_NAME, iri);
                        fail("Should be null!");
                    } catch(NotFoundException e) {
                        // ok
                    }
                    try {
                        datacoreApiClient.getAliases(CityCountrySample.CITY_MODEL_NAME, iri);
                        fail("Should be null!");
                    } catch(NotFoundException e) {
                        // ok
                    }
                });
    }
}
