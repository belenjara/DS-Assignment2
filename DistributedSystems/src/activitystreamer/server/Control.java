package activitystreamer.server;

import java.io.IOException;
import java.net.BindException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import messages.types.Authentication;
import messages.types.ServerAnnounce;
import messages.util.MessageProcessing;
import messages.util.MessageWrapper;
import activitystreamer.util.Response;
import activitystreamer.util.Settings;
import datalists.server.AnnouncedServer;
import datalists.server.RegisteredClient;

public class Control extends Thread {
	private static final Logger log = LogManager.getLogger();
	private static ArrayList<Connection> connections;

	private static ArrayList<AnnouncedServer> announcedServers;

	private static ArrayList<RegisteredClient> registeredClients;


	//// TODO: add logged clients list...

	private static boolean term=false;
	private static Listener listener;
	private static String serverId;

	protected static Control control = null;

	public static Control getInstance() {
		if(control==null){
			control=new Control();
		} 
		return control;
	}

	public Control() {
		// initialize the connections array
		connections = new ArrayList<Connection>();		
		// start a listener
		try {
			listener = new Listener();
		} catch (IOException e1) {
			log.fatal("failed to startup a listening thread: "+e1);
			System.exit(-1);
		}

		// Initialize the registered clients list.
		registeredClients = new ArrayList<RegisteredClient>();

		// Initialize the announced servers list.
		announcedServers = new ArrayList<AnnouncedServer>();

		//// Server Id...		
		setServerId(Settings.nextSecret());

		initiateConnection();

		start();
	}

	/*
	 * Make a connection to another server if remote hostname is supplied.
	 */
	public void initiateConnection(){
		// make a connection to another server if remote hostname is supplied
		if(Settings.getRemoteHostname()!=null){
			try {				
				Connection conn = outgoingConnection(new Socket(Settings.getRemoteHostname(), Settings.getRemotePort()));				
				conn.setPort(Settings.getRemotePort());
				conn.setHost(Settings.getRemoteHostname());
				conn.setIdClientServer(serverId);
				
				//// Authentication to other server.
				log.info("I'm going to authenticate...");
				Authentication auth = new Authentication();
				auth.doAuthentication(conn);

				if (conn.isOpen() && connections.contains(conn)) {
					//// The connection is updated, type server is specified and that is authenticated.
					conn.setType(Connection.TYPE_SERVER);
					conn.setAuth(true);
				}
			} catch (UnknownHostException e) {
				log.info("The connection already exist..");
			}catch (IOException e) {				
				log.error("failed to make connection to "+Settings.getRemoteHostname()+":"+Settings.getRemotePort()+" :"+e);
				System.exit(-1);
			}
		}
	}


	/*
	 * Processing incoming messages from the connection.
	 * Return true if the connection should close.
	 */
	public synchronized boolean process(Connection con, String msg){
		//log.info("I received a msg from the client: " + msg);

		//// Process the message according to its command. A list of responses is returned.
		List<Response> responses = new MessageProcessing().processMsg(con, msg);

		//// Each response message is send back to the client (or server).
		for(Response response : responses) {
			if (response.getMessage() != null && !response.getMessage().equals("")) {
				//// Write the response to the client (or server).
				log.info("I will respond this to the client: " + response.getMessage());

				//Create the message to be placed on the threads queue
				MessageWrapper msgForQueue = new MessageWrapper(false, response.getMessage());	
				////Place the message on the client's / or other server's queue
				con.getMessageQueue().add(msgForQueue);

				//// we don't use this write method directly, 
				//// we put every message in the queue of the connection to be send (see Connection.java)
				//con.writeMsg(response.getMessage());
			}		
			//// If is necessary to close the connection.
			if (response.getCloseConnection()) {
				log.info("I will close the client's connection..");
				return true;
			}
		}

		//// If is not necessary to close the connection for now.
		return false;
	}

	/*
	 * The connection has been closed by the other party.
	 */
	public synchronized void connectionClosed(Connection con){
		if(!term) connections.remove(con);
	}

