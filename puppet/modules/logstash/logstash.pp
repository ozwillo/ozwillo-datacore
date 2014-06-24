#Required : puppet module install elasticsearch-logstash

class { 'logstash': 

  install_contrib => true

}
