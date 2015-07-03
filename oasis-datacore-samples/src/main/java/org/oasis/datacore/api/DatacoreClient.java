package org.oasis.datacore.api;

import java.util.ArrayList;
import java.util.List;

import org.oasis.datacore.sample.openelec.Bureau;
import org.oasis.datacore.sample.openmairie.Collectivite;
import org.springframework.stereotype.Component;

/**
 * TODO or DatacoreClient<T> with binding to the local type T ???
 * 
 * TODO : refresh, query language & paging & sorting...
 * 
 * @author mdutoo
 *
 */
@Component
public class DatacoreClient {
   
   public static final String DATACORE_BASE_URI = "http://data.ozwillo.com/";
   public static final String LANGUAGE_SPARQL = "SPARQL";

   public DCObject get(String uri) {
      if (uri == null) {
         return null;
      }
      if (!uri.startsWith(DATACORE_BASE_URI)) {
         // TODO Q does datacore store objects with external uris ???
         uri = DATACORE_BASE_URI + uri;
      }
      
      if ((DATACORE_BASE_URI + "city/France/Lyon").equals(uri)) {
         DCObject dcObject = new DCObject();
         dcObject.put("uri", uri);
         dcObject.put("type", "city");
         // TODO source (id ???), version (& audit ?)...
         dcObject.put("name", "Lyon");
         dcObject.put("country", "France");
         return dcObject;
         
      } else if ((DATACORE_BASE_URI + "bureau/Lyon325").equals(uri)) {
         DCObject dcObject = new DCObject();
         dcObject.put("uri", uri);
         dcObject.put("type", "bureau");
         dcObject.put("code", "Lyon325");
         // TODO source (id ???), version (& audit ?)...
         dcObject.put("libelle_bureau", "Lyon");
         dcObject.put("adresse", "École Antoine Charial\n"
            + "25 rue Antoine Charial\n"
            + "69003 Lyon"); // TODO field mapping
         //dcObject.put("code_canton", "canton1"); // said not in datacore bureau metamodel
         dcObject.put("collectivite", DATACORE_BASE_URI + "city" + "/France/Lyon"); // TODO datacore ref (uri)
         return dcObject;
      }
      
      return null;
   }

   /**
    * get by rdf type
    * TODO paging, sorting...
    * @param rdfTypeOrBase rdf:type : TODO Q if ns full rdf:type, else will be prepended by "http://data.ozwillo.com/"
    * @param sourceId TODO itself by default
    * @param dataQualityMin TODO its own default one by default
    * @return
    */
   public List<DCObject> get(String rdfTypeOrBase, String sourceId, int dataQualityMin) {
      // TODO Q what rdf type base for datacore ? if any ??
      //if (!rdfTypeOrBase.startsWith(DATACORE_TYPE_BASE)) {
      //   rdfTypeOrBase = DATACORE_TYPE_BASE + rdfTypeOrBase;
      //}
      if ("city".equals(rdfTypeOrBase)) {
         ArrayList<DCObject> res = new ArrayList<DCObject>();
         DCObject dcObject = new DCObject();
         dcObject.put("uri", DATACORE_BASE_URI + rdfTypeOrBase + "/France/Lyon");
         dcObject.put("type", rdfTypeOrBase);
         // TODO source (id ???), version (& audit ?)...
         dcObject.put("name", "Lyon");
         dcObject.put("country", "France");
         res.add(dcObject);
         dcObject = new DCObject();
         dcObject.put("uri", DATACORE_BASE_URI + rdfTypeOrBase + "/UnitedKingdom/London");
         // TODO source (id ???), version (& audit ?)...
         dcObject.put("name", "London");
         dcObject.put("country", "UnitedKingdom");
         res.add(dcObject);
         return res;
         
      } else if ("bureau".equals(rdfTypeOrBase)) {
         ArrayList<DCObject> res = new ArrayList<DCObject>();
         DCObject dcObject = new DCObject();
         dcObject.put("uri", DATACORE_BASE_URI + rdfTypeOrBase + "/Lyon325");
         dcObject.put("type", rdfTypeOrBase);
         dcObject.put("code", "Lyon325");
         // TODO source (id ???), version (& audit ?)...
         dcObject.put("libelle_bureau", "Lyon");
         dcObject.put("adresse", "École Antoine Charial\n"
            + "25 rue Antoine Charial\n"
            + "69003 Lyon"); // TODO field mapping
         //dcObject.put("code_canton", "canton1"); // said not in datacore bureau metamodel
         dcObject.put("collectivite", DATACORE_BASE_URI + "city" + "/France/Lyon"); // datacore ref (uri), TODO LATER build & return it also (depth) ?!
         res.add(dcObject);
         return res;
         
      }
      
      return new ArrayList<DCObject>(0); // TODO EMPTY_LIST ?
   }

