class datacore::source::source(

) inherits datacore::params {

    #TODO :  CHANGE according to the user executing the script
    exec { 
      "Make .ssh":
        command => "/bin/mkdir -p /root/.ssh";
    }
    

    file {'/root/.ssh/oasis':
      ensure => file,
      owner => "root",
      group => "root",
      mode  => 400,
      #TODO!! : CHANGE!
      source => $private_key_path,
      require => Exec['Make .ssh'];
    }
    
    file {'/root/.ssh/config':
      ensure => file,
      owner => "root",
      group => "root",
      #TODO!! : CHANGE!
      source => $ssh_config_file,
      require => Exec['Make .ssh'];
    }
    
    exec {
      "Create dt dir":
        command => "/bin/mkdir -p /home/oasis/upload /home/oasis/save /home/oasis/bin /home/oasis/install /home/oasis/dev/workspace",
        require => File['/root/.ssh/oasis'];
      "Add Github to known_hosts":
        command => "/usr/bin/ssh-keyscan -H github.com >> /root/.ssh/known_hosts",
        require => File['/root/.ssh/oasis', '/root/.ssh/config'];
      "Clone repo":
        command => "/usr/bin/git clone git@github.com:pole-numerique/oasis-datacore.git /home/oasis/dev/workspace/oasis-datacore/",
        timeout => 0,
        require => Exec['Create dt dir', 'Add Github to known_hosts'],
        unless  => "/bin/ls /home/oasis/dev/workspace/oasis-datacore/ | /bin/grep -c pom";
    }

}
