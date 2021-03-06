# puppet-oasis-datacore

Puppet module to install a Datacore. Also installs : Java JDK 7, Apache, maven, git, sudo, ssh, wget for setup.

### Configuration

To be able to build the datacore from source, you should provide a valid private key to be used to clone the repository. Place the key in ```files/oasis.private.key```.

### Build the module

In the directory of the project :
```puppet module build .```

It should create ```pkg/oasis-datacore-0.0.3.tar.gz```.

### Install the module

```puppet module install pkg/oasis-datacore-0.0.3.tar.gz --force```

### Install datacore

```puppet apply install.pp```

### Dependency

Module from [Puppet Forge](https://forge.puppetlabs.com/):
* [MongoDB](https://forge.puppetlabs.com/dwerder/mongodb)

This module is automatically installed.

In case it did not work, you can still install it manually.
```
puppet module install dwerder-mongodb
```
