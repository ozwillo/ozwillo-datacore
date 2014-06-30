Puppet Mongo Agent
==============

Use it:
```
class { '::mmsagent':
  archiveUrl => 'https://mms.mongodb.com/download/agent/monitoring/mongodb-mms-monitoring-agent-2.2.0.70-1.linux_x86_64.tar.gz',
  apiKey => 'An API Key',
}
```

[MongoDB Agent Installation Documentation](http://mms.mongodb.com/help/tutorial/install-monitoring-agent/)
