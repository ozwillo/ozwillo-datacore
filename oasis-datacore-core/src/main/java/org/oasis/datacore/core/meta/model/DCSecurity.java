package org.oasis.datacore.core.meta.model;

public class DCSecurity {
   
   /** default is true */
   private boolean isGuestReadable = true;
   /** default is true (but isGuestReadable takes precedence) */
   private boolean isAuthentifiedReadable = true;
   /** default is true
    * NB. writers can't be anonymous, should at least have a named account, like on wikipedia */
   private boolean isAuthentifiedWriteable = true;

   public boolean isGuestReadable() {
      return isGuestReadable;
   }

   public void setGuestReadable(boolean isGuestReadable) {
      this.isGuestReadable = isGuestReadable;
   }

   public boolean isAuthentifiedReadable() {
      return isAuthentifiedReadable;
   }

   public void setAuthentifiedReadable(boolean isAuthentifiedReadable) {
      this.isAuthentifiedReadable = isAuthentifiedReadable;
   }

   public boolean isAuthentifiedWritable() {
      return isAuthentifiedWriteable;
   }

   public void setAuthentifiedWritable(boolean isAuthentifiedWriteable) {
      this.isAuthentifiedWriteable = isAuthentifiedWriteable;
   }

}
