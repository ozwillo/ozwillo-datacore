import React from 'react';
import LinkPlayground from '../linkPlayground.js';

export default class commonQueries extends React.Component{
  render() {
    return (
      <div>
        <h2>Most common queries</h2>
        <ul>
          <li className="listespace">
            <LinkPlayground tools={this.props.tools} url="/dc/type/dcmp:Project_0/geo_1" value="geo_1" /> - list countries
            : <LinkPlayground tools={this.props.tools} url="/dc/type/geohier:Hierarchical_0" value="countries
            supported by Ozwillo (Portal etc.)" />, or also <LinkPlayground tools={this.props.tools} url="/dc/type/geohier:Hierarchical_0?geoco:name=+" value="sorted by name" />.
            Or for some use cases <LinkPlayground tools={this.props.tools} url="/dc/type/geoco:Country_0 any" value="any country" /> (actually first 10 countries ;
            or <LinkPlayground tools={this.props.tools} url="/dc/type/geoco:Country_0?limit=100" value="first 100 countries" />but beware
            that limit can't go beyond 100, in which case iteration on ex. id or name should be preferred, see below for organizations).
          </li>

          <li className="listespace">
            <LinkPlayground tools={this.props.tools} url="/dc/type/dcmp:Project_0/geo_1" value="geo_1" /> - choose city by upper hierarchy ex.
            Paris or Sofia (София) : having chosen country,
             <LinkPlayground tools={this.props.tools} url="/dc/type/geon2:Nuts2_0?geo:country=http://data.ozwillo.com/dc/type/geocofr:Pays_0/FR&geo:name=+" value="NUTS2s by country" />
            (or country <LinkPlayground tools={this.props.tools} url="/dc/type/geon2fr:R%C3%A9gion_0?geo:name=+" value="NUTS2s" />) then
            <LinkPlayground tools={this.props.tools} url="/dc/type/geon3fr:D%C3%A9partement_0?geon:parent=http://data.ozwillo.com/dc/type/geon2fr:R%C3%A9gion_0/FR/FR-J&geo:name=+" value="NUTS3s by NUTS2" />,
            or depending on your country
            <LinkPlayground tools={this.props.tools} url="/dc/type/geon3:Nuts3_0?geo:country=http://data.ozwillo.com/dc/type/geocobg:C%D1%82%D1%80%D0%B0%D0%BD%D0%B0_0/BG&geo:name=+" value="NUTS3s by country" /> (or
            <LinkPlayground tools={this.props.tools} url="/dc/type/geon3bg:%D0%9E%D0%B1%D0%BB%D0%B0%D1%81%D1%82_0?geo:name=+" value="country NUTS3s" />),
            and finally <LinkPlayground tools={this.props.tools} url="/dc/type/addrpostci:PostalCity_0?addrpostci:nuts3=http://data.ozwillo.com/dc/type/geon3fr:D%C3%A9partement_0/FR/FR-75" value="address cities by NUTS3" />
            (<LinkPlayground tools={this.props.tools} url="/dc/type/addrpostci:PostalCity_0?addrpostci:nuts3=http://data.ozwillo.com/dc/type/geon3bg:%D0%9E%D0%B1%D0%BB%D0%B0%D1%81%D1%82_0/BG/BG-22" value="another example" />),
            or for some use cases <LinkPlayground tools={this.props.tools} url="/dc/type/geoci:City_0?geoci:nuts3=http://data.ozwillo.com/dc/type/geon3fr:D%C3%A9partement_0/FR/FR-75&geo:name=+" value="geographical cities by NUTS3" />
            (or <LinkPlayground tools={this.props.tools} url="/dc/type/geocibg:%D0%9D%D0%B0%D1%81%D0%B5%D0%BB%D0%B5%D0%BD%D0%BEM%D0%B5%D1%81%D1%82%D0%BE_0?geoci:nuts3:http://data.ozwillo.com/dc/type/geon3bg:%D0%9E%D0%B1%D0%BB%D0%B0%D1%81%D1%82_0/BG/BG-22&geo:name=+" value="country geographical cities by NUTS3" />).
          </li>

          <li className="listespace">
            <LinkPlayground tools={this.props.tools} url="/dc/type/dcmp:Project_0/geo_1" value="geo_1" />- choose city using autocompletion field:
             <LinkPlayground tools={this.props.tools} url="/dc/type/addrpostci:PostalCity_0?geo:name=$fulltextparis+&geo:country=http://data.ozwillo.com/dc/type/geocofr:Pays_0/FR" value="address city by fulltext name and country" />
            (<LinkPlayground tools={this.props.tools} url="/dc/type/addrpostci:PostalCity_0?geo:name=$fulltextСофия+&geo:country=http://data.ozwillo.com/dc/type/geocobg:C%D1%82%D1%80%D0%B0%D0%BD%D0%B0_0/BG" value="another example" />;
            however it has been disabled on generic display name to avoid finding its cities when looking for a province),
            or for some use cases <LinkPlayground tools={this.props.tools} url="/dc/type/addrpostci:PostalCity_0?odisp:name=$fulltextparis+&geo:country=http://data.ozwillo.com/dc/type/geocofr:Pays_0/FR" value="geographical city by fulltext name and country" />.
          </li>

          <li className="listespace">
              <LinkPlayground tools={this.props.tools} url="/dc/type/dcmp:Project_0/org_1" value="org_1" />- choose org using autocompletion input :
              <LinkPlayground tools={this.props.tools} url="/dc/type/org:Organization_0?org:legalName=$fulltextbonnard+&org:country=http://data.ozwillo.com/dc/type/geocofr:Pays_0/FR" value="orgs by fulltext name in country" />
              <LinkPlayground tools={this.props.tools} url="/dc/type/org:Organization_0?org:altName=$fulltextbonnard+&org:country=http://data.ozwillo.com/dc/type/geocofr:Pays_0/FR" value="then also by altName in country" />
              (must be done in separate requests),
              or <LinkPlayground tools={this.props.tools} url="/dc/type/orgfr:Organisation_0?org:legalName=$fulltextbonnard+" value="country org by fulltext" /> name then
              <LinkPlayground tools={this.props.tools} url="/dc/type/orgfr:Organisation_0?org:altName=$fulltextbonnard+" value="also by altName" /> (however it has
              been disabled on <LinkPlayground tools={this.props.tools} url="/dc/type/org:Organization_0?odisp:name=$fulltextbonnard+&org:country=http://data.ozwillo.com/dc/type/geocofr:Pays_0/FR" value="generic display name" />)
          </li>

          <li className="listespace">
            <LinkPlayground tools={this.props.tools} url="/dc/type/dcmp:Project_0/org_1" value="org_1" /> - choose business activity using
            autocompletion input : <LinkPlayground tools={this.props.tools} url="/dc/type/orgact:Activity_0?orgact:label=$fulltextaquacult+&orgact:country=http://data.ozwillo.com/dc/type/geocofr:Pays_0/FR" value="activity by fulltext name and country" />
            <LinkPlayground tools={this.props.tools} url="/dc/type/orgactfr:Activit%C3%A9NAF2008_0?orgact:label=$fulltextaquacult+" value="or country activity by fulltext name" />.
          </li>

          <li className="listespace">
            <LinkPlayground tools={this.props.tools} url="/dc/type/dcmp:Project_0/org_1" value="org_1" /> - downloading all business
            activities to cache them by iterating on @id
            (easier than on orgact:code which would need to do >=and merge same-code activities of different countries, rather than merely >)
            : <LinkPlayground tools={this.props.tools} url="/dc/type/orgact:Activity_0?limit=100&@id=+" value="first query" />, then iterate on activities
            coming after last name returned : <LinkPlayground tools={this.props.tools} url="/dc/type/orgact:Activity_0?limit=100&@id=>http://data.ozwillo.com/dc/type/orgactbg:%D0%94%D0%B5%D0%B9%D0%BD%D0%BE%D1%81%D1%82%D0%9A%D0%98%D0%94_0/BG/14.13+" value="second query" />,
            <LinkPlayground tools={this.props.tools} url="/dc/type/orgact:Activity_0?limit=100&@id=>http://data.ozwillo.com/dc/type/orgactbg:%D0%94%D0%B5%D0%B9%D0%BD%D0%BE%D1%81%D1%82%D0%9A%D0%98%D0%94_0/BG/25.61+" value="third query" />...
          </li>
        </ul>
      </div>
    );
  }
  }
