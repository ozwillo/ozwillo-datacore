import React from 'react';
import LinkPlayground from '../linkPlayground.js';

export default class UserManual extends React.Component{

  render() {
    return (
      <div>
        <h2>Playground User Manual</h2>
        <p>The aim of this playground is to familiarize yourself with the <a>Ozwillo Datacore API</a>.<br/>
        Indeed, it is far easier to discover this API with the playground than calling it with curl or equivalent.</p>
       <table className="ui basic table">
        <tbody>
          <tr><td><strong>Click on a URI's id</strong> to go to the corresponding Resource</td></tr>
          <tr><td><strong>Click on type in a URI or on an item of the @type list</strong> to go to the corresponding model definition.</td></tr>
          <tr><td><strong>Click on :</strong> between a property's key and value to query all other resources with the same value for this property
            in the same model type. Note that the Playground shows said key and value UNencoded in order to make it easier to enter
            text in the language of the user, but that developers should URI-encode them nonetheless, as Playground actually does.
            (see <a href="http://www.w3schools.com/tags/ref_urlencode.asp">http://www.w3schools.com/tags/ref_urlencode.asp
          </a>)
        </td></tr>
        
        <tr><td><strong>Click on /</strong> between a URI's model type and id to look for resources that refer (link) to it : if it's a metamodel
          it queries its resources, otherwise it lists all models that define a resource field that links to it through its model
          type at top level or in a top level list field, and provides links allowing to do the same at further depth below top
          level and for other mixins besides its model type. </td></tr>
        <tr><td><strong>Project portal</strong> : select another project in the dropdown above the Playground (see details on <a>how to explore its
          available (meta)models</a>).
        </td></tr>
        <tr><td><strong>Query line </strong>: write there the relative URI of a single Resource or of a LDP-like Datacore query (see documentation below
          in Swagger UI of operation findDataInType for typed queries (criteria, sort, ranged query- or iterator-based pagination). </td></tr>
        <tr><td><strong>Buttons </strong>: roll your mouse over them to see what they do. </td></tr>
      </tbody>
    </table>
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
