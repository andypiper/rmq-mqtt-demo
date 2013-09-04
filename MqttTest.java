// Heavily based on RabbitMQ MQTT adapter test case code!

// first, import the RabbitMQ Java client
// and the Paho MQTT client classes, plus any other
// requirements

import com.rabbitmq.client.*;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.NetworkModule;
import org.eclipse.paho.client.mqttv3.internal.TCPNetworkModule;
// import org.eclipse.paho.client.mqttv3.internal.trace.Trace;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttOutputStream;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/***
 *  MQTT v3.1 tests
 *  TODO: synchronise access to variables
 */

public class MqttTest implements MqttCallback {

    // setup some variables which define where the MQTT broker is
    private final String host = "localhost";
    private final int port = 1883;
    private final String brokerUrl = "tcp://" + host + ":" + port;
    private String clientId;
    private String clientId2;
    private MqttClient client;
    private MqttClient client2;
    private MqttConnectOptions conOpt;
    private ArrayList<MqttMessage> receivedMessages;

    // specify a message payload - doesn't matter what this says, but since MQTT expects a byte array
    // we convert it from string to byte array here
    private final byte[] payload = "this payload was published on MQTT and read using AMQP".getBytes();

    // specify the topic to be used
    private final String topic = "test-topic";

    private int testDelay = 2000;
    private long lastReceipt;
    private boolean expectConnectionFailure;

    private ConnectionFactory connectionFactory;
    private Connection conn;
    private Channel ch;

    // override 10s limit
    private class MyConnOpts extends MqttConnectOptions {
        private int keepAliveInterval = 60;
        @Override
        public void setKeepAliveInterval(int keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
        }
        @Override
        public int getKeepAliveInterval() {
            return keepAliveInterval;
        }
    }

    public void setUpMqtt() throws MqttException {
        clientId = getClass().getSimpleName() + ((int) (10000*Math.random()));
        clientId2 = clientId + "-2";
        client = new MqttClient(brokerUrl, clientId);
        client2 = new MqttClient(brokerUrl, clientId2);
        conOpt = new MyConnOpts();
        setConOpts(conOpt);
        receivedMessages = new ArrayList<MqttMessage>();
        expectConnectionFailure = false;
    }

    public  void tearDownMqtt() throws MqttException {
        // clean any sticky sessions
        setConOpts(conOpt);
        client = new MqttClient(brokerUrl, clientId);
        try {
            client.connect(conOpt);
            client.disconnect();
        } catch (Exception _) {}

        client2 = new MqttClient(brokerUrl, clientId2);
        try {
            client2.connect(conOpt);
            client2.disconnect();
        } catch (Exception _) {}
    }

    private void setUpAmqp() throws IOException {
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        conn = connectionFactory.newConnection();
        ch = conn.createChannel();
    }

    private void tearDownAmqp() throws IOException {
        conn.close();
    }

    private void setConOpts(MqttConnectOptions conOpts) {
        // provide authentication if the broker needs it
        // conOpts.setUserName("guest");
        // conOpts.setPassword("guest".toCharArray());
        conOpts.setCleanSession(true);
        conOpts.setKeepAliveInterval(60);
    }

    private void publish(MqttClient client, String topicName, int qos, byte[] payload) throws MqttException {
        MqttTopic topic = client.getTopic(topicName);
        MqttMessage message = new MqttMessage(payload);
        message.setQos(qos);
        MqttDeliveryToken token = topic.publish(message);
        token.waitForCompletion();
    }

    public void connectionLost(Throwable cause) {
        if (!expectConnectionFailure)
            System.out.println("Connection unexpectedly lost");
    }

    public void messageArrived(String topic, MqttMessage message) throws Exception {
        lastReceipt = System.currentTimeMillis();
        receivedMessages.add(message);
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    public void run() {
        try {

        setUpMqtt(); // initialise the MQTT connection
        setUpAmqp(); // initialise the AMQP connection

        String queue = ch.queueDeclare().getQueue();
        ch.queueBind(queue, "amq.topic", topic);

        client.connect(conOpt);
        publish(client, topic, 1, payload); // publish the MQTT message
        client.disconnect();
        Thread.sleep(testDelay);

        GetResponse response = ch.basicGet(queue, true); // get the AMQ response
        System.out.println(new String(response.getBody()));

        tearDownAmqp(); // cleanup AMQP resources
        tearDownMqtt(); // cleanup MQTT resources

        } catch (Exception mqe) {
            mqe.printStackTrace();
        }
    }

    public static void main(String[] args) {
        MqttTest mqt = new MqttTest();
        mqt.run();
    }


}
