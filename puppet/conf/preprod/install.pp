$mongo=true

if($mongo) {
  #Required : puppet module install dwerder-mongodb https://forge.puppetlabs.com/dwerder/mongodb
  
  # mongo 2.4.x
  #include mongodb
  
  # mongo 2.6.4
  class { 'mongodb':
    package_name  => 'mongodb-org',
    package_ensure => '2.6.4',
    old_servicename => 'mongod', # renamed (from mongodb) in 2.6.x on debian https://github.com/echocat/puppet-mongodb/issues/45
    logdir       => '/var/log/mongodb/'
  }
  
  mongodb::mongod { 'mongod':
      mongod_instance    => 'standalone',
      mongod_rest => 'false', # disable for security
      mongod_auth => 'false' # TODO require auth once users
  }
}

include '::datacore'
