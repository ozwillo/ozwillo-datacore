class datacore::apache::apache(

) inherits datacore::params {
  
  exec {
    "Reload Apache":
      command => "/etc/init.d/apache2 reload";
  }

}
