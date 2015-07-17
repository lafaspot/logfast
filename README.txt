# logfast
Logger that scales better for multi cpus machines and large number of threads

# to build before you submit a PR
mvn clean install

# for contibutors run deploy to do a push to nexus servers
mvn clean deploy -Dgpg.passphrase=[pathPhrase]
