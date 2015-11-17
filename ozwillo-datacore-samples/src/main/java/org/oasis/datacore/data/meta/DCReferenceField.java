package org.oasis.datacore.data.meta;


/**
 * TODO NOT USED, move to -core
 * 
 * TODO Q one single uri field (indexed, search by regexp), or several as here ??
 * 
 * Models the URI of a Datacore Resource (reference or instance),
 * for now also works as URI of a Social Graph Resource (reference).
 * TODO LATER refactor SCURI out of it for Social Graph.
 * 
 * A value modeled by this field is a String that is a valid URI path.
 * 
 * @author mdutoo
 *
 */
public class DCReferenceField extends DCField {
   
   /** Container host ex. data.ozwillo.com, by default set to the current container's.
    * Protocol is assumed to be HTTP (if HTTPS, there must be a redirection) */
   private String container;
   /** Base type ex. city. Corresponds to a type of use and a data governance configuration.
    * For the instance's (rather than reference) URI, by default set to its model's.
    * For Social Graph ex. user (account), organization */
   // TODO alternate extending type SCURI with ex. "account" instead
   private String type;

   
   
}
