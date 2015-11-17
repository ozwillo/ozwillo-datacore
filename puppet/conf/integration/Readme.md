# Integration

#### Installation

Once [datacore-puppet](https://github.com/ozwillo/ozwillo-datacore/tree/master/puppet/modules/datacore) has been installed:
```
puppet apply install.pp
```

#### Configuration

If you also need to install mongo, install the required module and set ```$mongo=true``` in install.pp.

```
puppet module install dwerder-mongodb 
```
