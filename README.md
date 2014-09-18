Java Fundamentals - Chat client
===========

This is a chat client, useful for "Java Networking" homework on "Java Fundamentals" course. 

If you do not feel like building it from the source, you can also **download** it from
[here](https://dl.dropboxusercontent.com/u/44963211/JavaFundamentals/ChatClientFx.jar).

Requirements
---------------
* **Java 8** (the GUI is implemented with JavaFX)
* **Maven 3** (or newer) -- needed only for building, required by [JavaFX Maven plugin](http://zenjava.com/javax/maven/)).

Building
---------------
To build it, execute:

```shell
mvn clean package
```

It produces a single executable JAR file (target/ChatClientFx.jar).

Running
---------------

To run it, use the "java -jar" command, like this:

```shell
java -jar target/ChatClientFx.jar
```

Usage instructions:

```
Usage: java -jar ChatClientFx.jar --name=yourName --host=hostname [--serverPort=number] [--httpPort=number]
```
