#Required :
#Fork -> Build it from https://github.com/vvision/puppet-jmeter
#Default -> puppet module install dduvnjak-jmeter
#/!\ Automatically install openjdk-6-jre-headless

class { 'jmeter::server':
  version => '2.11',
  plugins_install => True,
  server_ip => '127.0.0.1',
}