	/*
	 * A new incoming connection has been established, and a reference is returned to it
	 */
	public synchronized Connection incomingConnection(Socket s) throws IOException{
		log.info("incomming connection: "+Settings.socketAddress(s));
		Connection c = new Connection(s);
		c.setIncommingConn(true);
		connections.add(c);
		return c;

	}

	/*
	 * A new outgoing connection has been established, and a reference is returned to it
	 */
	public synchronized Connection outgoingConnection(Socket s) throws IOException{
		log.debug("outgoing connection: "+Settings.socketAddress(s));
		Connection c = new Connection(s);
		c.setIncommingConn(false);
		connections.add(c);
		return c;

	}

	@Override
	public void run(){	
		log.info("using activity interval of "+Settings.getActivityInterval()+" milliseconds");
		while(!term){
			// do something with 5 second intervals in between
			try {
				Thread.sleep(Settings.getActivityInterval());
			} catch (InterruptedException e) {
				log.info("received an interrupt, system is shutting down");
				break;
			}
			if(!term){
				log.debug("doing activity");
				//// Server announce every 5 seconds.
				term=doActivity();
			}

		}
		log.info("closing "+connections.size()+" connections");
		// clean up
		for(Connection connection : connections){
			connection.closeCon();	
		}
		listener.setTerm(true);
	}

	public boolean doActivity(){
		//// Server announce every 5 seconds.
		return new ServerAnnounce().sendServerAnnounce();
	}

	public final void setTerm(boolean t){
		term=t;
	}

	public final ArrayList<Connection> getConnections() {
		return connections;
	}


	/**
	 *  * New methods *  
	 */

	/**
	 * Return the list of the servers that were announced.
	 */
	public final ArrayList<AnnouncedServer> getAnnouncedServers() {
		return announcedServers;
	}

	/**
	 * Add a new server to the list of announced servers, if the server already is in the list, is updated.
	 * @param announcedServer
	 */
	public static void addAnnouncedServers(AnnouncedServer announcedServer) {
		boolean exist = false;
		if (Control.announcedServers.size() == 0) {
			Control.announcedServers.add(announcedServer);
		}
		else {
			for(AnnouncedServer as : Control.announcedServers) {
				if (as.getServerId().equals(announcedServer.getServerId())) {
					exist = true;
					as.setPort(announcedServer.getPort());
					as.setLoad(announcedServer.getLoad());	
					as.setHostname(announcedServer.getHostname());
					break;
				}
			}

			if (!exist) {
				Control.announcedServers.add(announcedServer);
			}
		}
	}	

	/**
	 * Get the list of clients that are registered.
	 * @return LIst of registered clients.
	 */
	public final ArrayList<RegisteredClient> getRegisteredClients() {
		return registeredClients;
	}

	/**
	 * Add a registered client to the list.
	 * @param registeredClient
	 */
	public void addRegisteredClients(RegisteredClient registeredClient) {
		Control.registeredClients.add(registeredClient);
	}

	/**
	 * Broadcast a message to all servers (only) connected, except the original sender.
	 * @param msg
	 * @param senderConn
	 */
	public synchronized void broadcastServers(String msg, Connection senderConn) {
		// Broadcast to all connected servers.
		List<Connection> connServers = Control.getInstance().getConnections();
		for(Connection sc : connServers) {			
			if (sc.getType() == Connection.TYPE_SERVER && sc.getAuth() && sc.isOpen()) {
				Boolean isSender = (senderConn != null && sc.equals(senderConn));
				//// We don't want to send to the original sender
				if (!isSender) {
					//log.info("Msg broadcast to servers : " + msg);
					System.out.println("I'm going to send a broadcast to only servers: " + msg);

					//Create the message to be placed on the threads queue
					MessageWrapper msgForQueue = new MessageWrapper(false, msg);	
					////Place the message on the client's / or other server's queue
					sc.getMessageQueue().add(msgForQueue);

					//// we don't use this write method directly, 
					//// we put every message in the queue of the connection to be send (see Connection.java)
					//sc.writeMsg(msg);
				}	
			}
		}
	}

