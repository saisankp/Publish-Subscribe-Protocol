import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import tcdIO.Terminal;

//Author: Prathamesh Sai
//Student Number: 19314123
public class Publisher extends Node {

	InetSocketAddress dstAddress;
	Terminal terminal;

	Publisher(Terminal terminal, String dstHost, int dstPort, int srcPort) {
		try {
			this.terminal = terminal;
			dstAddress = new InetSocketAddress(dstHost, dstPort);
			socket = new DatagramSocket(srcPort);
			listener.go();
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {
			Terminal terminal = new Terminal("Publisher-1");
			(new Publisher(terminal, DEFAULT_DST, BROKER_PORT, PUBLISHER_1_PORT)).start();
			terminal.println("Program completed");
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	/* 
	 * This function implements what the publisher will do when it gets packets.
	 */
	public synchronized void onReceipt(DatagramPacket packet) {
		this.notify();
		byte[] incomingData = packet.getData();
		if (ProtocolUtility.getType(incomingData) == MESSAGE) {
			terminal.println("I just got a message: " + ProtocolUtility.getMessage(incomingData));
			sendAck(packet, terminal);
		}
		else if (ProtocolUtility.getType(incomingData) == ACK) {
			terminal.println("I just got an ACK for Sequence Number " + ProtocolUtility.getSequenceNumber(incomingData));
		} 
	}

	/* 
	 * This function starts the publisher's functionality - It can publish a message to connected subscribers.
	 */
	public synchronized void start() throws Exception {
		while (true) {
			String startingString = terminal.readString("Enter START to create/join a new or existing topic or PUBLISH to publish a new message: ");
			if (startingString.contains("PUBLISH")) {
				if (publishMessage()) {
					this.wait(); // wait for the ack first
					this.wait(); // then wait for the message
				}
			}
			else if (startingString.contains("START")) {
				createTopic();
				this.wait(); // wait for the ack first
				this.wait(); // then wait for the message
			}  else {
				terminal.println("You just entered invalid input!");
			}
		}
	}


	/* 
	 * This function publishes a message by taking user input and communicating with the broker to publish a message 
	 * to all the current subscribers of the current topic.
	 */
	private boolean publishMessage() {
		String topic = terminal.readString("What's the name of the topic you want to publish a message for?: ");
		String message = terminal.readString("What's the message that you would like to publish?: ");
		int topicNumber = Integer.MAX_VALUE;
		for (int i = 0; i < ProtocolUtility.topicNumbersToNamesMap.size(); i++) {
			if ((ProtocolUtility.topicNumbersToNamesMap.get(i)).equals(topic)) {
				topicNumber = i;
			}
		}
		if (topicNumber == Integer.MAX_VALUE) {
			terminal.println("I dont think this topic exists!");
		} 
		else {
			DatagramPacket[] publicationPackets = createPackets(PUBLISH, topicNumber, message, dstAddress);
			try {
				terminal.println("Sending packet now!");
				socket.send(publicationPackets[0]);
			} catch (IOException e) {
				e.printStackTrace();
			}
			terminal.println("Packet has been sent!");
			return true;
		}
		return false;
	}

	/* 
	 * This function creates a topic by using user input for the topic name and sending a packet to the
	 * broker to create the appropriate packet.
	 */
	private void createTopic() {
		String topic = terminal.readString("Enter a topic to create/join: ");
		terminal.println("Sending packet now!");

		DatagramPacket[] creationPackets = createPackets(CREATE_TOPIC, ProtocolUtility.topicNumbersToNamesMap.size(), topic, dstAddress);
		ProtocolUtility.topicNumbersToNamesMap.put(ProtocolUtility.topicNumbersToNamesMap.size(), topic);
		try {
			socket.send(creationPackets[0]);
		} catch (IOException e) {
			e.printStackTrace();
		}
		terminal.println("Packet has been sent!");
	}


}