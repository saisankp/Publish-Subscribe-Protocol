import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import tcdIO.Terminal;

//Author: Prathamesh Sai
//Student Number: 19314123
public class Broker extends Node {

	//This maps topic numbers to topic names which is cohesive the same map used by Publishers (In ProtocolUtilities)
	private Map<Integer, String> topicNumbersToTopicNamesMap;
	//This maps topic names to a list of it's subscribers so the system can handle multiple subscribers.
	private Map<String, ArrayList<InetSocketAddress>> topicsToSubscribersMap;
	private Terminal terminal;

	Broker(Terminal terminal, int srcPort) {
		this.terminal = terminal;
		try {
			socket = new DatagramSocket(srcPort);
			listener.go();
			topicsToSubscribersMap = new HashMap<String, ArrayList<InetSocketAddress>>();
			topicNumbersToTopicNamesMap = new HashMap<Integer, String>();
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {
			Terminal terminal = new Terminal("Broker");
			(new Broker(terminal, BROKER_PORT)).start();
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	/* 
	 * This function starts the broker's functionality - It waits for contact by another node!
	 */
	public synchronized void start() throws Exception {
		terminal.println("Waiting for contact...");
		while (true) {
			this.wait();
		}
	}

	/*
	 * This function creates a topic by using data passed in from a creation packet.
	 */
	private boolean createTopic(byte[] data) {
		ArrayList<InetSocketAddress> socketNumbers = new ArrayList<InetSocketAddress>();
		String topicName = ProtocolUtility.getMessage(data);
		if (!topicsToSubscribersMap.containsKey(topicName)) {
			topicsToSubscribersMap.put(topicName, socketNumbers);
			int topicNumber = ProtocolUtility.getTopicNumber(data);
			topicNumbersToTopicNamesMap.put(topicNumber, topicName);
			terminal.println("A topic called " + topicName + " was created!");
			return true;
		}
		return false;
	}

	/* 
	 * This function sends a message as a Datagram Packet given the message as a String and the destination
	 * address to where the message will be sent.
	 */
	private void sendMessage(String message, SocketAddress socketAddress) {
		DatagramPacket packet = createPackets(MESSAGE, 0, message, (InetSocketAddress) socketAddress)[0];
		try {
			socket.send(packet);
			terminal.println("The Broker sent a message: " + message);
		} catch (IOException e) {
			e.printStackTrace();
			terminal.println("The Broker failed to send a message: " + message);
		}
	}

	/* 
	 * This function allows a subscriber to subscribe to a topic by using data passed in from a subscription packet 
	 * and the subscriber's address.
	 */
	private boolean subscribe(byte[] data, SocketAddress subscriberAddress) {
		String nameOfTopic = ProtocolUtility.getMessage(data);
		if (topicsToSubscribersMap.containsKey(nameOfTopic)) {
			ArrayList<InetSocketAddress> subscriberList = topicsToSubscribersMap.get(nameOfTopic);
			subscriberList.add((InetSocketAddress) subscriberAddress);
			topicsToSubscribersMap.remove(nameOfTopic);
			topicsToSubscribersMap.put(nameOfTopic, subscriberList);
			terminal.println("A new subscriber has just subscribed to the topic:" + nameOfTopic + "!");
			return true;
		}
		return false;
	}

	/* 
	 * This function publishes a message for a topic by using data passed in from a publication packet.
	 */
	private boolean publish(byte[] data) {
		int topicNumber = ProtocolUtility.getTopicNumber(data);
		ProtocolUtility.setType(data, PUBLISH);
		if (topicNumbersToTopicNamesMap.containsKey(topicNumber)) {
			String nameOfTopic = topicNumbersToTopicNamesMap.get(topicNumber);
			ArrayList<InetSocketAddress> dstAddressList = topicsToSubscribersMap.get(nameOfTopic);
			if (!dstAddressList.isEmpty()) {
				for (int i = 0; i < dstAddressList.size(); i++) {
					try {
						socket.send(new DatagramPacket(data, data.length, dstAddressList.get(i)));
						terminal.println("The topic " + nameOfTopic + " was published to!");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return true;
		}
		return false;
	}

	/* 
	 * This function implements the functionality of the broker when it receives packets.
	 */
	public synchronized void onReceipt(DatagramPacket packet) {
		try {
			this.notify();
			byte[] data = packet.getData();
			switch (ProtocolUtility.getType(data)) {
			case PUBLISH:
				terminal.println("I just got a request to publish a message!");
				sendAck(packet, terminal);
				if (publish(data)) {
					sendMessage("The publication has been completed!", packet.getSocketAddress());
				} else {
					sendMessage("I don't think this topic exists!", packet.getSocketAddress());
				}
				break;
			case SUBSCRIBE:
				terminal.println("I just got a request to subscribe to a topic!");
				sendAck(packet, terminal);
				if (subscribe(data, packet.getSocketAddress())) {
					sendMessage("The subscription has been completed!", packet.getSocketAddress());
				} else {
					sendMessage("I don't think this topic exists!", packet.getSocketAddress());
				}
				break;
			case ACK:
				terminal.println("I just got an ACK for Sequence Number " + ProtocolUtility.getSequenceNumber(data) + "!");
				break;
			case CREATE_TOPIC:
				terminal.println("I just got a request to create a topic!");
				sendAck(packet, terminal);
				if (createTopic(data)) {
					sendMessage("The topic has been created!", packet.getSocketAddress());
				} else {
					sendMessage("Another Publisher has joined the topic!", packet.getSocketAddress());
				}
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}