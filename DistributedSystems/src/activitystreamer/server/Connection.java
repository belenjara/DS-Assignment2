package activitystreamer.server;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.util.Settings;
import messages.util.MessageReader;
import messages.util.MessageWrapper;


public class Connection extends Thread {
	private static final Logger log = LogManager.getLogger();
	private DataInputStream in;
	private DataOutputStream out;
	private BufferedReader inreader;
	private PrintWriter outwriter;
	private boolean open = false;
	private Socket socket;
	private boolean term=false;

	private String type;
	private boolean auth;
	public static final String TYPE_SERVER = "SERVER";
	public static final String TYPE_CLIENT = "CLIENT";

	public static final String STATUS_CONN_OK = "OK";
	public static final String STATUS_CONN_ERROR = "ERROR";

	public static final String STATUS_CONN_DISABLED = "DISABLED";

	private boolean incommingConn;

	private String status;

	private String idClientServer; // client username or server id.
	
	private int port;
	private String host;

	//// Our queue!
	private BlockingQueue<MessageWrapper> messageQueue;
	

	Connection(Socket socket) throws IOException{
		in = new DataInputStream(socket.getInputStream());
		out = new DataOutputStream(socket.getOutputStream());
		inreader = new BufferedReader( new InputStreamReader(in));
		outwriter = new PrintWriter(out, true);
		this.socket = socket;
		open = true;
		auth = false;

		status = STATUS_CONN_OK;

		//// Initializing the queue... 
		messageQueue = new LinkedBlockingQueue<MessageWrapper>();

		start();
	}

	public void setOpen(boolean open) {
		this.open = open;
	}

	/*
	 * returns true if the message was written, otherwise false
	 */
	private boolean writeMsg(String msg) {
		if(open){
			outwriter.println(msg);
			outwriter.flush();
			return true;	
		}
		return false;
	}

	public void closeCon(){
		if(open){
			log.info("closing connection "+Settings.socketAddress(socket));
			try {
				term=true;
				inreader.close();
				out.close();
				//open = false;
			} catch (IOException e) {
				// already closed?
				log.error("received exception closing the connection "+Settings.socketAddress(socket)+": "+e);
			}
		}
	}


	public void run(){
		try {
			//Start the client message reader thread. It 'listens' for any
			//incoming messages from the client's socket input stream and places
			//them in a queue (producer)
			MessageReader messageReader = new MessageReader(inreader, messageQueue, this);
			messageReader.setName(this.getName() + "Msg Reader");
			messageReader.start();

			//Monitor the queue to process any incoming messages (consumer)
			while(!term) {
				//This method blocks until there is something to take from the queue
				//(when the messageReader receives a message and places it on the queue
				//or when another thread places a message on this client's queue)
				MessageWrapper msg = null;
				//Thread.sleep(1000); // to give time to change the status of the connection
				if (this.status.equals(STATUS_CONN_OK)) {
					msg = messageQueue.take(); //  this method take the message from the queue and remove it.

					if(msg.isFromOther()) {
						Control.getInstance().process(this, msg.getMessage());
					} else {
						//If the message is from a thread and it isn't exit, then
						//it is a message that needs to be sent to the client
						writeMsg(msg.getMessage());
					}

				} else if (this.status.equals(STATUS_CONN_ERROR)){
					// We are not going to take any message from the queue for now, because something is wrong with the connection (network partition or crash).
					// Other thread in MessageReader.java will continue filling the queue.
					// We are going to try to reconnect to the parent or to re-send queued messages to the child (server or client)
					Reconnection.getInstance(this);
				}
				
				if (msg != null && msg.isCloseConnection()) {
					open=false;
					Control.getInstance().connectionClosed(this);
					break;
				}
			}

			//log.debug("connection closed to "+Settings.socketAddress(socket));
			
			//Control.getInstance().connectionClosed(this);
			//in.close();
		} catch (Exception e) {			
			e.printStackTrace();
		//	Control.getInstance().connectionClosed(this); //?
		}	
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public boolean isOpen() {
		return open;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean getAuth() {
		return auth;
	}

	public void setAuth(Boolean auth) {
		this.auth = auth;
	}

	public BlockingQueue<MessageWrapper> getMessageQueue() {
		return messageQueue;
	}

	public void setMessageQueue(BlockingQueue<MessageWrapper> queue) {
			this.messageQueue.addAll(queue);
	}

	public boolean isIncommingConn() {
		return incommingConn;
	}

	public void setIncommingConn(boolean incommingConn) {
		this.incommingConn = incommingConn;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getIdClientServer() {
		return idClientServer;
	}

	public void setIdClientServer(String id) {
		this.idClientServer = id;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}
}
