import React from 'react';

export default class serverConfiguration extends React.Component{

  render() {
    return (
      <div className="center">
        <h3>Server Configuration</h3>
        <div className="ui segment">
          <p>
            ## Development mode (also allows to set system property datacore.dropdb=true ex. in maven)<br/>
            datacore.devmode=true<br/>

            ## Shard Endpoint - full URL (for clients...)<br/>
            datacoreApiServer.baseUrl=https://data.ozwillo-preprod.eu<br/><br/>

            ## Container - default URL (for URIs...)<br/>
            datacoreApiServer.containerUrl=http://data.ozwillo.com<br/>

            ## Other (or all) known Datacores (comma-separated container URLs)<br/>
            datacoreApiServer.knownDatacoreContainerUrls=<br/><br/>

            ## Query :<br/>
            ## default maximum number of documents to scan when fulfilling a query, overriden by<br/>
            ## DCFields', themselves limited by DCModel's. 0 means no limit (for tests), else ex.<br/><br/>
            ## 1000 (secure default), 100000 (on query-only nodes using secondary & timeout)...<br/>
            ## http://docs.mongodb.org/manual/reference/operator/meta/maxScan/<br/>
            datacoreApiServer.query.maxScan=0<br/>
            ## default maximum start position<br/>
            datacoreApiServer.query.maxStart=500<br/>
            ## default maximum number of documents returned<br/>
            datacoreApiServer.query.maxLimit=100<br/>
            ## default number of documents returned<br/>
            datacoreApiServer.query.defaultLimit=10<br/><br/>

            ## Kernel Configuration<br/>
            kernel.baseUrl=https://kernel.ozwillo-preprod.eu<br/>
          </p>
        </div>

      </div>
    );
  }
}
