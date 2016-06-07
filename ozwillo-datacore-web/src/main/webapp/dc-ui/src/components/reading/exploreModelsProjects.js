import React from 'react';

export default class ExploreModelsProjects extends React.Component{

  render() {
    return (
      <div>
        <h2>Explore models and projects</h2>
        <p>
          A <strong>project</strong> is a business-consistent set of models and their resources, and can see those of its visible
          projects (its dependencies). All interactions occur within the context of the current project, but some
          may be prevented : for instance, it is usually forbidden to write models in another project than the current
          one. A project can have forked URIs i.e. that cannot be seen in visible projects, which mostly allows to
          fork models i.e. redefine / override its own version of a model.
        </p>
        <p>
          To <strong>start exploring available models</strong>, select your chosen project (i.e. the point of view that you wish to explore from,
          ex. <a className="project">geo_1</a> for geographical jurisdictions such as countries and cities) or merely the default <a className="project">oasis.main</a>
          project in the dropdown above the Playground UI. This will display in the Playground the <strong>list of storage models</strong>
          (ex. in <a className="project">geo_1</a> <a className="playground" onClick={this.props.callAPIUpdatePlaygroundOnClick} href="/dc/type/pli:city_0?limit=1" >geo:Area_0</a> )
          and for each of them a link to models they store (ex. models stored in geo:Area_0 ).
        <br/>It also displays the <strong>list of pure mixins</strong> and for each of them a link to models that
          use them as mixins, that are local to the selected project. Note that this is why there are none in the geo project,
          which makes resources and models of <a className="project">geo_1</a> visible but doesn't have any of its own.
        </p>
        <p>
          Further, <strong>querying models</strong> as Resources allows to know in which Model type(s) a given kind of Resources can be queried, for instance :
        </p>
        <ul>
          <li>
            <a className="playground">dcmo:globalMixins=pl:place_0</a>: lists Models that describe a location i.e. have a pl:shape WKS point, such as : <a className="playground">co:company_0</a>, whose WKS can
              then be retrieved, for instance by GET <a className="playground">/dc/type/co:company_0</a>.
            </li>
            <li>
              <a className="playground">dcmo:globalFields.dcmf:name=plo:name</a> : lists Models that "officially" have a
                country (i.e. having the "official" field for a country in Ozwillo Datacore), and that can therefore be queried on that, such as :
                <ul>
                  <li><a className="playground">plo:country_0</a></li>
                  <li><a className="playground">pli:city_0</a></li>
                  <li><a className="playground">co:company_0</a></li>
                  <li><a className="playground">cityareauseit:urbanAreaDestinationOfUse_0</a></li>
                  <li><a className="playground">cityarea:cityArea_0</a></li>
                </ul>
                They can then be queried by country, for instance by:
                <ul>
                  <li>GET <a className="playground">/dc/type/plo:country_0</a></li>
                  <li>GET <a className="playground">/dc/type/pli:city_0</a></li>
                  <li>GET <a className="playground">/dc/type/co:company_0</a></li>
                  <li>GET <a className="playground">/dc/type/cityareauseit:urbanAreaDestinationOfUse_0</a></li>
                  <li>GET <a className="playground">/dc/type/cityarea:cityArea_0</a></li>
                </ul>
              </li>
            </ul>

            <h4>Here are the generic projects :</h4>
            <ul>
              <li><a className="project">oasis.main</a> is the default project and shows all current stable versions of projects of public interest</li>
              <li><a className="project">oasis.meta</a> contains the metamodel and is visible by all other projects</li>
              <li><a className="project">oasis.sample</a> contains unit test technical samples</li>
              <li><a className="project">oasis.sandbox</a> can be used by anyone to test the Datacore with its own models and resources, especially in the Import UI</li>
            </ul>


            <h4>Here are some of the most often reused projects :</h4>
            <ul>
              <li><a className="project">geo_1</a> (major version 1) contains all geographical jurisdictions (such as countries and cities), while geo makes its current stable version visible whatever its number </li>
              <li><a className="project">org_1</a> (major version 1) contains all public and private organizations as well as persons and also sees geo_1, while org makes its current stable version visible whatever its number </li>
            </ul>

            <h4>Here are project-level Rights features :</h4>
            <ul>
              <li><em>dcmp:securityConstraints</em> : if any, checked firsthand whatever the resource.</li>
              <li><em>dcmp:securityDefaults</em> : used when !modelLevelSecurityEnabled or no security found in model hierarchy ; null means global defaults (depends on devmode) ;
                in case of multi-project storage models (such as dcmi:mixin_0 i.e. the metamodel), is directly used instead of model-level security which makes no
                sense (because without that, models of any project would only be editable by oasis.meta writers).</li>
              <li><em>dcmp:modelLevelSecurityEnabled</em> : allows different security levels in models/mixins (even in same resource), with permissions that are defined
                at Resource-level allowing the most rights (ex. owner on orgpr:PrivateOrganization_0 fields), and other permissions being allowed by model- or project-level
                security (ex. reader on org:Organization_0 fields). Defaults to false.</li>
              <li><em>dcmp:useModelSecurity</em> : use model security instead of project security as default security ; if disabled, model security has not to be
                defined and is replaced by project securityDefaults (even and especially if modelLevelSecurityEnabled !) and is the same across all models
                of this project.</li>
              <li><em>dcmp:visibleSecurityConstraints</em> : allows generic conf & constraints on the project to visible project relation without having to manage &
                store a visibleProjectRelation object ; is checked firsthand if any WHATEVER THE RESOURCE (null dataEntity) when checking rights in a model
                from another current project. Typically used to prevent from writing from a project in one of its dependency project's models.</li>
            </ul>
            <ul>
              <h4>Here are more project-level features :</h4>
              <li><em>dcmp:forkedUris</em> : add a model URI there to allow to fork it i.e. to import it in this project even if it is already in one of its visible
                projects. This also works with any data resource outside (meta) models, but it shouldn't be abused. In details, resources listed here are
                handled as if, only for them, their project did not inherit / have any other project visible.</li>
              <li><em>dcmp:frozenModelNames</em> : allows to freeze (forbid change on) some of this project's models or all of them using the '*' wildcard, once
                they are stable and in order to be able to use them (create resources in them) without risking these becoming obsolete at some point.</li>
              <li><em>dcmp:allowedModelPrefixes</em> : if any, only models whose name starts by any one of those prefixes ('*' wildcard also allowed, to ease switching it
                on and off) can be changed in this project, in order to avoid to add models that belong to another project by mistake, such as POSTing an org
                model while importing geo elements using the master model file.</li>
            </ul>
          </div>
        );
      }
    }
