import React from 'react';
import LinkPlayground from '../linkPlayground.js';

export default class commonQueries extends React.Component{
  render() {
    return (
      <div>
        <h2>Most common queries</h2>
        <ul>
          <li className="listespace">
            <a className="playground">geo_1</a> - list countries : <a className="playground">countries</a> supported by Ozwillo (Portal etc.), or also <a className="playground">sorted by name</a>.
            Or for some use cases any country (actually first 10 countries ; or <a className="playground">first 100 countries</a> but beware that limit can't go beyond 100, in which case iteration
            on ex. id or name should be preferred, see below for organizations).
          </li>

          <li className="listespace">
            <a className="playground">geo_1</a>  - choose city by upper hierarchy ex. Paris or Sofia (София) : having chosen country, <a className="playground">NUTS2s</a> by country (or country <a className="playground">NUTS2s</a>) then
            <a className="playground">NUTS3s by NUTS2</a> , or depending on your country <a className="playground">NUTS3s by country</a> (or <a className="playground">country NUTS3s</a>),
              and finally <a className="playground">address cities by NUTS3</a> (<a className="playground">another example </a>),
              or for some use cases <a className="playground">geographical cities by NUTS3</a> (or <a className="playground">country geographical cities by NUTS3</a>).
          </li>

          <li className="listespace">
            <a className="playground">geo_1</a> - choose city using autocompletion field: <a className="playground">address city by fulltext name and country</a>
            (<a className="playground">another example</a>; however it has beenv disabled on generic display name to avoid finding its cities when looking for a province),
            or for some use cases <a className="playground">geographical city by fulltext name and country</a>.
          </li>

          <li className="listespace">
              <a className="playground">org_1</a> - choose org using autocompletion input : <a className="playground">orgs by fulltext name in country</a>
              <a className="playground">then also by altName in country</a>(must be done in separate requests),
              or <a className="playground">country org by fulltext</a> name then <a className="playground">also by altName</a> (however it has
              been disabled <a className="playground">on generic display name</a>)
          </li>

          <li className="listespace">
            <a className="playground">org_1</a> - choose business activity using autocompletion input : <a className="playground">activity by fulltext name and country</a>
            or country activity by fulltext name.
          </li>

          <li className="listespace">
            <a className="playground">org_1</a> - downloading all business activities to cache them by iterating on @id
            (easier than on orgact:code which would need to do >=and merge same-code activities of different countries, rather than merely >)
            : <a className="playground">first query</a>, then iterate on activities coming
            after last name returned : <a className="playground">second query</a>, <a className="playground">third query</a>...
          </li>
        </ul>
      </div>
    );
  }
  }
