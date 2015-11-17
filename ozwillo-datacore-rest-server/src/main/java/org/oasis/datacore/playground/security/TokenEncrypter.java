package org.oasis.datacore.playground.security;

public interface TokenEncrypter {

   String encrypt(String token);
   String decrypt(String encryptedToken);

}
