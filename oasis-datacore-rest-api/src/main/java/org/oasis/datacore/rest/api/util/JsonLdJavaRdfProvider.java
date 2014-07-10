package org.oasis.datacore.rest.api.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.commons.io.IOUtils;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.binding.DatacoreObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.impl.NQuadRDFParser;
import com.github.jsonldjava.utils.JsonUtils;


/*
 * Media Types defined in DatacoreApi
 */

@javax.ws.rs.ext.Provider
@javax.ws.rs.Consumes(DatacoreMediaType.APPLICATION_NQUADS)
@javax.ws.rs.Produces(DatacoreMediaType.APPLICATION_NQUADS) 
public class JsonLdJavaRdfProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {
   
   /** wired by Spring XML ; should normally be DatacoreObjectMapper */
   private ObjectMapper objectMapper;

   @Override
   public boolean isWriteable(Class<?> type,
         java.lang.reflect.Type genericType, Annotation[] annotations,
         MediaType mediaType) {
      return DatacoreMediaType.APPLICATION_NQUADS_TYPE.isCompatible(mediaType);
   }


   @Override
   public long getSize(Object t, Class<?> type,
         java.lang.reflect.Type genericType, Annotation[] annotations,
         MediaType mediaType) {
      return -1;
   }


   @Override
   public void writeTo(Object t, Class<?> type,
         java.lang.reflect.Type genericType, Annotation[] annotations,
         MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
         OutputStream entityStream) throws IOException, WebApplicationException {

      String json = objectMapper.writeValueAsString(t);
      Object jsonObject = JsonUtils.fromInputStream(new ByteArrayInputStream(json.getBytes()));
      // Create a context JSON map containing prefixes and definitions
      Map<String, String> context = new HashMap<String, String>();
      context.put("dc", "http://dc");
      context.put("i18n:name", "{\"@container\": \"@language\"}");
      // Create an instance of JsonLdOptions with the standard JSON-LD options
      JsonLdOptions options = new JsonLdOptions();
      //options.setExpandContext(context);
      options.format = "application/nquads";

      String nquadsRdf = null;
      try {
         nquadsRdf = (String) JsonLdProcessor.toRDF(jsonObject, options);
      } catch (JsonLdError e) {
         e.printStackTrace();
      }

      InputStream rdfStream = IOUtils.toInputStream(nquadsRdf);
      IOUtils.copy(rdfStream, entityStream);
   }


   @Override
   public boolean isReadable(Class<?> type, java.lang.reflect.Type genericType,
         Annotation[] annotations, MediaType mediaType) {
      return DatacoreMediaType.APPLICATION_NQUADS_TYPE.isCompatible(mediaType);
   }


   
   @Override
   public Object readFrom(Class<Object> type,
         java.lang.reflect.Type genericType, Annotation[] annotations,
         MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
         InputStream entityStream) throws IOException, WebApplicationException {
      
      JsonLdOptions options = new JsonLdOptions();
      //options.format = "application/nquads";
      String rdfString = IOUtils.toString(entityStream);
      Object jsonObject = null;
      
      try {
         NQuadRDFParser nquadParser = new NQuadRDFParser();
         //Object nquad = nquadParser.parse(rdfString);
         //jsonObject = JsonLdProcessor.fromRDF(nquad, options);
         options.outputForm = "compacted";
         options.setUseNativeTypes(true);
         jsonObject = JsonLdProcessor.fromRDF(rdfString, options, nquadParser);
      } catch (JsonLdError e) {
         e.printStackTrace();
      }
      
      if(type.toString().contains("DCResource")) {
         return parseAndBuildResource(jsonObject);
      } else {
         ArrayList<Object> resourceList = new ArrayList<Object>();
         //Test if single resource or already resource list.
         LinkedHashMap<?,?> map = (LinkedHashMap<?,?>) jsonObject;
         if(map.containsKey("@graph")) {
            ArrayList<?> tempList = (ArrayList<?>) map.get("@graph");
            for(int i = 0; i < tempList.size(); i++) {
               resourceList.add(parseAndBuildResource(tempList.get(i)));
            }
         } else {
            resourceList.add(parseAndBuildResource(jsonObject));
         }
         
         return resourceList;
      }
   }
   
   public Object parseAndBuildResource(Object jsonObject) throws IOException {
      String json = objectMapper.writeValueAsString(jsonObject);
      return objectMapper.reader(DCResource.class) // TODO rather type but if list after handling single value array like in interceptor
            .readValue(json);
   }

   public ObjectMapper getObjectMapper() {
      return objectMapper;
   }


   public void setObjectMapper(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
   }

}
