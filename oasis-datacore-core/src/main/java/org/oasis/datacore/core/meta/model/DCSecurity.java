package org.oasis.datacore.core.meta.model;

public class DCSecurity {
   
   /** default is true */
   private boolean isPublicRead = true;
   /** default is true TODO Q rather false once security is plugged ?? */
   private boolean isPublicWrite = true;

   public boolean isPublicRead() {
      return isPublicRead;
   }

   public void setPublicRead(boolean isPublicRead) {
      this.isPublicRead = isPublicRead;
   }

   public boolean isPublicWrite() {
      return isPublicWrite;
   }

   public void setPublicWrite(boolean isPublicWrite) {
      this.isPublicWrite = isPublicWrite;
   }

}
