package org.oasis.datacore.rest.api;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import org.junit.Assert;
import org.junit.Test;
import org.oasis.datacore.rest.api.util.UriHelper;

public class DatacoreApiUtilTest {

   @Test
   public void testEncodingLibs() throws Exception {
      String value = "\"Bordeaux&= +.;#~_\"";
      String jdkEncodedValue = "name=" + URLEncoder.encode(value, "UTF-8");
      Assert.assertEquals("JDK and CXF should do same URL encoding", "name=%22Bordeaux%26%3D+%2B.%3B%23%7E_%22", jdkEncodedValue);
   }

   @Test
   public void testUriHelper() throws Exception {
      String containerUrl = "http://data.oasis-eu.org/";
      String testRelativeUri = "dc//type/sample.marka.field//1";
      
      // test normalizeUrlMode
      String[] res = UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
            containerUrl + testRelativeUri , containerUrl, true, false);
      Assert.assertEquals(containerUrl, res[0]);
      Assert.assertEquals("dc/type/sample.marka.field/1", res[1]);
      
      // test normalizeUrlMode HTTPS
      res = UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
            "https://data.oasis-eu.org/" + testRelativeUri, containerUrl, true, false);
      Assert.assertEquals("https://data.oasis-eu.org/", res[0]);
      Assert.assertEquals("dc/type/sample.marka.field/1", res[1]);

      // test normalizeUrlMode relative URI
      res = UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
            testRelativeUri, containerUrl, true, false);
      Assert.assertEquals(containerUrl, res[0]);
      Assert.assertEquals("dc/type/sample.marka.field/1", res[1]);

      // test normalizeUrlMode fails on bad URI chars
      // see URI spec & http://stackoverflow.com/questions/1547899/which-characters-make-a-url-invalid
      try {
         UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
               containerUrl + "\\", containerUrl, true, false);
         Assert.fail("Should fail on bad URI chars");
      } catch (URISyntaxException e) {
         Assert.assertTrue(true);
      }

      // test normalizeUrlMode absolute URL fails on bad URL
      try {
         UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
               "unknown://data.oasis-eu.org/" + testRelativeUri, containerUrl, true, false);
         Assert.fail("Should fail on unknown URL protocol");
      } catch (MalformedURLException e) {
         Assert.assertTrue(true);
      }

      // test normalizeUrlMode absolute URL fails on non HTTP(S) URL
      try {
         UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
               "ftp://data.oasis-eu.org/" + testRelativeUri, containerUrl, true, false);
         Assert.fail("Should fail on non HTTP(S) URL");
      } catch (MalformedURLException e) {
         Assert.assertTrue(true);
      }
      
      // test matchBaseUrlMode BEWARE DOESN'T CHECK URI OR URL CHARS
      res = UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
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
      
      // test bare replace mode BEWARE NO CHECKS AND EXPECTS CONTAINER URL
      res = UriHelper.getUriNormalizedContainerAndPathWithoutSlash(
            containerUrl + testRelativeUri, containerUrl, false, false);
      Assert.assertEquals(containerUrl, res[0]);
      Assert.assertEquals(testRelativeUri, res[1]);
   }
}