	/**
	 * Broadcast a message to all servers and clients connected, except the original sender.
	 * @param msg
	 * @param senderConn
	 */

	public synchronized void broadcastAll(String msg, Connection senderConn) {
		// Broadcast to all connected servers & clients.
		List<Connection> connections = Control.getInstance().getConnections();
		for(Connection c : connections) {			
			if (c.getAuth() && c.isOpen()) {
				Boolean isSender = (senderConn != null && c.equals(senderConn));
				//// We don't want to send to the original sender
				if (!isSender) {
					System.out.println("I'm going to send a broadcast to all S + C: " + msg);

					//Create the message to be placed on the threads queue
					MessageWrapper msgForQueue = new MessageWrapper(false, msg);	
					////Place the message on the client's / or other server's queue
					c.getMessageQueue().add(msgForQueue);

					//// we don't use this write method directly, 
					//// we put every message in the queue of the connection to be send (see Connection.java)
					////c.writeMsg(msg);
				}	
			}
		}
	}

	/**
	 * Check if the server is authenticated. The property Auth has to be True to be authenticated, otherwise is not.
	 * @param conn
	 * @return TRUE if is authenticated, otherwise FALSE.
	 */
	public final Boolean serverIsAuthenticated(Connection conn) {
		return conn.getAuth();
	}

	/**
	 * @return number of clients connected.
	 */
	public final int getNumberClientsConnected(){		
		List<Connection> connections = Control.getInstance().getConnections();
		int countClients = 0;

		for(Connection c : connections) {
			if (c.getType() == Connection.TYPE_CLIENT && c.getAuth() && c.isOpen()) {
				countClients++;
			}
		}

		return countClients;	
	}

	/**
	 * @return number of servers connected.
	 */
	public int getNumberServersAnnounced(){		
		List<AnnouncedServer> announcedServers = Control.getInstance().getAnnouncedServers();
		int countServers = announcedServers.size();

		return countServers;	
	}

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		Control.serverId = serverId;
	}

	/**
	 * Make a connection to another server using the supplied port and host. We use this for reconnection.
	 * @param oldConnection
	 */
	public void reInitiateConnection(Connection oldConnection) {
		// make a connection to another server if remote hostname is supplied
			
		if(Settings.getRemoteHostname()!=null){
			try {				
				Socket socket = new Socket(oldConnection.getHost(), oldConnection.getPort());
				Connection conn = outgoingConnection(socket);
				//// Authentication to other server.
				log.info("I'm going to authenticate...");
				Authentication auth = new Authentication();
				auth.doAuthentication(conn);

				if (conn.isOpen() && connections.contains(conn)) {
					//// The connection is updated, type server is specified and that is authenticated.
					conn.setType(Connection.TYPE_SERVER);
					conn.setAuth(true);
					
					conn.setMessageQueue(oldConnection.getMessageQueue());
					
					oldConnection.setStatus(Connection.STATUS_CONN_DISABLED);
				}
				
			} catch (UnknownHostException e) {
				log.info("The connection already exist..");
			}catch (IOException e) {				
				log.error("failed to make connection to "+Settings.getRemoteHostname()+":"+Settings.getRemotePort()+" :"+e);
				System.exit(-1);
			}
		}
	}
	
	/**
	 We use this method to add into the queue of a new connection the messages of an old connection queue.
	 * This is used when a child connection is lost and we try to re-send the messages to this child when is back. 
	 * @param conn
	 */
	public void transferMsgQueue(Connection conn) {
		for (Connection c : connections) {
			if (c.getAuth() && c.isOpen()) {
				if (c.getIdClientServer().equals(conn.getIdClientServer())) {
					c.setMessageQueue(conn.getMessageQueue());
					conn.setStatus(Connection.STATUS_CONN_DISABLED);
					conn.closeCon();
					break;
				}
			}
		}
	}
}
