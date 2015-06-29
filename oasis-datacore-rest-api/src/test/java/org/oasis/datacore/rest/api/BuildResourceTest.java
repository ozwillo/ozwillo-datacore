package org.oasis.datacore.rest.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.oasis.datacore.rest.api.util.UriHelper;

public class BuildResourceTest {

   private String containerUrlString = "http://data.ozwillo.com/";
   private URI containerUrl;
   
   @Before
   public void init() throws URISyntaxException {
      containerUrl = new URI(containerUrlString);
   }
   
   @Test
   public void testBuild() {
      String modelType = "alt.tourism.placeKind";
      final String kind = "hotel";
      @SuppressWarnings("serial")
      DCResource r = build(modelType, kind, new HashMap<String,Object>() {{
         put("name", kind);
      }});
      Assert.assertNotNull(r);
      Assert.assertEquals(r.getUri(), this.containerUrl + "dc/type/" + modelType + '/' + kind);
      Assert.assertEquals(r.getProperties().get("name"), kind);
   }

   @Test
   public void testFluentCreate() {
      String modelType = "alt.tourism.placeKind";
      String kind = "winery";
      DCResource r = DCResource.create(this.containerUrl, modelType, kind).set("name", kind);
      Assert.assertNotNull(r);
      Assert.assertEquals(r.getUri(), this.containerUrl + "dc/type/" + modelType + '/' + kind);
      Assert.assertEquals(r.getProperties().get("name"), kind);
   }

   @Test
   public void testFluentCreateService() {
      String modelType = "alt.tourism.placeKind";
      String kind = "monastery";
      DCResource r = create(modelType, kind).set("name", kind);
      Assert.assertNotNull(r);
      Assert.assertEquals(r.getUri(), this.containerUrl + "dc/type/" + modelType + '/' + kind);
      Assert.assertEquals(r.getProperties().get("name"), kind);
   }

   
   private DCResource create(String modelType, String iri) {
      return DCResource.create(this.containerUrl, modelType, iri);
   }
   
   private DCResource build(String type, String iri, Map<String,Object> fieldValues) {
      DCResource resource = new DCResource();
      resource.setUri(UriHelper.buildUri(containerUrl, type, iri));
      //resource.setVersion(-1l);
      resource.setProperty("type", type);
      resource.setProperty("iri", iri);
      for (Map.Entry<String, Object> fieldValueEntry : fieldValues.entrySet()) {
         resource.setProperty(fieldValueEntry.getKey(), fieldValueEntry.getValue());
      }
      return resource;
   }
   
}
