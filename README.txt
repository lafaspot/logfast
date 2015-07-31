Logger that is designed to scale better for multiple cpus machines and reduce contention when using large number of threads.

- To build before you submit a PR
$ mvn clean install

- For contibutors run deploy to do a push to nexus servers
$ mvn clean deploy -Dgpg.passphrase=[pathPhrase]

- All Pull requests need to pass continous integration before being merged.
  Please goto https://travis-ci.org/lafaspot/logfast
  
