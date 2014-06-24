class datacore::core (

) inherits datacore::params {
  
    exec {
      "mvn installation":
        command => "/home/oasis/dev/apache-maven-3.0.5/bin/mvn -f /home/oasis/dev/workspace/oasis-datacore/pom.xml clean install -DskipTests",
        timeout => 0;
      #Beware, not saving from now
      "Removing previous files":
        command => "/bin/rm -rf /home/oasis/install/tomcat7-dc/webapps/ROOT/*";
      "Copying datacore to Tomcat":
        command => "/bin/cp -rf /home/oasis/dev/workspace/oasis-datacore/oasis-datacore-web/target/datacore/* /home/oasis/install/tomcat7-dc/webapps/ROOT/",
        require => Exec['mvn installation', 'Removing previous files'];
      "Install conf files":
        command => "/bin/cp -rf /home/oasis/dev/workspace/oasis-datacore/oasis-datacore-deploy/vmdc/* /",
        require => Exec['mvn installation', 'Copying datacore to Tomcat'];
      "Change owner":
        command => "/bin/chown -R oasis ~oasis";
      "Change group":
        command => "/bin/chgrp -R oasis ~oasis";
      #DAAS : Datacore As A (Linux) Service
      "DAAS":
        command => "/bin/chmod +x /etc/init.d/*";
    }

}
