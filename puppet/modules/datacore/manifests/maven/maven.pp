class datacore::maven::maven(

) inherits datacore::params {

    exec {
      "Get Maven":
        command => "/usr/bin/wget ${mvn_http} -P /home/oasis/upload",
        timeout => 0,
        unless  => "/bin/ls /home/oasis/upload | /bin/grep -c apache-maven-3";
      "Untar Maven":
        command => "/bin/tar xfz /home/oasis/upload/apache-maven-3*.tar.gz -C /tmp",
        timeout => 0,
        require => Exec['Get Maven'],
        unless  => "/bin/ls /tmp | /bin/grep -c apache-maven-3";
      "Move Maven":
        command => "/bin/mv -f /tmp/apache-maven-3* /home/oasis/dev/apache-maven-3",
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
