import React from 'react';
import LinkPlayground from '../linkPlayground.js';

export default class Quickstart extends React.Component{

  render() {
    return (
      <div>
        <h2>Playground User Manual</h2>
        <p>The aim of this playground is to familiarize yourself with the <a>Ozwillo Datacore API</a>.<br/>
        Indeed, it is far easier to discover this API with the playground than calling it with curl or equivalent.</p>
    <ul>
      <li><strong>Data Resources</strong> are handled in JSON-LD-like (with an implicit context) format and with
        W3C LDP (Linked Data Platform, see primer and wiki)-like operations (URIs, future collection filtering (1, 2) -inspired finders etc.).
        Have a look at existing Resources, at <LinkPlayground tools={this.props.tools} url="/dc/type/pli:city_0" value="/dc/type/pli:city_0" /> for instance for resources in the pli:city_0 Model
        type (meaning version 0 of the pli:city Model). In addition to the simpler default native pure JSON format, "true" JSON-LD formats
        and semantic web formats (such as RDF, see example) are available.</li>

      <li><strong>Data Models</strong> describe what kinds of Data Resources are allowed, in a JSON Schema-like structure
        (see <a href="http://jsonschema.net/">jsonschema.net playground</a>) with
        string, boolean, int, float, long, double, date, map, list, i18n, resource fields grouped in reusable Mixin types. Have a look at
        known Models at <LinkPlayground tools={this.props.tools} url="/dc/type/dcmo:model_0" value="/dc/type/dcmo:model_0" /> and Mixins at <LinkPlayground tools={this.props.tools} url="/dc/type/dcmi:mixin_0" value="/dc/type/dcmi:mixin_0" />,
        in their own metamodel where they may be introspected, but also drafted and published (upcoming).</li>
    </ul>
  </div>
  );
  } 
}
