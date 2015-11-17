package org.oasis.datacore.sample.openelec;

import java.util.List;

import org.oasis.datacore.sample.openmairie.Collectivite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Full mongodb backed impl
 * 
 * @author mdutoo
 *
 */
@Service
public class OpenElecServiceImpl implements OpenElecService {
   
   @Autowired
   private BureauDao bureauDao;
   @Autowired
   private CollectiviteDao collectiviteDao;
   
   public Bureau getBureauById(String id) {
      return bureauDao.getById(id);
   }
   
   public List<Bureau> getBureaux() {
      List<Bureau> bureaux = bureauDao.list();
      return bureaux;
   }
   
   public void saveBureau(Bureau bureau) throws Exception {
      /*WriteResult wr = */bureauDao.save(bureau);
      //TODO explode for manual retry if not saved
   }
   
   public Collectivite getCollectiviteById(String id) {
      return collectiviteDao.getById(id);
   }

   public List<Collectivite> getCollectivites() {
      return collectiviteDao.list();
   }
   
   public void saveCollectivite(Collectivite collectivite) throws Exception {
      /*WriteResult wr = */collectiviteDao.save(collectivite);
      //TODO explode for manual retry if not saved
   }
   
}
