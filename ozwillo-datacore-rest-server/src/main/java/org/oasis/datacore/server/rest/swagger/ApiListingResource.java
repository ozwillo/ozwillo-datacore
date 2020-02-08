package org.oasis.datacore.server.rest.swagger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.jaxrs.listing.BaseApiListingResource;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;

/**
 * Adaptation of the default ApiListingResource required upgrades :
 * - passes a null ServletContext, else CXF 3.3.1 now injects a non null
 * but non working ServletContext : NPE because its ThreadLocal
 * contains null (CXF should not, because Swagger is not deployed
 * in servlet i.e. DefaultJaxrsConfig mode but in BeanConfig mode)
 * - deployed at the old URL path
 * 
 * @author mdutoo
 */
//@Path("/swagger.{type:json|yaml}")
@Path("/api-docs")
@Api("/api-docs")
@Produces({"application/json; charset=utf-8"})
public class ApiListingResource extends BaseApiListingResource {

    //@Context
    //ServletContext context;

    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
    @ApiOperation(value = "The swagger definition in either JSON or YAML", hidden = true)
    public Response getListing(
            @Context Application app,
            @Context ServletConfig sc,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo,
            @PathParam("type") String type) {
        if (StringUtils.isNotBlank(type) && type.trim().equalsIgnoreCase("yaml")) {
            return getListingYamlResponse(app, null, sc, headers, uriInfo);
        } else {
            return getListingJsonResponse(app, null, sc, headers, uriInfo);
        }
    }

}
