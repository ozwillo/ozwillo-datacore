$mongo=true

if($mongo) {
  #Required : puppet module install dwerder-mongodb https://forge.puppetlabs.com/dwerder/mongodb
  
  # mongo 2.4.x
  #include mongodb
  
  # mongo 2.6.4
  class { 'mongodb':
    package_name  => 'mongodb-org',
    package_ensure => '2.6.4',
    logdir       => '/var/log/mongodb/'
  }
  
  mongodb::mongod { 'mongod':
      mongod_instance    => 'standalone',
      mongod_rest => 'false', # disable for security
      mongod_auth => 'false' # TODO require auth once users
  }
}

include '::datacore'
