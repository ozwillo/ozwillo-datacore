
class mmsagent (
  $archiveUrl = $mmsagent::params::archiveUrl,
  $apiKey = $mmsagent::params::apiKey,
) inherits mmsagent::params {

  exec {
    "Get agent":
      command => "/usr/bin/wget --no-check-certificate --no-cookies ${archiveUrl} -O /tmp/mongoAgent.tar.gz",
      timeout => 0;
    "Untar agent":
      command => "/bin/tar xfz /tmp/mongoAgent.tar.gz -C /tmp",
      require => Exec['Get agent'],
      timeout => 0;
    "Move agent":
      command => "/bin/mv -f /tmp/mongodb-mms-monitoring-agent-* /opt/mongodb-mms-agent",
      require => Exec['Untar agent'],
      timeout => 0;
    "Add API Key":
      command => "/bin/sed -i.bak s/mmsApiKey=/mmsApiKey=${apiKey}/g /opt/mongodb-mms-agent/monitoring-agent.config",
      require => Exec['Move agent'],
      timeout => 0;
  }

  file {'/etc/init.d/mongodb-mms-monitoring-agent':
    ensure => file,
    owner => "root",
    group => "root",
    mode => 555,
    source => "puppet:///modules/mmsagent/mmsagent-init",
    before => Service['mongodb-mms-monitoring-agent'];
  }

  service {
    'mongodb-mms-monitoring-agent':
      require => Exec['Add API Key'],
      ensure => running,
      enable => true;
  }
  
}
