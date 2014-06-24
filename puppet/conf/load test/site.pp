$config_port = 24000
$rs_port = 24400
$mongos_port = 27017

$config0="10.28.9.81"
$config1="10.28.9.83"
$config2="10.28.9.84"

#REPLICA SET
$mongos = "10.28.9.51:27017"
$rs_ids = ["rs0", "rs1"]


node  'monitoring' {
  
  class { 'graphite':
    gr_web_cors_allow_from_all => true,
    secret_key                 => 'CHANGE_IT!'
  }  

  $ipadress='127.0.0.1'

class { 'elasticsearch':
  package_url => 'https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-1.1.1.deb',
 config                   => {
   'node'                 => {
     'name'               => 'elasticsearch-dc-1'
   },
   'index'                => {
     'number_of_replicas' => '0',
     'number_of_shards'   => '1'
   },
   'network'              => {
     'host'               => $::ipaddress
   }
 }
}

class { 'jmeter':
  jmeter_version => '2.11',
  jmeter_plugins_install => True,
  jmeter_plugins_version => '1.1.3'
}

}

node 'java01',
     'java02',
     'java03',
     'java04' {
  include mongodb
  mongodb::mongos { 'mongos':
    mongos_instance      => 'mongos',
    mongos_port          => $mongos_port,
    #WARNING! : Use CNAMES for config servers
    mongos_configServers => "${config0}:${config_port},${config1}:${config_port},${config2}:${config_port}"
  }

  include '::datacore'

  class { '::riemann':
    version => '0.2.5',
  }
 
  class { '::collectd':
    purge        => true,
    recurse      => true,
    purge_config => true,
  }

  class { 'collectd::plugin::df':
    mountpoints    => ['/u'],
    fstypes        => ['nfs','tmpfs','autofs','gpfs','proc','devpts'],
    ignoreselected => true,
  }
  class { 'collectd::plugin::disk':
    disks          => ['/^dm/'],
    ignoreselected => true
  }
  class { 'collectd::plugin::cpu':
  }
  class{ 'collectd::plugin::entropy':
  }
  class { 'collectd::plugin::interface':
    interfaces     => ['eth0'],
    ignoreselected => true
  }
  class { 'collectd::plugin::load':
  }
  #class { 'collectd::plugin::memory':
  #}
  #class { 'collectd::plugin::network':
  #}  
  class { 'collectd::plugin::swap':
    reportbydevice => false,
    reportbytes    => true
  }  
  class { 'collectd::plugin::write_graphite':
    graphitehost => '10.28.9.55',
  }
}

node basenode {

}

import "nodes/*.pp"
