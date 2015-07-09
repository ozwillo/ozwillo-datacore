package org.oasis.datacore.rest.server.event;

import org.oasis.datacore.core.meta.SimpleUriService;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.server.resource.ResourceException;
import org.oasis.datacore.server.uri.BadUriException;


/**
 * topic is modelType
 * TODO TODO or store in Model event listener impls' confs, possibly shared & serializable ?!? YES
 * TODO rather on BUILT event i.e. (init) computation, validation...
 * 
 * @author mdutoo
 *
 */
public class DCInitIdEventListener extends DCResourceEventListener implements DCEventListener {

   private String idFieldName = "id";
   
   public DCInitIdEventListener() {
      
   }

   /**
    * helper to create it programmatically (in tests...)
    * @param modelType if not null, inited ; else must be set and inited afterwards
    * (that's auto done when doing model/mixin.addListener(resourceListener)
    * @param idFieldName TODO prevent null
    */
   public DCInitIdEventListener(String modelType, String idFieldName) {
      super(modelType);
      this.setIdFieldName(idFieldName);
      // and must be set and inited afterwards, ex. done in DCModelBase.addListener()
   }

   /**
    * Same, but to allow setting modelType afterwards (by model type itself)
    */
   public DCInitIdEventListener(String idFieldName) {
      super();
      this.setIdFieldName(idFieldName);
      // and must be set and inited afterwards, ex. done in DCModelBase.addListener()
   }

   @Override
   public void handleEvent(DCEvent event) throws AbortOperationEventException {
      if (DCResourceEvent.Types.ABOUT_TO_BUILD.name.equals(event.getType())) { // TODO ABOUT_TO_CREATE ??
         DCResourceEvent re = (DCResourceEvent) event;
         DCResource r = re.getResource();
         Object id = r.get(this.idFieldName);
         
         if (r.getUri() == null) {
            // TODO can't happen when called in postData (but in test shortcuts or build()
            // after create(String containerUrl, String modelType), yes)
            if (id == null) {
               throw new AbortOperationEventException(new ResourceException("Missing id field "
                     + idFieldName + " in resource with null uri of types " + r.getTypes(), r, null));
            }
            r.setUri(SimpleUriService.buildUri(r.getModelType(), id.toString()));
            
         } else if (id == null) {
            String uri = r.getUri();
            try {
               DCURI dcUri = uriService.parseUri(uri);
               r.set(this.idFieldName, dcUri.getId());
            } catch (BadUriException e) {
               // should not happen
               throw new AbortOperationEventException(new ResourceException(
                     "Error parsing existing uri before creation", e, r, null));
            }
         }
      }
   }

   public String getIdFieldName() {
      return idFieldName;
   }

   public void setIdFieldName(String idFieldName) {
      this.idFieldName = idFieldName;
   }

}
