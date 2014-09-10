class datacore::maven::maven(

) inherits datacore::params {

    exec {
      "Get Maven":
        command => "/usr/bin/wget ${mvn_http} -O ${$mvn_file}",
        timeout => 0;
      "Untar Maven":
        command => "/bin/tar xfz ${$mvn_file} -C /home/oasis/dev",
        timeout => 0,
        require => Exec['Get Maven'];
    }
    
    file {'/home/oasis/.bashrc':
      ensure => file,
      content => $mvn_conf,
      require => User['oasis'];
    }
  
}
