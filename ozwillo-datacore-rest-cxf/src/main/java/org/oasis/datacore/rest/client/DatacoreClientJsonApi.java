package org.oasis.datacore.rest.client;

import javax.ws.rs.core.MediaType;

@javax.ws.rs.Consumes(MediaType.APPLICATION_JSON)
@javax.ws.rs.Produces(MediaType.APPLICATION_JSON)
public interface DatacoreClientJsonApi extends DatacoreClientApi {

}