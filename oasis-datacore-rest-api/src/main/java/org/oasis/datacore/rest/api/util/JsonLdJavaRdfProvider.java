package org.oasis.datacore.rest.api.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.oasis.datacore.rest.api.DCResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.impl.NQuadRDFParser;
import com.github.jsonldjava.utils.JsonUtils;


/*
 * Media Types defined in DatacoreApi
 */

@Provider
@Consumes({MediaType.APPLICATION_JSON, DatacoreMediaType.APPLICATION_NQUADS,
   DatacoreMediaType.APPLICATION_TURTLE, DatacoreMediaType.APPLICATION_JSONLD,
   DatacoreMediaType.APPLICATION_JSONLD + "; format=expand" })
@Produces({MediaType.APPLICATION_JSON, DatacoreMediaType.APPLICATION_NQUADS,
   DatacoreMediaType.APPLICATION_TURTLE, DatacoreMediaType.APPLICATION_JSONLD,
   DatacoreMediaType.APPLICATION_JSONLD + "; format=expand" })
public class JsonLdJavaRdfProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

   private static final Logger logger = LoggerFactory.getLogger(JsonLdJavaRdfProvider.class);
   
   /** wired by Spring XML ; should normally be DatacoreObjectMapper */
   private ObjectMapper objectMapper;
   
   @Override
   public boolean isWriteable(Class<?> type,
         java.lang.reflect.Type genericType, Annotation[] annotations,
         MediaType mediaType) {
      if (mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
         // default
         return false;
      }

      //return mediaType.isCompatible(MediaType.valueOf("text/turtle;a=b")) // TODO constant
      //      || mediaType.isCompatible(DatacoreMediaType.APPLICATION_NQUADS_TYPE);*/
      return true;
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
      
      /*String acceptedMediaTypeString = (String) httpHeaders.getFirst(HttpHeaders.ACCEPT);
      MediaType acceptedMediaType = acceptedMediaTypeString == null || acceptedMediaTypeString.isEmpty()
            ? MediaType.APPLICATION_JSON_TYPE : MediaType.valueOf(acceptedMediaTypeString);*/
      if (!isWriteable(type, genericType, annotations, mediaType)) {
         // should never be reached
         throw new WebApplicationException(Response.serverError().entity(
               "JsonLdJavaRdfProvider can't handle media type " + mediaType).build());
      }
      
      /*
      if (acceptedMediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
         // default
         objectMapper.writer().writeValue(entityStream, t);
         return;
      }
      
      if (!(acceptedMediaType.isCompatible(MediaType.valueOf("application/json+ld"))
            || acceptedMediaType.isCompatible(DatacoreMediaType.APPLICATION_NQUADS_TYPE))) {
         throw new WebApplicationException(Response.serverError().entity(
               "JsonLdJavaRdfProvider can't handle "
               + "accepted media type " + acceptedMediaTypeString).build());
      }
      */
      
      try {
         // TODO TODOOOOOOOOOOOOOOOOOOOOOOOO improve that !!
         String json = objectMapper.writeValueAsString(t)
               .replaceAll("\"l\"", "\"@language\"")
               .replaceAll("\"v\"", "\"@value\"");
         
         Object jsonObject = JsonUtils.fromInputStream(new ByteArrayInputStream(json.getBytes()));
         
         // Create a context JSON map containing prefixes and definitions
         Map<String,String> context = new HashMap<String,String>();
         // Customise context...
         context.put("dc", "http://dc");
         context.put("i18n:name", "{\"@container\": \"@language\"}");
         // Create an instance of JsonLdOptions with the standard JSON-LD options
         JsonLdOptions options = new JsonLdOptions();
         ///options.set
         // Customise options...
         Object res = jsonObject;

         String format = mediaType.getParameters().get(DatacoreMediaType.APPLICATION_JSONLD_FORMAT_PARAM);
         // NB. JSONLD parameterized media types are all "compatible" together,
         // so rather checking format parameter explicitly :
         if(DatacoreMediaType.JSONLD_COMPACT.equals(format)) {
            res = JsonLdProcessor.compact(jsonObject, context, options);
            //System.out.println(JsonUtils.toPrettyString(compact));
         } else if(DatacoreMediaType.JSONLD_FLATTEN.equals(format)) {
            res = JsonLdProcessor.flatten(jsonObject, context, options);
         } else if(DatacoreMediaType.JSONLD_EXPAND.equals(format)) {
            res = JsonLdProcessor.expand(jsonObject, options);
         } else if(DatacoreMediaType.JSONLD_FRAME.equals(format)) {
            res = JsonLdProcessor.frame(jsonObject, context, options);
         } else if(mediaType.isCompatible(DatacoreMediaType.APPLICATION_TURTLE_TYPE)
               || "turtle".equals(format) || "ttl".equals(format)) {
            options.format = "text/turtle";
            res = JsonLdProcessor.toRDF(jsonObject, options);
         } else if(format == null
               || mediaType.isCompatible(MediaType.TEXT_PLAIN_TYPE) || "text/plain".equals(format)
               || mediaType.isCompatible(DatacoreMediaType.APPLICATION_NQUADS_TYPE)
               || mediaType.isCompatible(DatacoreMediaType.APPLICATION_NTRIPLES_TYPE)
               || "nquads".equals(format) || "nq".equals(format) || "nt".equals(format)
               || "ntriples".equals(format)) {
            //System.out.println("Generating Nquads Report");
            options.format = "application/nquads";
            res = JsonLdProcessor.toRDF(jsonObject, options);
         }
         JsonUtils.write(new OutputStreamWriter(entityStream), res);
         // NB. and NOT res.toString() else map key & values not quoted !!
         
      } catch(IOException | JsonLdError ioe) {
         //Problem with json ld fall back to normal execution
         logger.error("Error while outputting JSONLD-based format (RDF etc.)", ioe);
         objectMapper.writer().writeValue(entityStream, t);
         return;
      }
      
      
      
/*
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
      */
   }


   @Override
   public boolean isReadable(Class<?> type, java.lang.reflect.Type genericType,
         Annotation[] annotations, MediaType mediaType) {
      if (mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
         // default
         return false;
      }

      return mediaType.isCompatible(MediaType.valueOf("application/json+ld")) // TODO constant
            || mediaType.isCompatible(DatacoreMediaType.APPLICATION_NQUADS_TYPE);
   }


   
   @Override
   public Object readFrom(Class<Object> type,
         java.lang.reflect.Type genericType, Annotation[] annotations,
         MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
         InputStream entityStream) throws IOException, WebApplicationException {

      if (!isWriteable(type, genericType, annotations, mediaType)) {
         // should never be reached
         throw new WebApplicationException(Response.serverError().entity(
               "JsonLdJavaRdfProvider can't handle accepted type " + mediaType).build());
      }
      
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
   
   private Object parseAndBuildResource(Object jsonObject) throws IOException {
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
