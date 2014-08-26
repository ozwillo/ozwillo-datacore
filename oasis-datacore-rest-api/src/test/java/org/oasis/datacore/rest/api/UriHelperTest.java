package org.oasis.datacore.rest.api;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;
import org.oasis.datacore.rest.api.util.UriHelper;

public class UriHelperTest {

   private String containerUrl = "http://data.oasis-eu.org/";
   private String testRelativeUri = "dc//type/sample.marka.field//1";
   
   @Test
   public void testNormalizeUrlMode() throws Exception {
      String[] res = UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
            containerUrl + testRelativeUri , containerUrl, true, false);
      Assert.assertEquals(containerUrl, res[0]);
      Assert.assertEquals("dc/type/sample.marka.field/1", res[1]);
   }
   
   @Test
   public void testNormalizeUrlModeHttps() throws Exception {
      String[] res = UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
            "https://data.oasis-eu.org/" + testRelativeUri, containerUrl, true, false);
      Assert.assertEquals("https://data.oasis-eu.org/", res[0]);
      Assert.assertEquals("dc/type/sample.marka.field/1", res[1]);
   }
   
   @Test
   public void testNormalizeUrlModeRelativeURI() throws Exception {
      String[] res = UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
            testRelativeUri, containerUrl, true, false);
      Assert.assertEquals(null, res[0]);
      Assert.assertEquals("dc/type/sample.marka.field/1", res[1]);
   }
   
   @Test
   public void testNormalizeUrlModeFailsOnBadUriChars() throws Exception {
      // see URI spec & http://stackoverflow.com/questions/1547899/which-characters-make-a-url-invalid
      try {
         UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
               containerUrl + "\\", containerUrl, true, false);
         Assert.fail("Should fail on bad URI chars");
      } catch (URISyntaxException e) {
         Assert.assertTrue(true);
      }
   }
   
   @Test
   public void testNormalizeUrlModeAbsoluteUrlFailsOnBadUrl() throws Exception {
      try {
         UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
               "unknown://data.oasis-eu.org/" + testRelativeUri, containerUrl, true, false);
         Assert.fail("Should fail on unknown URL protocol");
      } catch (MalformedURLException e) {
         Assert.assertTrue(true);
      }
   }
   
   @Test
   public void testNormalizeUrlModeAbsoluteUrlFailsOnNonHttpOrSUrl() throws Exception {
      try {
         UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
               "ftp://data.oasis-eu.org/" + testRelativeUri, containerUrl, true, false);
         Assert.fail("Should fail on non HTTP(S) URL");
      } catch (MalformedURLException e) {
         Assert.assertTrue(true);
      }
   }
   
   @Test
   public void testMatchBaseUrlMode() throws Exception {
      // test matchBaseUrlMode BEWARE DOESN'T CHECK URI OR URL CHARS
      String[] res = UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
            containerUrl + testRelativeUri, containerUrl, false, true);
      Assert.assertEquals(containerUrl, res[0]);
      Assert.assertEquals("dc/type/sample.marka.field/1", res[1]);
      
      // test matchBaseUrlMode HTTPS BEWARE DOESN'T CHECK URI OR URL CHARS
      res = UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
            "https://data.oasis-eu.org/dc//type/sample.marka.field//1", containerUrl, false, true);
      Assert.assertEquals("https://data.oasis-eu.org/", res[0]);
      Assert.assertEquals("dc/type/sample.marka.field/1", res[1]);

      // test matchBaseUrlMode absolute URL fails on non HTTP(S) URL BEWARE DOESN'T CHECK URI OR URL CHARS
      try {
         UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
               "ftp://data.oasis-eu.org/" + containerUrl, containerUrl, false, true);
         Assert.fail("Should fail on bad non HTTP(S) URL");
      } catch (MalformedURLException e) {
         Assert.assertTrue(true);
      }
      
      // test matchBaseUrlMode relative URI BEWARE DOESN'T CHECK URI OR URL CHARS
      res = UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
            testRelativeUri, containerUrl, false, true);
      Assert.assertEquals(null, res[0]);
      Assert.assertEquals("dc/type/sample.marka.field/1", res[1]);
      
      // test bare replace mode BEWARE NO CHECKS AND EXPECTS CONTAINER URL
      res = UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
            containerUrl + testRelativeUri, containerUrl, false, false);
      Assert.assertEquals(containerUrl, res[0]);
      Assert.assertEquals(testRelativeUri, res[1]);
   }
   
   @Test
   public void testBuildUri() throws Exception {  
      String modelType = "alt.tourism.placeKind";
      
      // test OK on always characters in modelType ($-_.())
      String uri = UriHelper.buildUri(containerUrl, modelType + "$", "hotel");
      Assert.assertEquals(containerUrl + "dc/type/" + modelType + "$/hotel", uri);

      // test fails on bad practice modelType (characters beyond $-_.())
      try {
         UriHelper.buildUri(containerUrl, modelType + "?", "hotel");
         Assert.fail("? should be bad practice in modelType");
      } catch (IllegalArgumentException iaex) {
         Assert.assertTrue(true);
      }
      
      // TODO check unsafe characters are encoded in iri
   }
   
}
