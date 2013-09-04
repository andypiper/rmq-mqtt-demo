## Build

Grab required client libraries:

Paho latest snapshot (as of writing):

https://repo.eclipse.org/content/repositories/paho-snapshots/org/eclipse/paho/mqtt-client/0.4.1-SNAPSHOT/mqtt-client-0.4.1-20130821.133635-1.jar

RabbitMQ Java client:

http://www.rabbitmq.com/releases/rabbitmq-java-client/v3.1.5/rabbitmq-java-client-bin-3.1.5.zip

Now, compile the code:

    javac -classpath ./mqtt-client-0.4.1-20130821.133635-1.jar:./rabbitmq-java-client-bin-3.1.5/rabbitmq-client.jar MqttTest.java

## Setup

Install RabbitMQ. If running on a Mac, installation via homebrew (http://brew.sh) will automatically enable the MQTT plugin. Otherwise, you will need to enable the MQTT plugin using `sudo rabbitmq-plugins enable rabbitmq_mqtt`

## Run

    java -classpath ./mqtt-client-0.4.1-20130821.133635-1.jar:./rabbitmq-java-client-bin-3.1.5/rabbitmq-client.jar:. MqttTest

Expected output:

    this payload was published on MQTT and read using AMQP


## TODO - make it all more interesting!

This is just a single, simple piece of code which connects to the same RabbitMQ broker over two protocols (MQTT and AMQP) and passes a message in, then gets it back.

What would be more interesting would be two programs:

- connect to RabbitMQ on MQTT and publish a series of messages, possibly on different topics
- connect to RabbitMQ on AMQP and receive the published messages, displaying them in some interesting way... ideally using SpringAMQP rather than the simple RabbitMQ Java client.