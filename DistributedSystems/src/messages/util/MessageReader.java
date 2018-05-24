package messages.util;

import java.io.BufferedReader;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;

import activitystreamer.server.Connection;
import activitystreamer.server.Reconnection;

public class MessageReader extends Thread {
	private BufferedReader reader; 
	private BlockingQueue<MessageWrapper> messageQueue;
	private Connection conn;
	
	public MessageReader(BufferedReader reader, BlockingQueue<MessageWrapper> messageQueue, Connection conn) {
		this.reader = reader;
		this.messageQueue = messageQueue;
		this.conn = conn;
	}
	
	@Override
	//This thread reads messages from the client's socket input stream
	public void run() {
		while(this.conn.isOpen()) {
			try {			
				String receivedMessage = null;
				while(this.conn.isOpen() && (receivedMessage = reader.readLine()) != null) {
					//place the message in the queue for the client connection thread to process
					MessageWrapper msg = new MessageWrapper(true, receivedMessage, false);
					messageQueue.add(msg);
					conn.setStatus(Connection.STATUS_CONN_OK);
				}
				
				conn.setStatus(Connection.STATUS_CONN_ERROR);
				break;
				
			} catch (SocketException e) {
				conn.setStatus(Connection.STATUS_CONN_ERROR);
				break;
			} catch (Exception e) {	
				conn.setStatus(Connection.STATUS_CONN_ERROR);
				break;
			}
		}
	}
}
