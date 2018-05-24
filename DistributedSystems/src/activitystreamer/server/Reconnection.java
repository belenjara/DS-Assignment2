package activitystreamer.server;

import activitystreamer.util.Settings;

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

		long startTime = System.currentTimeMillis();
		long duration = startTime + Settings.getConnTimeLimit();
		Control control = Control.getInstance();

		//// Old connection gone.
		conn.closeCon();		
		boolean status = false;

		while ((System.currentTimeMillis()) < duration) {
			System.out.println("........... try to reconnect ...........");

			status = control.reInitiateConnection(conn);	
			if (status == true){
				break;
			}
		}

		//// I could not connect again, I assumed the other server crashed. 	
		if (!status) {

			//// TODO: failure model protocol... (Yanlong protocol)

		}

		conn.setOpen(false);
		conn.getMessageQueue().clear();
		control.connectionClosed(conn);
	}

	/**
	 * When I'm the parent and I have to try to re-send the messages in the queue.
	 */
	private void reSendChildMsgQueue() {

		long startTime = System.currentTimeMillis();
		long duration = startTime + Settings.getConnTimeLimit();
		Control control = Control.getInstance();

		//// Old connection gone.
		conn.setStatus(Connection.STATUS_CONN_DISABLED);
		conn.closeCon();		
		boolean status = false;

		while ((System.currentTimeMillis()) < duration) {
			System.out.println("........... try to re-send messages to server/client ...........");
			status = control.transferMsgQueue(conn);
			////if the old connection is closed, it means that the old queue  messages were added in the new connection queue.
			if (status == true){
				break;
			}
		}

		if (!status) {
			//// announce that this client/server is dead? something went wrong..
			//// if is a client maybe I have to send the queue to the root... (the other protocol aim time T, set X)
		}

		conn.setOpen(false);
		conn.getMessageQueue().clear();
		control.connectionClosed(conn);
	}
}