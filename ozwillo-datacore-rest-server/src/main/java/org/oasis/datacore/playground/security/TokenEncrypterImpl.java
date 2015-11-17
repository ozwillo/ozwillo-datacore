package org.oasis.datacore.playground.security;

import org.springframework.stereotype.Component;

@Component
public class TokenEncrypterImpl implements TokenEncrypter {

   @Override
   public String encrypt(String token) {
      // TODO
      return token;
   }

   @Override
   public String decrypt(String encryptedToken) {
      if (encryptedToken.startsWith("encr:")) {
         // TODO LATER
         //encryptedToken = decrypt(encryptedToken.substring("encr:".length()));
      }
      return encryptedToken;
   }

}
