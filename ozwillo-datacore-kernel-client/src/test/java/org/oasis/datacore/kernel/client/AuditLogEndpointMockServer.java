package org.oasis.datacore.kernel.client;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import org.oasis.datacore.rest.server.cxf.JaxrsServerBase;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/l")
@Api(value = "/l", description = "Audit log API")
public class AuditLogEndpointMockServer extends JaxrsServerBase {

	@Path("/event")
	@POST
	@Consumes("application/json")
	@ApiOperation("Log an event in the audit log service")
	public Response json() {
		return Response
		        .status(200)
		        .type("text/plain")
		        .entity("response")
		        .build();
	}

}