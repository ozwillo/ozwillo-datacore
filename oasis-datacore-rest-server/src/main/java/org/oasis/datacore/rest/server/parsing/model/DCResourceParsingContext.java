package org.oasis.datacore.rest.server.parsing.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCModel;


/**
 * Keeps the state of parsing (path in model instance) in order to be able
 * to display error messages that pinpoint to error location.
 * 
 * @author mdutoo
 *
 */
public class DCResourceParsingContext {
   
   private Stack<DCResourceValue> resourceValueStack = new Stack<DCResourceValue>(); // TODO or of String only ??

   // TODO or same list, with error level in Log and boolean hasError ?!?
   private List<ResourceParsingLog> errors = null;
   private List<ResourceParsingLog> warnings = null;
   
   /**
    * For multi resource parsing, requires enter(DCModel, uri) first
    */
   public DCResourceParsingContext() {
      
   }
   
   /**
    * For single resource parsing
    * @param model
    * @param uri
    */
   public DCResourceParsingContext(DCModel model, String uri) {
      this.enter(model, uri);
   }
   
   public void enter(DCModel model, String uri) {
      this.resourceValueStack.add(new DCResourceValue(model.getName(), null, uri));
   }
   
   public void enter(DCField field, Object value) {
      String fullValuedPath;
      if (this.resourceValueStack.isEmpty()) {
         fullValuedPath = "Missing root model name";
      } else {
         DCResourceValue previousResourceValue = this.resourceValueStack.peek();
         fullValuedPath = previousResourceValue.getFullValuedPath() + "/"
               + ((value != null) ? value : field.getName()); // TODO better fullValuedPath,
         // also xpath-like in addition to being a value path (which is particularly ugly in list case)
      }
      this.resourceValueStack.add(new DCResourceValue(fullValuedPath, field,  value));
   }
   public void exit() {
      this.resourceValueStack.pop();
   }
   
   public void addError(String message) {
      this.addError(message, null);
   }

   public void addError(String message, Exception ex) {
      String fieldFullPath = this.resourceValueStack.peek().getFullValuedPath();
      this.getOrCreateErrors().add(new ResourceParsingLog(fieldFullPath, message, ex));
   }

   public boolean hasErrors() {
      return this.errors != null;
   }
   
   public List<ResourceParsingLog> getErrors() {
      return this.errors;
   }
   
   public void addWarning(String message) {
      this.addWarning(message, null);
   }

   public void addWarning(String message, Exception ex) {
      String fieldFullPath = this.resourceValueStack.peek().getFullValuedPath();
      this.getOrCreateWarnings().add(new ResourceParsingLog(fieldFullPath, message, ex));
   }

   public boolean hasWarnings() {
      return this.warnings != null;
   }
   
   public List<ResourceParsingLog> getWarnings() {
      return this.warnings;
   }
   
   private List<ResourceParsingLog> getOrCreateErrors() {
      if (this.errors == null) {
         this.errors = new ArrayList<ResourceParsingLog>();
      }
      return this.errors;
   }

   private List<ResourceParsingLog> getOrCreateWarnings() {
      if (this.warnings == null) {
         this.warnings = new ArrayList<ResourceParsingLog>();
      }
      return this.warnings;
   }
   

   // TODO extract to ResourceParsingService
   public static String formatParsingErrorsMessage(DCResourceParsingContext resourceParsingContext,
         boolean detailedErrorsMode) {
      // TODO or render (HTML ?) template ?
      StringBuilder sb = new StringBuilder("Parsing aborted, found "
            + resourceParsingContext.getErrors().size() + " errors "
            + ((resourceParsingContext.hasWarnings()) ? "(and "
            + resourceParsingContext.getWarnings().size() + " warnings) " : "")
            + ".\nErrors:");
      for (ResourceParsingLog error : resourceParsingContext.getErrors()) {
         sb.append("\n   - in context ");
         sb.append(error.getFieldFullPath());
         sb.append(" : ");
         sb.append(error.getMessage());
         if (error.getException() != null) {
            sb.append(". Exception message : ");
            sb.append(error.getException().getMessage());
            if (detailedErrorsMode) {
               sb.append("\n      Exception details : \n\n");
               sb.append(ExceptionUtils.getFullStackTrace(error.getException()));
               sb.append("\n");
            }
         }
      }
      
      if (resourceParsingContext.hasWarnings()) {
         // TODO or render (HTML ?) template ?
         sb.append("\nWarnings:");
         for (ResourceParsingLog warning : resourceParsingContext.getWarnings()) {
            sb.append("\n   - for field ");
            sb.append(warning.getFieldFullPath());
            sb.append(" : ");
            sb.append(warning.getMessage());
            if (warning.getException() != null) {
               sb.append(". Exception message : ");
               sb.append(warning.getException().getMessage());
               if (detailedErrorsMode) {
                  sb.append("\n      Exception details : \n\n");
                  sb.append(ExceptionUtils.getFullStackTrace(warning.getException()));
                  sb.append("\n");
               }
            }
         }
      }
      
      return sb.toString();
   }

}
