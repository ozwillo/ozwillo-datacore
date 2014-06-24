class datacore::env::env(

) inherits datacore::params {

  package {
    "git-core":                 ensure => "present"; # Git!
    "apache2":			            ensure => "present";
    "ssh":			                ensure => "present";
    "sudo":			                ensure => "present"; #Required for tomcat7-dc init.d
    "wget":                     ensure => "present";
  }

  group { "puppet":
    ensure => "present",
  }

  user { "oasis":
    ensure => "present",
    managehome => true,
  }

}
