import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import tcdIO.Terminal;

//Author: Prathamesh Sai
//Student Number: 19314123
public abstract class Node {
	//These are the types of packets for this project.
	static final byte ACK = 0;
	static final byte CREATE_TOPIC = 1;
	static final byte PUBLISH = 2;
	static final byte SUBSCRIBE = 3;
	static final byte UNSUBSCRIBE = 4;
	static final byte MESSAGE = 5;

	//These are the packet size which stays constant, and the default dst string used for InetSocketAddress.
	static final int SIZE_OF_PACKET = 1400;
	static final String DEFAULT_DST = "Broker";

	//These are the port numbers for the publishers, subscribers, and brokers for this project.
	static final int SUBSCRIBER_1_PORT = 50002;
	static final int SUBSCRIBER_2_PORT = 50003;
	static final int PUBLISHER_1_PORT = 50004;
	static final int PUBLISHER_2_PORT = 50005;
	static final int BROKER_PORT = 50006;

	DatagramSocket socket;
	Listener listener;
	CountDownLatch latch;

	Node() {
		latch = new CountDownLatch(1);
		listener = new Listener();
		listener.setDaemon(true);
		listener.start();
	}

	/* 
	 * This function sends an acknowledgement (DatagramPacket) through the socket,
	 * and the packet is set with the appropriate acknowledgement type before sending it.
	 */
	protected void sendAck(DatagramPacket packetSentHere, Terminal terminal) {
		//Convert received packet to byte array to set type to acknowledgement.
		byte[] data = packetSentHere.getData();
		ProtocolUtility.setType(data, ACK);
		try {
			//Convert back to DatagramPacket after type has been set and sent through the socket.
			socket.send(new DatagramPacket(data, data.length, packetSentHere.getSocketAddress()));
			terminal.println("ACK has been sent!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** 
	 * This function creates packets by taking in the type of packet, topic number,
	 * message and destination address - The message must fit in one packet for it to work.
	 */
	protected DatagramPacket[] createPackets(int typeOfPacket, int topicNumber, String message, InetSocketAddress dstAddress) {
		int numberOfPacketsForMessage = 0;
		int sizeOfMessage = SIZE_OF_PACKET - 6;
		int offset = 0;

		//Convert the String message to an array of bytes.
		byte[] temporaryArray = message.getBytes();
		byte[] messageArray = new byte[temporaryArray.length];
		for (int i = 0; i < temporaryArray.length; i++) {
			messageArray[i] = temporaryArray[i];
		}

		//Calculate the number of packets in the message.
		for (int messageLength = messageArray.length; messageLength > 0; messageLength -= sizeOfMessage) {
			numberOfPacketsForMessage++;
		}

		//Create DatagramPacket to return with the size of the number of packets required from the message.
		DatagramPacket[] packets = new DatagramPacket[numberOfPacketsForMessage];

		//Create packet data and add to DatagramaPacket packets variable.
		for (int sequenceNumber = 0; sequenceNumber < numberOfPacketsForMessage; sequenceNumber++) {
			byte[] offsetMessage = new byte[sizeOfMessage];
			for (int j = offset; j < offset + messageArray.length; j++) {
				offsetMessage[j] = messageArray[j + offset];
			}
			byte[] data = createPacketData(typeOfPacket, sequenceNumber, topicNumber, offsetMessage);
			DatagramPacket packet = new DatagramPacket(data, data.length, dstAddress);
			packets[sequenceNumber] = packet;
			offset += sizeOfMessage;
		}
		return packets;
	}

	/**
	 * This function creates packet data by creating an array of bytes for a 
	 * Datagram Packet.
	 * HEADER INFORMATION:
	 * byte[0] = typeOfPacket
	 * byte[1] = sequence number for GO-BACK-N protocol.
	 * byte[2->5] = topic number
	 * PAYLOAD:
	 * byte[6->endOfArray] = remaining bytes for the message.
	 */
	private byte[] createPacketData(int typeOfPacket, int sequenceNumber, int topicNumber, byte[] message) {
		byte[] packetData = new byte[SIZE_OF_PACKET];
		packetData[0] = (byte) typeOfPacket;
		packetData[1] = (byte) sequenceNumber;

		//Range of bytes 2->5 in packetData is allocated for the topic number.
		ByteBuffer byteBuffer = ByteBuffer.allocate(4);
		byte[] topicNumberArray = byteBuffer.array();
		for (int i = 0; i < 4; i++) {
			packetData[i + 2] = topicNumberArray[i];
		}

		//Any remaining bytes in packetData is used for the message.
		for (int i = 0; i < message.length && i < SIZE_OF_PACKET; i++) {
			packetData[i + 6] = message[i];
		}
		return packetData;
	}

	/* 
	 * This function is Abstract so it can be adjusted to different classes such as Subscribers,
	 * Publishers, and Brokers. Hence, different classes can behave differently when they receive packets.
	 */
	public abstract void onReceipt(DatagramPacket packet);

	/**
	 * This class listens for incoming packets on a Datagram socket and tells appropriate
	 * receivers about such incoming packets.
	 */
	class Listener extends Thread {
		//Stating to the listener that the socket has been initialized.
		public void go() {
			latch.countDown();
		}

		//Listen for incoming packets and inform receivers.
		public void run() {
			try {
				latch.await();
				// Infinite loop that attempts to receive packets, and notify receivers.
				while (true) {
					DatagramPacket packet = new DatagramPacket(new byte[SIZE_OF_PACKET], SIZE_OF_PACKET);
					socket.receive(packet);
					onReceipt(packet);
				}
			} catch (Exception e) {
				if (!(e instanceof SocketException))
					e.printStackTrace();
			}
		}
	}
}