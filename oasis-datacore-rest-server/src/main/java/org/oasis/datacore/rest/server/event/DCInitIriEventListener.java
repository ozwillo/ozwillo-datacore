package org.oasis.datacore.rest.server.event;

import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.server.parsing.exception.ResourceParsingException;


/**
 * topic is modelType
 * TODO TODO or store in Model event listener impls' confs, possibly shared & serializable ?!? YES
 * TODO rather on BUILT event i.e. (init) computation, validation...
 * 
 * @author mdutoo
 *
 */
public class DCInitIriEventListener extends DCResourceEventListener implements DCEventListener {

   private String idFieldName = "id";
   
   public DCInitIriEventListener() {
      
   }

   /**
    * helper to create it programmatically (in tests...)
    * @param modelType if not null, inited ; else must be set and inited afterwards
    * (that's auto done when doin model/mixin.addListener(resourceListener)
    * @param idFieldName
    */
   public DCInitIriEventListener(String modelType, String idFieldName) {
      super(modelType);
      this.setIdFieldName(idFieldName);
      if (modelType != null) {
         this.init();
      } // else must be set and inited afterwards, ex. done in DCModelBase.addListener()
   }

   @Override
   public void handleEvent(DCEvent event) throws AbortOperationEventException {
      if (DCResourceEvent.Types.ABOUT_TO_CREATE.name.equals(event.getType())) {
         DCResourceEvent re = (DCResourceEvent) event;
         DCResource r = re.getResource();
         Object id = r.getProperties().get(this.idFieldName);
         if (r.getUri() == null) {
            // TODO SHOULD NOT HAPPEN...
            if (id == null) {
               throw new AbortOperationEventException("Missing id field " + idFieldName
                     + " in resource with null uri of types " + r.getTypes());
            }
            r.setUri(resourceService.buildUri(r.getTypes().get(0), id.toString()));
         } else if (id == null) {
            String uri = r.getUri();
            try {
               DCURI dcUri = resourceService.parseUri(uri);
               r.set(this.idFieldName, dcUri.getId());
            } catch (ResourceParsingException e) {
               // should not happen
               throw new AbortOperationEventException("Error parsing uri before creation", e);
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
