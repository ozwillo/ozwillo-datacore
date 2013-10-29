package org.oasis.datacore.rest.server.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCModel;

public class DCResourceParsingContext {
   
   private Stack<DCResourceValue> resourceValueStack = new Stack<DCResourceValue>(); // TODO or of String only ??

   // TODO or same list, with error level in Log and boolean hasError ?!?
   private List<ResourceParsingLog> errors = null;
   private List<ResourceParsingLog> warnings = null;
   
   public DCResourceParsingContext(DCModel model, String uri) {
      this.resourceValueStack.add(new DCResourceValue(model.getName(), null, uri));
   }
   
   public void enter(DCField field, Object value) {
      String fullValuedPath;
      if (this.resourceValueStack.isEmpty()) {
         fullValuedPath = "Missing root model name";
      } else {
         DCResourceValue previousResourceValue = this.resourceValueStack.peek();
         fullValuedPath = previousResourceValue.getFullValuedPath() + "/"
               + ((value != null) ? value : field.getName()); 
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
   
}
