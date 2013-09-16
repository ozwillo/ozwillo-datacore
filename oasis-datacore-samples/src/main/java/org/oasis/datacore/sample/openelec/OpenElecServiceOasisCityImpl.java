package org.oasis.datacore.sample.openelec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.oasis.datacore.api.DCObject;
import org.oasis.datacore.api.DatacoreClient;
import org.oasis.datacore.sample.openmairie.Collectivite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * mongodb-backed impl, save for collectivites which are in oasis (as cities)
 * (the simplest way to integrate datacore in the use case of a new app / feature being developed)
 * 
 * TODO or rather reuse local collectivites as cache for remote datacore cities ?!
 * Collectivite would have an additional Oasis URI field (instead of Bureau) and a transient DCObject ref
 * probably harder to manage / sync ??
 * 
 * @author mdutoo
 *
 */
@Service
public class OpenElecServiceOasisCityImpl implements OpenElecService {
   
   @Autowired
   private BureauDao bureauDao;
   @Autowired
   private CollectiviteDao collectiviteDao;
   @Autowired
   private DatacoreClient datacoreClient;
   
   public Bureau getBureauById(String id) {
      Bureau bureau = bureauDao.getById(id);
      completeBureauWithReferencedOasisData(bureau); // or in builder
      return bureau;
   }
   
   public void completeBureauWithReferencedOasisData(Bureau bureau) {
      // loading ref'd collectivite :
      if (bureau.getCollectivite_uri() != null) {
         // or in builder :
         Collectivite collectivite = getCollectiviteByUri(bureau.getCollectivite_uri());
         bureau.setCollectivite(collectivite);
      }
   }

   public List<Bureau> getBureaux() {
      List<Bureau> bureaux = bureauDao.list();

      // datacore integration :
      // or in getter or in onAfterConvert lifecycle event http://static.springsource.org/spring-data/data-document/docs/current/reference/html/#mongodb.mapping-usage.events
      for (Bureau bureau : bureaux) {
         // loading ref'd collectivite :
         completeBureauWithReferencedOasisData(bureau);
      }
      return bureaux;
   }
   
   public void saveBureau(Bureau bureau) throws Exception {
      // datacore integration
      // or in getter or in onBeforeConvert(Save) lifecycle event http://static.springsource.org/spring-data/data-document/docs/current/reference/html/#mongodb.mapping-usage.events
      Collectivite bureauCollectivite = bureau.getCollectivite();
      if (bureauCollectivite != null) {
         if (bureauCollectivite.getDcObject() == null) {
            // ref'd object (collectivite) not saved yet in datacore
            // TODO auto save in datacore ? or require explicit save first ?
            if (/*explicitSaveFirstMode*/false) {
               throw new Exception("collectivite not saved yet in " + bureau);
            }
            saveCollectivite(bureauCollectivite); // sets DCObject with URI or explodes
         }
         bureau.setCollectivite_uri(bureau.getCollectivite().getDcObject().getUri()); // NO ONLY IN level 2 integration
      }

      bureau.setCollectivite(null); // TODO remove, only to avoid putting it now @Transient (else MappingException can't save no id)
      /*WriteResult wr = */bureauDao.save(bureau);
      bureau.setCollectivite(bureauCollectivite); // TODO remove, only to avoid putting it now @Transient
      //TODO explode for manual retry if not saved
   }
   
   /** WARNING changes in level 1 : id becomes uri TODO OR REFACTOR API ?? */
   public Collectivite getCollectiviteById(String uri) {
      return getCollectiviteByUri(uri);
   }
   public Collectivite getCollectiviteByUri(String uri) {
      DCObject dcObject = datacoreClient.get(uri);
      if (dcObject == null) {
         // TODO explode or allow auto new ??????
      }
      Collectivite collectivite = getCollectivite(dcObject);
      // SIMPLE VERSION
      collectivite.setDcObject(dcObject);
      // CACHE VERSION but cache has to be managed (no too much memory) and collectivite still known in it
      dcObject.setBusinessObject(collectivite);
      return collectivite;
   }
   private Collectivite getCollectivite(DCObject dcObject) {
      // or in builder :
      Collectivite collectivite = new Collectivite();
      collectivite.fromDCObject(dcObject);
      return collectivite;
   }

   public List<Collectivite> getCollectivites() {
      // datacore integration :
      // conf
      String mairieCode = "69001_Lyon1";
      String sourceId = "http://www.openmairie.org/" + mairieCode;
      int dataQualityMin = 7;
      // query
      List<DCObject> cities = datacoreClient.get("city", sourceId, dataQualityMin); // TODO paging, sorting
      ArrayList<Collectivite> collectivites = new ArrayList<Collectivite>(cities.size());
      for (DCObject cityDCObject : cities) {
         Collectivite collectivite = getCollectivite(cityDCObject);
         collectivites.add(collectivite);
      }
      return collectivites;
   }
   
   public void saveCollectivite(Collectivite collectivite) throws Exception {
      // datacore integration
      // or in onAfterConvert lifecycle event http://static.springsource.org/spring-data/data-document/docs/current/reference/html/#mongodb.mapping-usage.events

      // or in datacore Java client fw
      DCObject dcObject = collectivite.toDCObject();
      DCObject datacoreDCObject = collectivite.getDcObject();
      if (datacoreDCObject == null) {
         // case of a new object
      } else {
         // TODO get diff to avoid useless save ??? NO could have changed on the other side, if conflicts would explode, BUT so at least saving here tells its up to date 
      }
      /*WriteResult wr = */datacoreClient.save(dcObject); //TODO explode for manual retry if not saved ;
      // NB. if because of optimistic locking failure, local cache will be replaced by datacore OPT TODO with local diffs reapplied if any
      collectivite.setDcObject(dcObject);
   }
   
}
