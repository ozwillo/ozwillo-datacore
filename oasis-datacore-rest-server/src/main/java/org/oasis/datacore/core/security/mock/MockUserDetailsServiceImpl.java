package org.oasis.datacore.core.security.mock;

import java.util.Collection;
import java.util.HashSet;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


/**
 * NOT USED YET
 * Doc about how impl :
 * http://howtodoinjava.com/2013/04/16/custom-userdetailsservice-example-for-spring-3-security/
 * 
 * @author mdutoo
 *
 */
//@Component
public class MockUserDetailsServiceImpl implements UserDetailsService {

   @Override
   public UserDetails loadUserByUsername(String username)
         throws UsernameNotFoundException {
      // TODO switch per username
      Collection<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
      authorities.add(new SimpleGrantedAuthority("group"));
      //authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
      UserDetails user = new User(username, "password", true, true, true, true, authorities);
      return user;
   }

}
