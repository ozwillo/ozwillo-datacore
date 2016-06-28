import React from 'react';
import LinkPlayground from '../linkPlayground.js';

export default class apiDetails extends React.Component{

  render() {
    var jsonTorino = {"v": "Torino", "l": "it"};
    var jsonTurin = {"v": "Turin","l": "fr"};
    return (
      <div>
        <h2>API details</h2>
        <p>
          This <strong>endpoint</strong> has for base URL <a href="https://data.ozwillo-preprod.eu">https://data.ozwillo-preprod.eu</a> (URL that must be called by clients using HTTP)
          and for container URL <a href="http://data.ozwillo.com">http://data.ozwillo.com</a> (URL that must be used to build Datacore Data resource URIs).
        </p>
        <p>
          <strong>URIs</strong> that uniquely identify Datacore Data resource are in the form $containerUrl/dc/type/$type/$id, where $containerUrl
          is unique for a container (ex. http://data.ozwillo.com), $type is among available model types and $id is the resource's
          business ID (therefore relative to tjee model type). Also note that /dc/type/$type/$id is the Resource's Internal Resource
          Identifier (IRI).
        </p>
        <p>
          IDs have 3 constraints : they must be valid URL endings, unique (not enforced yet), and if possible readable and representative
          of the resource.
        </p>
        <p>
          URIs must be valid URLs meaning that all non safe characters (beyond [0-9a-zA-Z]$-_.+!*'()) including reserved characters $&+,/:;=?@
          outside of their purpose such as '?' within $type) must be URL-encoded.It's bad practice for model names to contain some.
        </p>
        <p>
          URIs of embedded Resources (as sub Resource within another top level Resource) are by default $containerUrl/dc/type/$subResourceType/$resourceId/$subResourceJsonPath,
          but can be something else if parsing is defined consistently within $subResourceType.
        </p>
        <h3>Values</h3>
        <p>Values are formatted in JSON as follows :</p>
        <ul>
          <li>
            Date as <a href="https://en.wikipedia.org/wiki/ISO_8601">ISO8601</a> such as "2014-01-08T09:31:19.062Z", "2014-01-08T10:31:19.062+01:00". Put your (data or application) timezone in your dates so that they can
            be shared across the world while staying readable for your local use case.
          </li>
          <li>
            Long and double as string.
          </li>
          <li>
              i18n as a list of translation objects (like JSONLD), for instance : [{JSON.stringify(jsonTorino) }, {JSON.stringify(jsonTurin) }].
              It allows language-agnostic lookups such as <LinkPlayground tools={this.props.tools} url="/dc/type/pli:city_0?pli:name_i18n=Torino" value="/dc/type/pli:city_0?pli:name_i18n=Torino" />
              (or using the exact field path <LinkPlayground tools={this.props.tools} url="/dc/type/pli:city_0?pli:name_i18n.v=Torino" value="/dc/type/pli:city_0?pli:name_i18n.v=Torino" />) so that values are
              useful even if not untranslated and therefore are not siloed in their language, but also language-scoped lookups
              such as <LinkPlayground tools={this.props.tools} url="/dc/type/pli:city_0?pli:name_i18n.it=Torino" value="/dc/type/pli:city_0?pli:name_i18n.it=Torino" /> (or using the exact field path through $elemMatch
              <LinkPlayground tools={this.props.tools} url='/dc/type/pli:city_0?pli:name_i18n=$elemMatch{"v":"Torino","l":"it"}' value='/dc/type/pli:city_0?pli:name_i18n=$elemMatch{"v":"Torino","l":"it"}' /> ). Moreover, language can also be specified once
              for the whole query in a l or @language parameter such as in <LinkPlayground tools={this.props.tools} url="/dc/type/pli:city_0?pli:name_i18n=Torino&l=it" value="/dc/type/pli:city_0?pli:name_i18n=Torino&l=it" />.
              Beyond such exact lookups, all string operators can be used, and most usefully $fulltext (must be explicitly enabled on the field)
              and $regex.
          </li>
        </ul>

        <h3>Advanced field features</h3>
        <p>They are documented in the Models' import wiki and templates, all linked from their Import UI. Here's a quick list : required,
          defaultValue, queryLimit, aliasedStorageNames, readonly ; string & i18n : fulltext ; i18n : defaultLanguage ; resource : resourceType ;
          list : listElementField, isSet, keyFieldName ; map : mapFields. </p>
        <h3>Operations</h3>
        <p>
            Operations are specified in details below within the Swagger UI playground. You can also :
        </p>
        <ul>
            <li>"postAllDataInType" for typed updates (version)</li>
            <li>"findDataInType" for typed queries (criteria including $fulltext, sort, ranged query- or iterator-based pagination, debug mode, view).</li>
        </ul>
        <p>
            Note that typed operations where type of data is provided in URL are usually more efficient : '*inType' methods are faster, finder queries with type even more.
        </p>
        <h3>Authentication</h3>
        <ul>
          <li>In production (and integration), Ozwillo Kernel OAuth2 authentication must be used, i.e.
          <a href="https://github.com/ozwillo/ozwillo-datacore/wiki/How-to-use-OAuth2-with-Datacore">HTTP requests' Authorization</a>
          header must be "Bearer &lt;valid OAuth2 Bearer / Access Token&gt;	" (see how to get one).This implies that you must first
          register your application with the Kernel (see documentation</li>
          <li>In dev mode, there is a mock authentication system that supports Basic Auth (i.e. HTTP requests' Authorization header
          must be "Basic &lt;base 64-encoding of username:password&gt;	"), for a few users without password, see available ones in configuration.</li>
          <li>Datacore API's Swagger UI allows to provide said Authorization header, with a default value that logs as admin in dev mode.</li>
        </ul>
      </div>
    );
  }
}
