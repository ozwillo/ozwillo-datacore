package org.oasis.datacore.core.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


/**
 * NOT YET USED
 * 
 * TODO implement it on top of Kernel autn / id / autz
 * 
 * Doc about how impl :
 * http://howtodoinjava.com/2013/04/16/custom-userdetailsservice-example-for-spring-3-security/
 * 
 * @author mdutoo
 *
 */
//@Component // always instanciated but non-test code can't call it
public class KernelUserDetailsServiceImpl implements UserDetailsService {

   @Override
   public UserDetails loadUserByUsername(String username)
         throws UsernameNotFoundException {
      throw new UnsupportedOperationException("Not implemented yet");
   }

}
