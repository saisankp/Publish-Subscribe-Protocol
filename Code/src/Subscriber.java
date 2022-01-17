import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import tcdIO.Terminal;

//Author: Prathamesh Sai
//Student Number: 19314123
public class Subscriber extends Node {

	private InetSocketAddress dstAddress;
	private boolean incorrectInput;
	private Terminal terminal;

	Subscriber(Terminal terminal, String dstHost, int dstPort, int srcPort) {
		try {
			incorrectInput = true;
			this.terminal = terminal;
			dstAddress = new InetSocketAddress(dstHost, dstPort);
			socket = new DatagramSocket(srcPort);
			listener.go();
		} catch (java.lang.Exception e) {
		}
	}

	public static void main(String[] args) {
		try {
			Terminal terminal = new Terminal("Subscriber-1");
			(new Subscriber(terminal, DEFAULT_DST, BROKER_PORT, SUBSCRIBER_1_PORT)).start();
		} catch (java.lang.Exception e) {
		}
	}

	/* 
	 * This function implements what the subscriber will do when it gets packets.
	 */
	public synchronized void onReceipt(DatagramPacket packet) {
		try {
			this.notify();
			byte[] incomingData = packet.getData();
			if (ProtocolUtility.getType(incomingData) == PUBLISH) {
				terminal.println("A new publication has been published!: " + ProtocolUtility.getMessage(incomingData));
				sendAck(packet, terminal);
			}
			else if (ProtocolUtility.getType(incomingData) == ACK) {
				terminal.println("I just got an ACK for Sequence Number " + ProtocolUtility.getSequenceNumber(incomingData) + "!");
			} 
			else if (ProtocolUtility.getType(incomingData) == MESSAGE) {
				terminal.println("I just got a message: " + ProtocolUtility.getMessage(incomingData));
				sendAck(packet, terminal);
				if (ProtocolUtility.getMessage(incomingData).equals("I don't think this topic exists!")) {
					incorrectInput = true;
				} else {
					incorrectInput = false;
				}
			} 

		} catch (Exception e) {
		}
	}

	/* 
	 * This function allows the subscriber to subscribe to a topic by taking user input about the name of the topic and sending a 
	 * subscription packet to the broker.
	 */
	public synchronized void subscribe() {
		String data = terminal.readString("Enter a topic to subscribe to!: ");
		terminal.println("Sending packet now!");
		try {
			socket.send(createPackets(SUBSCRIBE, 0, data, dstAddress)[0]);
		} catch (IOException e) {
		}
		terminal.println("Packet has been sent!");
	}


	/* 
	 * This function implements the subscriber's functionality - It can subscribe to a topic to receive communication from publishers.
	 */
	public synchronized void start() throws Exception {
		while (incorrectInput == true) {
			String startingString = terminal.readString("Enter SUBSCRIBE to subscribe to a topic: ");
			if (startingString.equals("SUBSCRIBE")) {
				subscribe();
				this.wait(); // wait for the ack first
				this.wait(); // then wait for the message
			} else {
				terminal.println("Invalid input.");
				incorrectInput = true;
			}
		}
		while (true) {
			this.wait();
		}
	}
}