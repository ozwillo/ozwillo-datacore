class datacore::maven::maven(

) inherits datacore::params {

    exec {
      "Get Maven":
        command => "/usr/bin/wget http://apache.crihan.fr/dist/maven/maven-3/3.0.5/binaries/apache-maven-3.0.5-bin.tar.gz -O /home/oasis/upload/apache-maven-3.0.5-bin.tar.gz",
        timeout => 0;
      "Untar Maven":
        command => "/bin/tar xfz /home/oasis/upload/apache-maven-3.0.5-bin.tar.gz -C /home/oasis/dev",
        timeout => 0,
        require => Exec['Get Maven'];
    }
    
    file {'/home/oasis/.bashrc':
      ensure => file,
      content => $mvn_conf,
      require => User['oasis'];
    }
  
}
