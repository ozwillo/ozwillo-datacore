
class riemann (
  $version         = $jmeter::params::version,
) inherits riemann::params {

  $riemann_deb_url="http://aphyr.com/riemann/riemann_${version}_all.deb"

  exec {
    "Get Riemann":
      command => "/usr/bin/wget --no-check-certificate --no-cookies ${riemann_deb_url} -O /tmp/riemann.deb",
      timeout => 0;
    "Install Riemann":
      command => "/usr/bin/dpkg -i /tmp/riemann.deb",
      require => Exec['Get Riemann'],
      timeout => 0;
  }

  service {
    'riemann':
      require => Exec['Install Riemann'],
      ensure => running,
      enable => true;
  }
  
}
