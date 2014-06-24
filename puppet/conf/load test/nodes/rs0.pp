#RS0
#Don't forget to set members variable
#in the node responsible for configuration

node 'mongo01',
     'mongo03' {
  $rs = "rs0"

  include mongodb
  mongodb::mongod { 'mongod_config':
    mongod_instance  => 'configsvr',
    mongod_port      => $config_port,
    mongod_replSet   => '',
    mongod_configsvr => 'true'
  }

  mongodb::mongod { "mongod_${rs}":
    mongod_instance => "${rs}",
    mongod_port     => $rs_port,
    mongod_replSet  => "${rs}",
    #mongod_shardsvr => 'true'
  }

}

node 'mongo02' {
  $rs = "rs0"
  $members = ['10.28.9.81', '10.28.9.82', '10.28.9.83']


  include mongodb
  mongodb::mongod { "mongod_${rs}":
    mongod_instance => "${rs}",
    mongod_port     => $rs_port,
    mongod_replSet  => "${rs}",
    mongod_shardsvr => 'true',
    before => Exec['Init rs'];
  } 

  exec {
    'Init rs':
      command => "/usr/bin/mongo localhost:${rs_port} --eval 'rs.initiate({_id : \"${rs}\", members: [ {_id: 0, host:\"${members[0]}:${rs_port}\"}, {_id:1, host:\"${members[1]}:${rs_port}\"}, {_id: 2, host: \"${members[2]}:${rs_port}\"}] });'";   
     
    #Register the replica set as a shard in the cluster
    'Add shard':
      command => "/usr/bin/mongo ${mongos} --eval 'sh.addShard(\"${rs}/${members[0]}:${rs_port},${members[1]}:${rs_port},${members[2]}:${rs_port}\");'",
      require => Exec['Init rs'];
  }
} 
