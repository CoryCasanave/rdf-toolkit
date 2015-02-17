# Developing the rdf-serializer

## build

This project is being build and packaged on the EDM Council Jenkins Server by [this job](https://jenkins.edmcouncil.org/job/rdf-serializer-build/).

The current Jenkins build status is: 
[![Build Status](https://jenkins.edmcouncil.org/buildStatus/icon?job=rdf-serializer-build)](https://jenkins.edmcouncil.org/job/rdf-serializer-build/)

## test

You can run the rdf-serializer without first packaging it as a jar (see "package" below) by launching it via sbt:

```
sbt "run --help"
```

All the unit tests can be executed by this command:

```
sbt test
```

## package

### normal packaging

Normal packaging as a jar is done with the following command:

```
sbt package
```

This creates a jar file like:

```
./target/scala-2.11/rdf-serializer_2.11-<version>.jar
```

### packaging as "uber jar"

The RDF Serializer is packaged as one "fat jar" or "uber jar" which can be downloaded from the Jenkins server:

- https://jenkins.edmcouncil.org/job/rdf-serializer-build/lastSuccessfulBuild/artifact/target/scala-2.11/rdf-serializer.jar

You can create this uber jar from the command line yourself as well:

```
sbt assembly
```
