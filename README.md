Java Fundamentals - Chat client
===========

This is a fully functional chat client, needed for "Java Networking" homework on "Java Fundamentals" course. 

Requirements
---------------
* <strong>Java 8</strong> (the GUI is implemented with JavaFX)
* <strong>Maven 3</strong> (or newer), required by [JavaFX Maven plugin](http://zenjava.com/javax/maven/)).

Getting Started
---------------
To build it, execute:

```shell
mvn clean package
```

It produces a single executable JAR file (target/ChatClientFx.jar) that can be executed via

```shell
java -jar target/ChatClientFx.jar
```

It prints out usage instructions:

<pre>
Usage: java -jar ChatClientFx.jar --name=yourName --host=hostname [--serverPort=number] [--httpPort=number]
</pre>
