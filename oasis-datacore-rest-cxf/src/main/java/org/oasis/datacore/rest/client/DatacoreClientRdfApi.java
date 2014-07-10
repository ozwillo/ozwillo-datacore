package org.oasis.datacore.rest.client;

import org.oasis.datacore.rest.api.util.DatacoreMediaType;

@javax.ws.rs.Consumes(DatacoreMediaType.APPLICATION_NQUADS)
@javax.ws.rs.Produces(DatacoreMediaType.APPLICATION_NQUADS)
public interface DatacoreClientRdfApi extends DatacoreClientApi {

}
