class datacore::service::service(

) inherits datacore::params {
  service {
    'apache2':
      ensure => running,
      enable => true;
    'tomcat7-dc':
      ensure => running,
      enable => true,
  }
}
