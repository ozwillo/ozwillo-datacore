class datacore::maven::maven(

) inherits datacore::params {

    exec {
      "Get Maven":
        command => "/usr/bin/wget ${mvn_http} -O /home/oasis/upload",
        timeout => 0;
      "Untar Maven":
        command => "/bin/tar xfz /home/oasis/upload/apache-maven-3*.tar.gz -C /tmp",
        timeout => 0,
        require => Exec['Get Maven'];
      "Move Maven":
        command => "/bin/mv -f /home/oasis/upload/apache-maven-3*.tar.gz /home/oasis/dev/apache-maven-3",
        timeout => 0,
        require => Exec['Untar Maven'],
        unless  => "/bin/ls /home/oasis/dev/apache-maven-3/ | /bin/grep -c bin";
    }
    
    file {'/home/oasis/.bashrc':
      ensure => file,
      content => $mvn_conf,
      require => User['oasis'];
    }
  
}
