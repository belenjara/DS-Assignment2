package activitystreamer.server;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import messages.types.Authentication;
import messages.types.ServerAnnounce;
import messages.util.Message;
import messages.util.MessageProcessing;
import messages.util.MessageWrapper;
import activitystreamer.util.Response;
import activitystreamer.util.Settings;
import datalists.server.AnnouncedServer;
import datalists.server.MyLevel;
import datalists.server.RegisteredClient;

public class Control extends Thread {
	private static final Logger log = LogManager.getLogger();

	private static ArrayList<Connection> connections;

	private static ArrayList<AnnouncedServer> announcedServers;

	private static ArrayList<RegisteredClient> registeredClients;

	private static MyLevel myLevelDetail;

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

		myLevelDetail = new MyLevel();

		//// Server Id...
//		if  (Settings.getIdServer() == null || Settings.getIdServer().equals("")) {
			setServerId(Settings.nextSecret());
//		}else {
//			setServerId(Settings.getIdServer());
//		}

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
		else {
			myLevelDetail.setLevel(0);
		}
	}


	/*
	 * Processing incoming messages from the connection.
	 * Return true if the connection should close.
	 */
	public synchronized void process(Connection con, String msg){
		//log.info("I received a msg from the client: " + msg);

		//// Process the message according to its command. A list of responses is returned.
		List<Response> responses = new MessageProcessing().processMsg(con, msg);

		//// Each response message is send back to the client (or server).
		for(Response response : responses) {
			if (response.getMessage() != null && !response.getMessage().equals("")) {
				//// Write the response to the client (or server).
				log.info("I will respond this to the client: " + response.getMessage());

				//Create the message to be placed on the threads queue
				MessageWrapper msgForQueue = new MessageWrapper(false, response.getMessage(), response.getCloseConnection());	
				////Place the message on the client's / or other server's queue
				con.getMessageQueue().add(msgForQueue);
			}		
		}
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
		int count = 0;
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
				count++;
				term=doActivity(count);
			}

		}
		log.info("closing "+connections.size()+" connections");
		// clean up
		for(Connection connection : connections){
			connection.closeCon();	
		}
		listener.setTerm(true);
	}

	public boolean doActivity(int count){
		//// Server announce every 5 seconds.
		return new ServerAnnounce().sendServerAnnounce(count);
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
					//System.out.println("I'm going to send a broadcast to only servers: " + msg);

					//Create the message to be placed on the threads queue
					MessageWrapper msgForQueue = new MessageWrapper(false, msg, false);	
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
					//System.out.println("I'm going to send a broadcast to all S + C: " + msg);

					//Create the message to be placed on the threads queue
					MessageWrapper msgForQueue = new MessageWrapper(false, msg, false);	
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

	public final ArrayList<String> getServersConnected(){		
		List<Connection> connections = Control.getInstance().getConnections();
		ArrayList<String> ids = new ArrayList<String>();
		for(Connection c : connections) {
			if (c.getType() == Connection.TYPE_SERVER && c.getAuth() && c.isOpen()) {
				if (c.getIdClientServer() != null && !c.getIdClientServer().equals("")) {
					ids.add(c.getIdClientServer());
				}
			}
		}

		return ids;	
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


	public final MyLevel getMyLevelDetail() {
		return myLevelDetail;
	}

	public void setMyLevelDetail(MyLevel levelDetail) {
		Control.myLevelDetail = levelDetail;
	}

	/**
	 * Make a connection to another server using the supplied port and host. We use this for reconnection.
	 * @param oldConnection
	 */
	public boolean reInitiateConnection(String hostname, int port, BlockingQueue<MessageWrapper> messageQueue) {
		// make a connection to another server if remote hostname is supplied

		if(Settings.getRemoteHostname()!=null){
			try {	

				// try to reconnect not to fast and furious :)
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				log.info("I'm going to try to communicate again with server => port :" + port + ", host : " + hostname);
				Socket socket = new Socket(hostname, port);				

				Connection conn = outgoingConnection(socket);
				conn.setPort(port);
				conn.setHost(hostname);				
				conn.setIdClientServer(serverId);
				//// Authentication to other server.
				Authentication auth = new Authentication();
				auth.doAuthentication(conn);

				if (conn.isOpen() && connections.contains(conn)) {
					//// The connection is updated, type server is specified and that is authenticated.
					conn.setType(Connection.TYPE_SERVER);
					conn.setAuth(true);

					//// We add queue messages from old connection to the new connection...
					conn.setMessageQueue(messageQueue);	
				}

				return true;

			} catch (UnknownHostException e) {
				log.info("The connection already exist..");
			}catch (IOException e) {				
				log.error("failed to make connection to "+Settings.getRemoteHostname()+":"+Settings.getRemotePort()+" :"+e);
			}
		}
		return false;
	}

	/**
	 We use this method to add into the queue of a new connection the messages of an old connection queue.
	 * This is used when a child connection is lost and we try to re-send the messages to this child when is back. 
	 * @param conn
	 */
	public boolean transferMsgQueue(Connection conn) {

		// try to reconnect not to fast and furious :)
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log.info("I'm going to try to send queued messages if there is an active connection of client/server => " + conn.getIdClientServer());
		for (Connection c : connections) {
			if (c.getAuth() && c.isOpen()) {
				if (!c.getStatus().equals(Connection.STATUS_CONN_DISABLED) && c.getIdClientServer().equals(conn.getIdClientServer())) {
					log.info(conn.getStatus());
					c.setMessageQueue(conn.getMessageQueue());
					return true;
				}
			}
		}

		return false;
	}

	public void updateLevelDetail(Message msg, String parentId) {
		MyLevel level = getMyLevelDetail();
		ArrayList<String> candidates = level.getCandidateList();
		if (parentId == null && candidates != null && candidates.size() > 1) {
			parentId = candidates.get(1);		
		}

		if (parentId != null) {
			//// Is my parent
			if (msg.getId().equals(parentId)) {		
				//// My level = Parent level +1
				level.setLevel(msg.getLevel()+1);		
				//// MY parent is the root, so I have to update my candidate list
				if (msg.getLevel() == 0) {
					//// Initialize list of candidates..
					ArrayList<String> regCandidates = new ArrayList<String>();
					//// First in the list: me.
					regCandidates.add(Settings.getIdServer());
					//// Second in the list: my parent.
					regCandidates.add(parentId);
					
					int count = 0;
					int myOrder = 0;
					//// Then the elements of my parents child list, except me.
					for(String child : msg.getChildsList()) {
						count++;
						if (!child.equals(Settings.getIdServer())) {
							regCandidates.add(child);
						}
						else {
							myOrder = count;
						}
					}	
					
					//// If I'm the last child of the root, I'm the potential root if the current root crashes.
					if (myOrder == count) {
						level.setImPotentialRoot(true);
					}
					else {
						level.setImPotentialRoot(false);
					}
					
					level.setCandidateList(regCandidates);	
				}
				else
				{
					// I'm not a level one server, I just going to update candidate list with my parent's candidate list
					///// Initialize list of candidates..
					ArrayList<String> regCandidates = new ArrayList<String>();
					//// First in the list: me.
					regCandidates.add(Settings.getIdServer());
					//// Then the elements of my parents candidate list.
					regCandidates.addAll(msg.getCandidatesList());
					level.setCandidateList(regCandidates);		
				}

				setMyLevelDetail(level);
			}
		}

	}
}
