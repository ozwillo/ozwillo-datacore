package org.oasis.datacore.rest.server.parsing.model;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.DCURI;
import org.oasis.datacore.rest.api.util.UriHelper;

public class DCResourceValue {

   /** for lazy computing of fullValuedPath */
   private DCResourceValue previousResourceValue;
   private DCField field;
   private Object value;
   private long index;
   /** lazily built */
   private String fullValuedPath;
   
   public DCResourceValue(DCResourceValue previousResourceValue, DCField field, Object value) {
      this.previousResourceValue = previousResourceValue;
      this.field = field;
      this.value = value;
   }

   public DCResourceValue(DCResourceValue previousResourceValue, DCField field, Object value, long index) {
      this(previousResourceValue, field, value);
      this.index = index;
   }

   /**
    * Lazy impl
    * @return
    */
   public String getFullValuedPath() {
      if (fullValuedPath == null) {
         if (previousResourceValue == null) {
            if (value == null) {
               fullValuedPath = "Missing root model name";
            } else {
               //fullValuedPath = value.toString(); // too long start, said anyway on top
               fullValuedPath = "";
            }
            
         } else {
            DCField previousField = previousResourceValue.getField();
            String fieldNameToDisplay;
            if (previousField != null && "list".equals(previousField.getType())) {
               // list element field : don't display useless name but index
               fieldNameToDisplay = String.valueOf(index); // index in list
            } else {
               fieldNameToDisplay = field.getName();
            }
            StringBuilder sb = new StringBuilder(previousResourceValue.getFullValuedPath());
            sb.append('/');
            sb.append(fieldNameToDisplay);
            switch (field.getType()) {
            // list or map : don't display value
            case "list" :
            case "map" :
               break;
             // (sub) resource : display iri (type + id)
            case "resource":
               String uri;
               if (value instanceof Map<?,?>) {
                  uri = (String) ((Map<?,?>) value).get(DCResource.KEY_URI);
               } else if (value instanceof String) {
                  uri = (String) value;
               } else if (value instanceof DCResource) {
                  uri = (String) ((DCResource) value).getUri();
               } else {
                  uri = null;
               }
               sb.append('[');
               if (uri == null) {
                  sb.append("missing uri!");
               } else {
                  DCURI dcUri;
                  try {
                     dcUri = UriHelper.parseUri(uri);
                     //sb.append(dcUri.getType()); // usually known by the field already
                     //sb.append('/');
                     String uriId = dcUri.getId();
                     int lastSlashId = uriId.lastIndexOf('/');
                     if (lastSlashId == -1) {
                        sb.append(uriId);
                     } else {
                        sb.append("...");
                        sb.append(uriId.substring(lastSlashId));
                     }
                  } catch (MalformedURLException | URISyntaxException e) {
                     sb.append("bad URI : "); // NB. reported in an Error elsewhere
                     sb.append(e.getMessage());
                  } catch (Throwable t) {
                     sb.append("unknown error on URI : "); // NB. reported in an Error elsewhere
                     sb.append(t.getMessage());
                  }
               }
               sb.append(']');
               break;
            // string formatted fields : quote
            case "string" :
            case "date" :
               sb.append("['");
               sb.append(value);
               sb.append("']");
               break;
            default:
               // don't quote
               sb.append('[');
               sb.append(value);
               sb.append(']');
            }
            fullValuedPath = sb.toString();
         }
      }
      return fullValuedPath;
   }

   public void setFullValuedPath(String fullValuedPath) {
      this.fullValuedPath = fullValuedPath;
   }

   public DCField getField() {
      return field;
   }

   public void setField(DCField field) {
      this.field = field;
   }

   public Object getValue() {
      return value;
   }

   public void setValue(Object value) {
      this.value = value;
   }
   
}
