package org.oasis.datacore.rest.api;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;
import org.oasis.datacore.rest.api.util.UriHelper;

public class UriHelperTest {

   private String containerUrl = "http://data.ozwillo.com";
   private String testRelativeUri = "/dc//type/sample.marka.field//1";
   
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
            "https://data.ozwillo.com" + testRelativeUri, containerUrl, true, false);
      Assert.assertEquals("https://data.ozwillo.com", res[0]);
      Assert.assertEquals("dc/type/sample.marka.field/1", res[1]);
   }
   
   @Test
   public void testNormalizeUrlModeRelativeURI() throws Exception {
      String[] res = UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
            testRelativeUri, containerUrl, true, false);
      Assert.assertEquals("", res[0]);
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
               "unknown://data.ozwillo.com" + testRelativeUri, containerUrl, true, false);
         Assert.fail("Should fail on unknown URL protocol");
      } catch (MalformedURLException e) {
         Assert.assertTrue(true);
      }
   }
   
   @Test
   public void testNormalizeUrlModeAbsoluteUrlFailsOnNonHttpOrSUrl() throws Exception {
      try {
         UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
               "ftp://data.ozwillo.com" + testRelativeUri, containerUrl, true, false);
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
            "https://data.ozwillo.com/dc//type/sample.marka.field//1", containerUrl, false, true);
      Assert.assertEquals("https://data.ozwillo.com", res[0]);
      Assert.assertEquals("dc/type/sample.marka.field/1", res[1]);

      // test matchBaseUrlMode absolute URL fails on non HTTP(S) URL BEWARE DOESN'T CHECK URI OR URL CHARS
      try {
         UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
               "ftp://data.ozwillo.com" + testRelativeUri, containerUrl, false, true);
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
   public void testSafeCharacters() throws Exception {
      String modelType = "alt.tourism.placeKind";

      // NB. modelType checked for URL_ALWAYS_SAFE_CHARACTERS only when not
      // country / language specific is done rather in ModelResourceMappingService
      
      // test OK on always characters in modelType ($-_.())
      String uri = UriHelper.buildUri(containerUrl, modelType + "$", "hotel");
      Assert.assertEquals(containerUrl + DatacoreApi.DC_TYPE_PATH + modelType + "$/hotel", uri);

      // same on ':'
      modelType = "geo:City_0";
      uri = UriHelper.buildUri(containerUrl, modelType, "FR/Paris");
      Assert.assertTrue(uri.contains(modelType));
      
      // same on all safe chars
      String safeChars = "$-_.()" + "+!*'";
      uri = UriHelper.buildUri(containerUrl, modelType, safeChars);
      Assert.assertTrue(uri.contains(safeChars));
      
      // TODO check unsafe characters are encoded in iri
   }

   
   @Test
   public void testEncodedCharacters() throws Exception {
      String modelType = "geo:CityGroup_0";
      
      // test that reserved chars are not encoded in path (except obviously ?)
      String uri = UriHelper.buildUri(containerUrl, modelType, "FR/CC les Châteaux"); // "http://data.ozwillo.com/dc/type/geo%3ACityGroup_0/FR/CC%20les%20Ch%C3%A2teaux"
      Assert.assertEquals(containerUrl + DatacoreApi.DC_TYPE_PATH + modelType + "/FR/CC%20les%20Ch%C3%A2teaux", uri);
      String reservedChars = "$&+,/:;=@"; // NOT ?
      uri = UriHelper.buildUri(containerUrl, modelType, reservedChars);
      for (int i = 0; i < reservedChars.length(); i++) {
         char reservedChar = reservedChars.charAt(i);
         Assert.assertTrue(uri.contains(reservedChar + ""));
      }
      
      // test that unsafe chars are encoded in path
      uri = UriHelper.buildUri(containerUrl, modelType, "FR/CC les Châteaux"); // "http://data.ozwillo.com/dc/type/geo%3ACityGroup_0/FR/CC%20les%20Ch%C3%A2teaux"
      Assert.assertEquals(containerUrl + DatacoreApi.DC_TYPE_PATH + modelType + "/FR/CC%20les%20Ch%C3%A2teaux", uri);
      String unsafeChars = " \"<>#%{}|\\^[]`"; // NOT ~ ?!!
      uri = UriHelper.buildUri(containerUrl, modelType, unsafeChars);
      for (int i = 0; i < unsafeChars.length(); i++) {
         char unsafeChar = unsafeChars.charAt(i);
         if (unsafeChar == '%') {
            continue; // can't be tested this way
         }
         Assert.assertTrue(!uri.contains(unsafeChar + ""));
      }
      
   }

   
   @Test
   public void testIdThatIsAnUri() throws Exception {
      String modelType = "photo:Library_0";
      String uriThatIsAnId = "https://www.10thingstosee.com/media/photos/france-778943_HjRL4GM.jpg";
      
      String uri = UriHelper.buildUri(containerUrl, modelType, uriThatIsAnId); // "http://data.ozwillo.com/dc/type/photo:Library_0/https%3A%2F%2Fwww.10thingstosee.com%2Fmedia%2Fphotos%2Ffrance-778943_HjRL4GM.jpg"
      Assert.assertEquals(containerUrl + DatacoreApi.DC_TYPE_PATH + modelType + "/https%3A%2F%2Fwww.10thingstosee.com%2Fmedia%2Fphotos%2Ffrance-778943_HjRL4GM.jpg", uri);
   }
   
}
