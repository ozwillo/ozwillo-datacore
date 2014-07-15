$mongo=true

if($mongo) {
  #Required : puppet module install dwerder-mongodb
  include mongodb
  mongodb::mongod {
    'mongod':
      mongod_instance    => 'mongod'
  }
}

include '::datacore'
