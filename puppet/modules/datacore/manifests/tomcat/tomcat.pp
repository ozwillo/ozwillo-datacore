class datacore::tomcat::tomcat(

) inherits datacore::params {

    exec {
      "Get Tomcat":
        command => "/usr/bin/wget ${tomcat_ftp} -O /home/oasis/upload/apache-tomcat-7.tar.gz",
        timeout => 0,
        unless  => "/bin/ls /home/oasis/upload | /bin/grep -c apache-tomcat-7";
      "Untar Tomcat":
        command => "/bin/tar xfz /home/oasis/upload/apache-tomcat-7.tar.gz -C /tmp",
        timeout => 0,
        require => Exec['Get Tomcat'],
        unless  => "/bin/ls /tmp | /bin/grep -c apache-tomcat-7";
      "Move Tomcat":
        command => "/bin/mv -f /tmp/apache-tomcat-7* /home/oasis/install/tomcat7-dc",
        timeout => 0,
        require => Exec['Untar Tomcat'],
        unless  => "/bin/ls /home/oasis/install/tomcat7-dc/ | /bin/grep -c bin";
    }

}
