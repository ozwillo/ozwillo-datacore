package org.oasis.datacore.sample.openelec;

import java.util.List;

import org.oasis.datacore.sample.openmairie.Collectivite;


/**
 * Sample to study what means integrating oasis datacore in an existing app
 * 
datacore integration levels & alts :
* 1. fully replace (an entity) by datacore ; Alt1 simple (entity refers to DCObject), Alt2 opposite (requires client cache & management) ; which implies...
* 2. ref (from an entity) to datacore ; Alt1 add ref'd uri field to entity, Alt2 NO SAVE EXCEPTION REQUIRES ADDITIONAL SCHEMA put in datacore technical id (that the entity refers to)
* 3. replace entity fields by those in datacore ; Alt1 add (id) uri field to entity, Alt2 Alt2 NO SAVE EXCEPTION REQUIRES ADDITIONAL SCHEMA put in datacore technical id (of entity)
 * 
 * @author mdutoo
 *
 */
public interface OpenElecService {

   Bureau getBureauById(String id);
   List<Bureau> getBureaux();
   void saveBureau(Bureau bureau) throws Exception;
   Collectivite getCollectiviteById(String id);
   List<Collectivite> getCollectivites();
   void saveCollectivite(Collectivite collectivite) throws Exception;
   
}