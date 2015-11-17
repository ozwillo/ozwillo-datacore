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
 * mongodb-backed impl, save for bureau & collectivites which are in Ozwillo (as bureau & city)
 * (the simplest way to integrate datacore in the use case of a new app / feature being developed)
 * 
 * TODO or rather reuse local collectivites as cache for remote datacore cities ?!
 * Collectivite would have an additional Ozwillo URI field (instead of Bureau) and a transient DCObject ref
 * probably harder to manage / sync ??
 * 
 * TODO LATER tx in save()...
 * 
 * @author mdutoo
 *
 */
@Service
public class OpenElecServiceOasisBureauCityImpl implements OpenElecService {
   
   @Autowired
   private BureauDao bureauDao;
   @Autowired
   private CollectiviteDao collectiviteDao;
   @Autowired
   private DatacoreClient datacoreClient;
   /** delegate for level 1 integration */
   @Autowired
   private OpenElecServiceOasisCityImpl openElecServiceOasisCityImpl;

   public Bureau getBureauById(String id) {
      Bureau bureau = bureauDao.getById(id); // NB. only to get uri
      return completeBureau(null, bureau);
   }
   /** TODO add in API level 2 ?? */
   public Bureau getBureauByUri(String uri) {
      DCObject bureauDCObject = datacoreClient.get(uri);
      if (bureauDCObject == null) {
         // TODO explode or allow auto new ??????
      }
      return getBureau(bureauDCObject);
   }
   private Bureau getBureau(DCObject bureauDCObject) {
      return completeBureau(bureauDCObject, null);
   }
   private Bureau completeBureau(DCObject bureauDCObject, Bureau bureau) {
      // or in builder :
      
      if (bureau == null) {
         ///Bureau bureau = new Bureau(); // NO created by DAO since some fields are not stored in datacore but locally (code_canton)
         bureau = bureauDao.getByField("uri", bureauDCObject.get("uri")); // TODO or using spring CRUD repo
         if (bureau == null) {
            // case of object not known locally yet
            bureau = new Bureau();
         }
         
      } else if (bureauDCObject == null) {
         bureauDCObject = datacoreClient.get(bureau.getUri());
      }
      
      bureau.fromDCObject(bureauDCObject);

      // loading ref'd collectivite :
      openElecServiceOasisCityImpl.completeBureauWithReferencedOasisData(bureau);
      
      return bureau;
   }
   
   public List<Bureau> getBureaux() {
      // datacore integration :
      // conf
      String mairieCode = "69001_Lyon1";
      String sourceId = "http://www.openmairie.org/" + mairieCode;
      int dataQualityMin = 7;
      // query (datacore is the reference for the list of bureaux)
      List<DCObject> bureauDCObjects = datacoreClient.get("bureau", sourceId, dataQualityMin); // TODO filter on Lyon, paging, sorting
      ArrayList<Bureau> bureaux = new ArrayList<Bureau>(bureauDCObjects.size());
      for (DCObject bureauDCObject : bureauDCObjects) {
         Bureau bureau = getBureau(bureauDCObject);
         bureaux.add(bureau);
      }
      
      return bureaux;
   }
   
   public void saveBureau(Bureau bureau) throws Exception {
      // datacore integration
      // save refs (Collectivite), then locally first ; for fields not known in datacore, follows pattern of a local cache (i.e. may be wrong)
      this.openElecServiceOasisCityImpl.saveBureau(bureau);

      // then save in datacore
      // or in datacore Java client fw
      DCObject dcObject = bureau.toDCObject();
      DCObject datacoreDCObject = bureau.getDcObject();
      if (datacoreDCObject == null) {
         // case of a new object
      } else {
         // TODO get diff to avoid useless save ??? NO could have changed on the other side, if conflicts would explode, BUT so at least saving here tells its up to date 
      }
      /*WriteResult wr = */datacoreClient.save(dcObject); //TODO explode for manual retry if not saved ;
      // NB. if because of optimistic locking failure, local cache will be replaced by datacore OPT TODO with local diffs reapplied if any
      
      // update local object :
      // if there's no auto code / behaviour on datacore server side, only thing to change is uri (case of new object)
      bureau.setDcObject(dcObject);
      bureau.setUri(dcObject.getUri());
      // TODO (???) else if auto code / behaviour on server (ex. address reformatting, computed properties, metadata extraction & enrichment) rather do :
      // (OPT TODO this possibly using Ozwillo Web Services ?????)
      //bureau.fromDCObject(dcObject);
   }
   
   public Collectivite getCollectiviteById(String uri) {
      // datacore integration : SAME AS OpenElecServiceOasisCityImpl
      return this.openElecServiceOasisCityImpl.getCollectiviteById(uri);
   }

   public List<Collectivite> getCollectivites() {
      // datacore integration : SAME AS OpenElecServiceOasisCityImpl
      return this.openElecServiceOasisCityImpl.getCollectivites();
   }
   
   public void saveCollectivite(Collectivite collectivite) throws Exception {
      // datacore integration : SAME AS OpenElecServiceOasisCityImpl
      this.openElecServiceOasisCityImpl.saveCollectivite(collectivite);
   }
   
}
