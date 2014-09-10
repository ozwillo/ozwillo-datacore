class datacore::params {

  $java_http="http://download.oracle.com/otn-pub/java/jdk/8u20-b26/jdk-8u20-linux-x64.tar.gz"

  $mvn_http="http://apache.crihan.fr/dist/maven/maven-3/3.0.5/binaries/apache-maven-3.0.5-bin.tar.gz"

  $mvn_conf="export MAVEN_HOME=~/dev/apache-maven-3.0.5 \n export MAVEN_OPTS=\"-XX:MaxPermSize=128m $MAVEN_OPTS\" \n export PATH=$MAVEN_HOME/bin:$PATH"

  # WARNING latest tomcat release is often removed and replaced !! TODO better
  $tomcat_ftp="ftp://ftp.ciril.fr/pub/apache/tomcat/tomcat-7/v7.0.55/bin/apache-tomcat-7.0.55.tar.gz"
  
  $ssh_config_file="puppet:///modules/datacore/config"
  
  #Absolut path to your private key file
  $private_key_path="puppet:///modules/datacore/oasis.private.key"

}
