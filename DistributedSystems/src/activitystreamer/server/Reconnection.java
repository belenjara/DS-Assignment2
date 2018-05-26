package activitystreamer.server;

import java.util.ArrayList;
import activitystreamer.util.Settings;
import datalists.server.AnnouncedServer;

public class Reconnection  {

	Connection conn;

	protected static Reconnection reconn = null;

	public static Reconnection getInstance(Connection conn) {
		//if(reconn==null){
		reconn=new Reconnection(conn);
		// } 
		return reconn;
	}

	private Reconnection(Connection conn) {
		this.conn = conn;

		// I'm the parent
		if (conn.isIncommingConn()) {
			reSendChildMsgQueue();
		}
		else { // I'm the child
			reconnectParent();
		}
	}

	/**
	 * When I'm the child and I want to reconnect to my parent server.
	 */ 
	private void reconnectParent() {

//		long startTime = System.currentTimeMillis();
//		long duration = startTime + Settings.getConnTimeLimit();
		Control control = Control.getInstance();

		//// Old connection gone.
		conn.closeCon();		
		boolean status = false;

		//// Normal reconnection, within time limit.
//		while ((System.currentTimeMillis()) < duration) {
//			System.out.println("........... try to reconnect ...........");
//			status = control.reInitiateConnection(Settings.getRemoteHostname(), Settings.getRemotePort(), conn.getMessageQueue());	
//			if (status == true){
//				break;
//			}
//		}

		//// I could not connect again, I assumed the other server crashed. 	
//		if (!status) {
			//// TODO: failure model protocol... (Yanlong protocol)

			//// My parent is the root, I'm a level 1 node.
			if (control.getMyLevelDetail().getLevel() == 1) {
				if (control.getMyLevelDetail().isImPotentialRoot()) {
				   System.out.println("I will be the new root  => " + Settings.getIdServer() + ". I will wait for new connections...");
				   Settings.setRemoteHostname(null);
				   control.getMyLevelDetail().setLevel(0);
				   control.getMyLevelDetail().getCandidateList().clear();
				   control.getMyLevelDetail().getCandidateList().add(Settings.getIdServer());
				   
				   //// I will not connect with any other server, just wait until others servers come and through server announce 
				   //// eventually my old child will update their levels (hopefully).
				}
				else {
					//// I will connect to the last child of my (ex) parent
					ArrayList<String> candidateList = control.getMyLevelDetail().getCandidateList();
					String canditate = candidateList.get(candidateList.size()-1); //the one after my (ex) parent.	
					for (AnnouncedServer s : control.getAnnouncedServers()) {
						if (s.getServerId().equals(canditate)) {
							System.out.println(".. trying to connect to the last candidate (new root) => port : " + s.getPort() + " , host : " + s.getHostname() + " ..");
							status = control.reInitiateConnection(s.getHostname(), s.getPort(), conn.getMessageQueue());	
							if (status == true){
								Settings.setRemoteHostname(s.getHostname());
								Settings.setRemotePort(s.getPort());
								break;
							}
						}
					}
				}
			}
			else {
				//// I'm not a level root node. My level is > 1.   
				ArrayList<String> candidateList = control.getMyLevelDetail().getCandidateList();
				String canditate = candidateList.get(2); //the one after my (ex) parent.	
				for (AnnouncedServer s : control.getAnnouncedServers()) {
					if (s.getServerId().equals(canditate)) {
						System.out.println(".. trying to connect to the next candidate => port : " + s.getPort() + " , host : " + s.getHostname() + " ..");
						status = control.reInitiateConnection(s.getHostname(), s.getPort(), conn.getMessageQueue());	
						if (status == true){
							Settings.setRemoteHostname(s.getHostname());
							Settings.setRemotePort(s.getPort());
							break;
						}
					}
				}
//			}
		}

		conn.setOpen(false);
		conn.getMessageQueue().clear();
		control.connectionClosed(conn);
	}

	/**
	 * When I'm the parent and I have to try to re-send the messages in the queue.
	 */
	private void reSendChildMsgQueue() {

//		long startTime = System.currentTimeMillis();
//		long duration = startTime + Settings.getConnTimeLimit();
		Control control = Control.getInstance();

//		//// Old connection gone.
//		conn.setStatus(Connection.STATUS_CONN_DISABLED);
		conn.closeCon();		
//		boolean status = false;
//
//		while ((System.currentTimeMillis()) < duration) {
//			System.out.println("........... try to re-send messages to server/client ...........");
//			status = control.transferMsgQueue(conn);
//			////if the old connection is closed, it means that the old queue  messages were added in the new connection queue.
//			if (status == true){
//				break;
//			}
//		}
//
//		if (!status) {
//			//// announce that this client/server is dead? something went wrong..
//			//// if is a client maybe I have to send the queue to the root... (the other protocol aim time T, set X)
//		}

		conn.setOpen(false);
		conn.getMessageQueue().clear();
		control.connectionClosed(conn);
	}
}