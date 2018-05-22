package messages.util;

import java.io.BufferedReader;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;

import activitystreamer.server.Connection;

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
		try {
			
			System.out.println(Thread.currentThread().getName() 
					+ " - Reading messages from client connection");
			
			String receivedMessage = null;
			while(this.conn.isOpen() && (receivedMessage = reader.readLine()) != null) {
				System.out.println(Thread.currentThread().getName() 
						+ " - Message received: " + receivedMessage);
				//place the message in the queue for the client connection thread to process
				MessageWrapper msg = new MessageWrapper(true, receivedMessage);
				messageQueue.add(msg);
			}
			
			//If the end of the stream was reached, the client closed the connection
			//Put the exit message in the queue to allow the client connection thread to 
			//close the socket
			MessageWrapper exit = new MessageWrapper(false, "socket_error");
			messageQueue.add(exit);
			
		} catch (SocketException e) {
			//In some platforms like windows, when the end of stream is reached, instead
			//of returning null, the readLine method throws a SocketException, so 
			//do whatever you do when the while loop ends here as well
		    MessageWrapper exit = new MessageWrapper(false, "socket_error");
			messageQueue.add(exit);			
		} catch (Exception e) {			
			e.printStackTrace();
			MessageWrapper exit = new MessageWrapper(false, "general_error");
			messageQueue.add(exit);
		}
	}
}
