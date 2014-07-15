#Puppet Datacore Components 

#### Set up

Install Puppet on every server and Puppet Master on one of them.
```
apt-get install -y puppet
OR
apt-get install -y puppetmaster
```

Get this directory on Puppet Master

#### Modules installation (Master)

Modules from [Puppet Forge](https://forge.puppetlabs.com/):
* [MongoDB](https://forge.puppetlabs.com/dwerder/mongodb)
* [Graphite](https://forge.puppetlabs.com/dwerder/graphite)
* [Elasticsearch](https://forge.puppetlabs.com/elasticsearch/elasticsearch)
* [Collectd](https://forge.puppetlabs.com/pdxcat/collectd)

```
puppet module install dwerder-mongodb
puppet module install dwerder-graphite
puppet module install elasticsearch-elasticsearch
puppet module install pdxcat-collectd
```

[Custom modules](https://github.com/pole-numerique/oasis-datacore/tree/master/puppet/modules) (Must be manually built):
* Datacore
* JMeter
* Riemann
* MMSAgent

```
puppet module install oasis-datacore-0.0.3.tar.gz --force
puppet module install jmeter-0.0.1.tar.gz --force
puppet module install riemann-0.0.1.tar.gz --force
puppet module install mmsagent-0.0.1.tar.gz --force
```

## Puppet Configuration

##### Agents
Dans /etc/puppet/puppet.conf :
```
[main]
pluginsync=true

[agent]
#IP Master
server=10.28.9.56
certname=agent #Replace it with the name of your node.
```

Dans /etc/default/puppet :
```
START=yes
```

##### Master
Dans /etc/puppet/puppet.conf :
```
[main]
pluginsync=true

[master]
dns_alt_names=puppet, master, 10.28.9.56 
certname=master

#Optionnal
reports=http
reporturl=http://status.ozwillo.org:3000/report
```

##### Association

Once you have a master and a slave running.

Connect to master with ssh and list certificates:
```
puppet cert --list
```

You should see your agent, so sign its certificate.
```
puppet cert --sign agent
```

Your agent should now be able to get its catalog from the master and apply the configuration.

##### Debug
Debugging puppet agent :
```
puppet agent --verbose --no-daemonize
```
## Configuration

#### Datacore

Modify [/home/oasis/install/tomcat7-dc/webapps/ROOT/WEB-INF/classes/oasis-datacore-deploy.properties](https://github.com/pole-numerique/oasis-datacore/blob/master/oasis-datacore-deploy/vmdc/home/oasis/install/tomcat7-dc/webapps/ROOT/WEB-INF/classes/oasis-datacore-deploy.properties) to fit your needs, especially *datacoreApiServer.baseUrl* and maybe also *kernel.baseUrl*

#### Graphite

In /etc/apache2/sites-available/graphite.conf change @DJANGO_ROOT@ with the path to your django install which you can get with:
```
python -c "
import sys
sys.path = sys.path[1:]
import django
print(django.__path__)"
```
Result: ```['/usr/lib/python2.7/dist-packages/django']```

In /opt/graphite/webapp/graphite/local_settings.py change the following:
* TIME_ZONE = 'Europe/Paris'

In /opt/graphite/conf/storage-schemas.conf:
* Change retentions according to your setting.

In /opt/graphite/conf/storage-aggregation.conf
* Change xFilesFactor to fit your needs

#### JMeter

Client in non-GUI mode, connecting to a jmeter server:
```
./jmeter -n -t ~/fullInsert.jmx -R 10.28.9.56 -l ~/res.jtl
```

Read logs server:
```
tail -f /usr/share/jmeter/bin/jmeter-server.log
```

https://jmeter.apache.org/usermanual/remote-test.html


## Example

#### Catalog

Configurations are available in the directory [conf](https://github.com/pole-numerique/oasis-datacore/tree/master/puppet/conf).

#### General Example

Add the configuration of the different nodes in /etc/puppet/manifests/site.pp on the master:
```
include mongodb

node  'certname of the concerned VM' {

  mongodb::mongod {
    'mongo':
      mongod_instance    => 'mongo'
  }
  
  include '::datacore'
}

node basenode {

}
```

