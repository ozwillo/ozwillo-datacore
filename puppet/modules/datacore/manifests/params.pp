class datacore::params {

  $mvn_conf="export MAVEN_HOME=~/dev/apache-maven-3.0.5 \n export MAVEN_OPTS=\"-XX:MaxPermSize=128m $MAVEN_OPTS\" \n export PATH=$MAVEN_HOME/bin:$PATH"

  $tomcat_ftp="ftp://ftp.ciril.fr/pub/apache/tomcat/tomcat-7/v7.0.54/bin/apache-tomcat-7.0.54.tar.gz"
  
  $ssh_config_file="puppet:///modules/datacore/config"
  
  #Absolut path to your private key file
  $private_key_path="puppet:///modules/datacore/oasis.private.key"

}
