package org.oasis.datacore.core.entity.mongodb;

import java.net.URI;
import java.net.URISyntaxException;


/**
 * and not java.net.URI because the path should start by /,
 * and not 10gen's Mongo(Client)URI because it may be obsoleted again in the future
 * @author mdutoo
 *
 */
public class MongoUri {
   
   private String host;
   private int port;

   private String database;
   
   public MongoUri(String host, int port, String database) {
      this.host = host;
      if (port < 0) {
         port = 27017; // default
      }
      this.port = port;
      this.database = database;
   }

   /**
    * public for test purpose
    * @param dbUriString
    * @return
    */
   public static MongoUri parse(String dbUriString) {
      if (dbUriString == null || dbUriString.trim().length() == 0) { // else empty
         return null;
      }
      
      URI dbUri;
      try {
         dbUri = new URI(dbUriString);
      } catch (URISyntaxException e) {
         throw new RuntimeException("Bad project DB uri " + dbUriString, e);
      }
      if (!dbUri.getScheme().equals("mongodb")) {
         throw new RuntimeException("Only mongodb scheme is supported bu found " + dbUri.getScheme());
      }
      String host = dbUri.getHost(); // "localhost";
      int port = dbUri.getPort(); // 27017;
      String database = dbUri.getPath().replaceAll("^/+", ""); // datacore (path starts with /)
      if (host == null || host.trim().length() == 0
            || database == null || database.trim().length() == 0) {
         return null;
      }

      return new MongoUri(host, port, database);
      // (and not java.net.URI because the path should start by /)
   }

   public String getHost() {
      return host;
   }
   public int getPort() {
      return port;
   }
   public String getDatabase() {
      return database;
   }
   
   public String toString() {
      return "mongodb://" + host + ":" + port + "/" + database;
   }
   
}
