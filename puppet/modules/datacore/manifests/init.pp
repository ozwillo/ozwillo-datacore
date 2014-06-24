class datacore (

) inherits datacore::params {
  
  class { '::datacore::env::env': } ->
  class { '::datacore::java::java': } ->
  class { '::datacore::source::source': } ->
  class { '::datacore::maven::maven': } ->
  class { '::datacore::tomcat::tomcat': } ->
  class { '::datacore::core': } ->
  class { '::datacore::apache::apache': } ->
  class { '::datacore::service::service': }

}
