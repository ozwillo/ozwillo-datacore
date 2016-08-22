import React from 'react';
import LinkPlayground from '../linkPlayground.js';

export default class faqs extends React.Component{
  render() {
    var jsonExample = {
      "owners": [ /* have all rights, including reading and changing rights */
        "u_2239bc06-5f33-49dd-af99-b5c87da055ab", /* 2239bc06-5f33-49dd-af99-b5c87da055ab is the id of a user */
        "9056cde5-8135-4fd9-a913-4c3103f19fac" /* id of a Kernel organization */
      ],
      "writers": [], /* have write (and delete) rights, in addition to read rights*/
      "readers": [] /* have read rights */
    };
    return (
      <div>
        <h2>FAQS</h2>
        <h3>Ressource FAQ</h3>
        <ul>
          <li><strong>"reached maxScan (300) before document limit"</strong> errors (or no return) on ex. "by geo area" queries : ask model designer to add the right
            indexes (and first to check that said field has values). Workaround : query in country-specific types.</li>

          <li><strong>"operation exceeded time limit"</strong> errors on ex. geoname "by geoname:id" queries : same as above, ask model designer to add the
            right indexes (and first to check that said field has values). Workaround : use the URI (@id field) as ID.</li>

          <li><strong>"Attempting a fulltext search on a field that is not configured so"</strong> errors : ask model designer to enable the right
            fulltext indexes. Workaround : query on odisp:name.</li>

          <li><strong>No return from a fulltext search</strong> : data can't be returned if it hasn't been saved since enabled for fulltext, so
            try saving some, else ask data provider to do so, possibly in bulk.</li>

          <li><strong>"Access Denied" or "Forbidden"</strong> : see Rights FAQ.</li>

          <li><strong>"Can't get Resource, more than one with same URI [uri] (in projects [a, b seeing a]): has been forked implicitly, should also be forked explicitly"</strong> :
            if this Resource shouldn't exist in project a, remove it from there. Otherwise, add its URI in b's dcmp:forkedUris. Then you will also be
            able to this Resource it from b if it should not be there, in which case don't forget to remove said URI from b's dcmp:forkedUris.</li>

          <li><strong>Requests made in your own code doesn't work</strong>, though in the Playground they do : your code does not do everything it should.
            Enable Firebug in Firefox and have a look in the "Console" tab at the actual request that the Playground makes: URL, headers, body...
            and find out where your code's request differs.</li>

          <li><strong>Query</strong> on Resource links doesn't work, though on primitive values they do : maybe you haven't encoded the Resource link's URI
            request parameter, as it should be per HTTP specs (so since URIs already have some encoded part, they're actually double encoded).
            It's actually a subset of the previous FAQ entry.</li>

          <li><strong>"Version of data resource to update is required in PUT"</strong> error : to update an existing data, you must PUT (rather than POST)
            it with the current version number (HTTP Entity Tag, used for optimistic locking to ensure data integrity). And if you don't store said
            current version locally, you must first GET it from server. You must also handle the case of the version being changed while your request
            is still reaching the server, by a retrying it a few times.</li>

          <li><strong>Wrong or inconsistent data</strong> : see known cases in issues tagged "data inconsistency" on github. If not yet there,
            report it in #62 for geo & org data or as such a new issue tagged by "data inconsistency".</li>

          <li>All reported FAQ cases : see issues tagged "FAQ" on github.</li>
        </ul>

        <h3>Project and Models FAQ</h3>
        <ul>
          <li><strong>Model failed to load</strong>: the Model is persisted as a Resource alright (its .../dcmo:model_0/modelName URI works) but getting its Resources
            fails with a ModelNotFoundException. This means that the Model can't be loaded from its Resource representation. To solve it, try to rePOST
            the Model's Resource representation (ex. using edit / POST or import playground features), which will trigger Model reload, and if it fails
            try to patch it according to the error message.</li>
          <li><strong>Can't write Resource ("access denied")</strong> : check that you have the rights to, and (using ex. the edit / POST playground feaure) that its Model
            is not among its Project's dcmp:frozenModelNames or dcmp:allowedModelPrefixes.</li>
          <li><strong>"Bad URI syntax: Illegal character in path at index n"</strong> (last index of the model URI) : check that the mixin name is not written in
            the "Mixin" column with an (illegal) invisible character such as a space at the end. </li>
        </ul>
        <h3>Rights FAQ</h3>
        <ul>
          <li className="listespace"><strong>"Access Denied" or "Forbidden"</strong> : maybe you're trying to write from another project than this Resource's, or to write a frozen model or
            with a non-allowed prefix (see its project's dcmp:frozenModelNames and dcmp:allowedModelPrefixes respectively or patch them using the
            edit / POST playground feature). Or maybe the Datacore can't yet see that a new Kernel organization has been add to those you belong to,
            in which case log out and in again or wait for 5 minutes. Otherwise, check that you have the rights to do that or patch them,
            see below.</li>
          <li className="listespace"><strong>How to see Resource-level permissions using the Playground</strong> : Resource-level permissions are returned when querying as owner (or admin) in
            debug mode. Therefore to check them in the Playground, go to the said Resource by entering ex. <LinkPlayground tools={this.props.tools} url="/dc/type/poitour:Geoloc_0/32949" value="/dc/type/poitour:Geoloc_0/32949" />
            in the Playground URI address bar, then click on the colon (":") of the URI field (@id) to build a single result query such as this one, then
            go in debug mode by clicking on the "?" button, and finally look in the results for the "ownedEntities" part
            (that only contains data Resources that you have owner / admin rights on, for data security reasons) and within in the "owners", "writers"
            and "readers" entity fields. See below how to change them.</li>
          <li className="listespace">
            How to change Resource-level permissions using the Swagger UI : To change Resource-level rights on a Resource, use the Swagger part at the bottom of
            the Playground, and more precisely operations in the "r : Rights management (add/remove/flush)" group (after selecting the Resource's Project in the
            top-level dropdown box if not done yet):<br/>
          <div className="ui ordered list">
            <p className="item">Do a GET /dc/r/... by providing type, id and current version of the Resource (if you don't know it, look
              the Resource up in the Playground) and clicking on the "Try it out" button. This returns the Resource's current rights, ex.:
            </p>
            <div className="ui segment">
              GET /dc/r/orgtr:Organizasyon_0/TR/27408293994/0
              <pre>{JSON.stringify(jsonExample, null, 2) }</pre>
            </div>

            <p className="item">Copy and paste the result in the body of a PUT /dc/r/..., change it as you wish, then again providing
              type, id and same current version, and do it (click on the "Try it out" button). Now rights should have changed.</p>
            <p className="item">To check that rights have changed, either use debug mode (see above) or do a GET /dc/r/... again BUT with the new current
              version (should have been incremented by 1). </p>
          </div>
        </li>
      </ul>

    </div>
  );
}
}
