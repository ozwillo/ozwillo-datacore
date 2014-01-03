package org.oasis.datacore.rest.server.cxf;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.impl.RequestImpl;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.oasis.datacore.rest.api.util.JaxrsApiProvider;
import org.oasis.datacore.rest.client.cxf.CxfMessageHelper;


/**
 * Provides HTTP protocol info to JAXRS / CXF server impl.
 * 
 * @author mdutoo
 *
 */
public class CxfJaxrsApiProvider implements JaxrsApiProvider {

   @Override
   public HttpHeaders getHttpHeaders() {
      return new HttpHeadersImpl(getInMessage());
   }

   @Override
   public UriInfo getUriInfo() {
      return new UriInfoImpl(getInMessage());
   }

   @Override
   public Request getJaxrsRequest() {
      return new RequestImpl(getInMessage());
   }

   public Message getInMessage() {
      return PhaseInterceptorChain.getCurrentMessage().getExchange().getInMessage();
   }

   @Override
   public List<String> getRequestHeader(String name) {
      // TODO better
      return new HttpHeadersImpl(getInMessage()).getRequestHeader(name);
   }

   @Override
   public String getHeaderString(String name) {
      return CxfMessageHelper.getHeaderString(getInMessage(), name);
   }

   @Override
   public MultivaluedMap<String, String> getRequestHeaders() {
      // TODO better
      return new HttpHeadersImpl(getInMessage()).getRequestHeaders();
   }

   @Override
   public List<MediaType> getAcceptableMediaTypes() {
      // TODO better
      return new HttpHeadersImpl(getInMessage()).getAcceptableMediaTypes();
   }

   @Override
   public List<Locale> getAcceptableLanguages() {
      // TODO better
      return new HttpHeadersImpl(getInMessage()).getAcceptableLanguages();
   }

   @Override
   public MediaType getMediaType() {
      // TODO better
      return new HttpHeadersImpl(getInMessage()).getMediaType();
   }

   @Override
   public Locale getLanguage() {
      // TODO better
      return new HttpHeadersImpl(getInMessage()).getLanguage();
   }

   @Override
   public Map<String, Cookie> getCookies() {
      // TODO better
      return new HttpHeadersImpl(getInMessage()).getCookies();
   }

   @Override
   public Date getDate() {
      // TODO better
      return new HttpHeadersImpl(getInMessage()).getDate();
   }

   @Override
   public int getLength() {
      // TODO better
      return new HttpHeadersImpl(getInMessage()).getLength();
   }

   @Override
   public String getPath() {
      // TODO better
      return new UriInfoImpl(getInMessage()).getPath();
   }

   @Override
   public String getPath(boolean decode) {
      // TODO better
      return new UriInfoImpl(getInMessage()).getPath(decode);
   }

   @Override
   public List<PathSegment> getPathSegments() {
      // TODO better
      return new UriInfoImpl(getInMessage()).getPathSegments();
   }

   @Override
   public List<PathSegment> getPathSegments(boolean decode) {
      // TODO better
      return new UriInfoImpl(getInMessage()).getPathSegments(decode);
   }

   @Override
   public URI getRequestUri() {
      // TODO better
      return new UriInfoImpl(getInMessage()).getRequestUri();
   }

   @Override
   public UriBuilder getRequestUriBuilder() {
      // TODO better
      return new UriInfoImpl(getInMessage()).getRequestUriBuilder();
   }

   @Override
   public URI getAbsolutePath() {
      // TODO better
      return new UriInfoImpl(getInMessage()).getAbsolutePath();
   }

   @Override
   public UriBuilder getAbsolutePathBuilder() {
      // TODO better
      return new UriInfoImpl(getInMessage()).getAbsolutePathBuilder();
   }

   @Override
   public URI getBaseUri() {
      // TODO better
      return new UriInfoImpl(getInMessage()).getBaseUri();
   }

   @Override
   public UriBuilder getBaseUriBuilder() {
      // TODO better
      return new UriInfoImpl(getInMessage()).getBaseUriBuilder();
   }

   @Override
   public MultivaluedMap<String, String> getPathParameters() {
      // TODO better
      return new UriInfoImpl(getInMessage()).getPathParameters();
   }

   @Override
   public MultivaluedMap<String, String> getPathParameters(boolean decode) {
      // TODO better
      return new UriInfoImpl(getInMessage()).getPathParameters(decode);
   }

   @Override
   public MultivaluedMap<String, String> getQueryParameters() {
      // TODO better
      return new UriInfoImpl(getInMessage()).getQueryParameters();
   }

   @Override
   public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
      // TODO better
      return new UriInfoImpl(getInMessage()).getQueryParameters(decode);
   }

   @Override
   public List<String> getMatchedURIs() {
      // TODO better
      return new UriInfoImpl(getInMessage()).getMatchedURIs();
   }

   @Override
   public List<String> getMatchedURIs(boolean decode) {
      // TODO better
      return new UriInfoImpl(getInMessage()).getMatchedURIs(decode);
   }

   @Override
   public List<Object> getMatchedResources() {
      // TODO better
      return new UriInfoImpl(getInMessage()).getMatchedResources();
   }

   @Override
   public String getMethod() {
      // TODO better
      return new RequestImpl(getInMessage()).getMethod();
   }

   @Override
   public Variant selectVariant(List<Variant> variants)
         throws IllegalArgumentException {
      // TODO better
      return new RequestImpl(getInMessage()).selectVariant(variants);
   }

   @Override
   public ResponseBuilder evaluatePreconditions(EntityTag eTag) {
      // TODO better
      return new RequestImpl(getInMessage()).evaluatePreconditions(eTag);
   }

   @Override
   public ResponseBuilder evaluatePreconditions(Date lastModified) {
      // TODO better
      return new RequestImpl(getInMessage()).evaluateIfNotModifiedSince(lastModified);
   }

   @Override
   public ResponseBuilder evaluatePreconditions(Date lastModified,
         EntityTag eTag) {
      // TODO better
      return new RequestImpl(getInMessage()).evaluatePreconditions(lastModified, eTag);
   }

   @Override
   public ResponseBuilder evaluatePreconditions() {
      // TODO better
      return new RequestImpl(getInMessage()).evaluatePreconditions();
   }
   
}
