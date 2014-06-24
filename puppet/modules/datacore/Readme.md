# puppet-oasis-datacore

Puppet module to install a datacore.

### Configuration

To be able to build the datacore from source, you should provide a valid private key to be used to clone the repository. Place the key in ```files/oasis.private.key```.

### Build the module

In the directory of the project :
```puppet module build .```

It should create ```pkg/oasis-datacore-0.0.1.tar.gz```.

### Install the module

```puppet module install pkg/oasis-datacore-0.0.1.tar.gz --force```

### Install datacore

```puppet apply install.pp```