   /**
    * TODO rather object model for query elements
    * @param rdfTypeOrBase
    * @param sourceId
    * @param dataQualityMin
    * @param criteriaFields same as sparql filters
    * @param criteriaOperators
    * @param criteriaValues
    * @param sortFields for sorting ; TODO default per metamodel ???
    * @param sortAscs for sorting
    * @param offset for paging ; TODO for it to work, impl should return deterministic order
    * (else add a default available ORDER BY / require one) see http://lists.w3.org/Archives/Public/public-rdf-dawg-comments/2011May/0017.html
    * @param limit for paging, TODO default (100 ??)
    * @return
    */
   public List<DCObject> get(String rdfTypeOrBase, String sourceId, int dataQualityMin,
         String[] criteriaFields, String[] criteriaOperators, Object[] criteriaValues,
         String[] sortFields, boolean[] sortAscs, int offset, int limit) {
      return this.get(rdfTypeOrBase, sourceId, dataQualityMin);
   }

   /**
    * 
    * @param query see good sparql reference http://rdf.myexperiment.org/howtosparql?page=LIMIT
    * @param language TODO default sparql
    * @param sourceId
    * @param dataQualityMin
    * @return
    * @throws Exception 
    */
   public List<DCObject> get(String query, String language, String sourceId, int dataQualityMin) throws Exception {
      if (language == null) {
         language = LANGUAGE_SPARQL;
      } else if (!LANGUAGE_SPARQL.equals(language)) {
         throw new Exception("Unsupported language " + language + ". Accepted ones are : " + LANGUAGE_SPARQL);
      }
      
      return this.get(query, sourceId, dataQualityMin);
   }

   /**
    * TODO throw ObsoleteDataException if optimistic locking fails
    * @param collectivite
    */
   public void save(DCObject dcObject) {
      if (dcObject.getUri() == null) { // TODO or doesn't exist in datacore
         // case of new object :
         // TODO check that : uri conforms to metamodel-specified format OR / AND generate one
         // TODO if null uri, how to know type ?????????
         String rdfTypeOrBase = dcObject.getType();
         if (rdfTypeOrBase == null) { // TODO or doesn't exist in datacore
            throw new RuntimeException("Invalid DCObject : missing type " + dcObject);
         }
         if ("city".equals(rdfTypeOrBase)) {
            dcObject.put("uri", DATACORE_BASE_URI + rdfTypeOrBase + "/France/Lyon");
         } else if ("bureau".equals(rdfTypeOrBase)) {
            dcObject.put("uri", DATACORE_BASE_URI + rdfTypeOrBase + "/Lyon325");
         }
      }
      //TODO actual (remote) save
      /*Result res = underlayingDatacoreClient.save(collectivite);
      if (res.isObsolete()) {
         // retry auto, several times ? NO can't retry auto because conflicting data must be approved manually
         //res = datacoreClient.save(collectivite);
         // retry manual :
         throw new Exception("Obsolete data, retry manually" + res.getLatest());
      }*/
      
   }

}
