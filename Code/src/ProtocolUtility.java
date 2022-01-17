import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

//Author: Prathamesh Sai
//Student Number: 19314123
public final class ProtocolUtility {

	private ProtocolUtility() throws Exception {
		throw new Exception();
	}
	//Global variable for publishers that maps topic numbers to topic names, map agreed with Broker.
	static Map<Integer, String> topicNumbersToNamesMap = new HashMap<Integer, String>();
	
	protected static int getType(byte[] data) {
		return data[0];
	}
	
	protected static void setType(byte[] data, byte type) {
		data[0] = type;
	}
	
	protected static int getSequenceNumber(byte[] data) {
		return data[1];
	}
	
	protected static int getTopicNumber(byte[] data) {
		byte[] intArray = new byte[4];
		for (int i = 0; i < intArray.length; i++) {
			intArray[i] = data[i + 2];
		}
		return ByteBuffer.wrap(intArray).getInt();
	}
	
	protected static String getMessage(byte[] data) {
		byte[] messageArray = new byte[data.length - 6];
		for (int i = 0; i < messageArray.length && data[i + 6] != 0; i++) {
			messageArray[i] = data[i + 6];
		}
		String message = new String(messageArray).trim();
		return message;
	}
}