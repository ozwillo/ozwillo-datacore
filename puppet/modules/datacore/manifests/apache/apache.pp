class datacore::apache::apache(

) inherits datacore::params {

  file {'/etc/apache2/sites-available/default':
    ensure => file,
    #TODO!! : CHANGE!
    source => 'puppet:///modules/datacore/oasis-datacore',
  }
  
  exec {
    "Reload Apache":
      command => "/etc/init.d/apache2 reload",
      require => File['/etc/apache2/sites-available/default'];
  }

}
