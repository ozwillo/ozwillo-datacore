package org.oasis.datacore.core.meta.model;

public class DCSecurity {
   
   /** default is true */
   private boolean isGuestReadable = true;
   /** default is true (but isGuestReadable takes precedence) */
   private boolean isAuthentifiedReadable = true;
   /** default is true
    * NB. writers can't be anonymous, should at least have a named account, like on wikipedia */
   private boolean isAuthentifiedWritable = true;
   /** default is true
    * NB. creators can't be anonymous, should at least have a named account, like on wikipedia */
   private boolean isAuthentifiedCreatable = true;

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
      return isAuthentifiedWritable;
   }

   public void setAuthentifiedWritable(boolean isAuthentifiedWriteable) {
      this.isAuthentifiedWritable = isAuthentifiedWriteable;
   }

   public boolean isAuthentifiedCreatable() {
      return isAuthentifiedCreatable;
   }

   public void setAuthentifiedCreatable(boolean isAuthentifiedCreatable) {
      this.isAuthentifiedCreatable = isAuthentifiedCreatable;
   }

}
