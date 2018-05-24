package activitystreamer.server;

import java.io.Console;

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
				conn.setStatus(Connection.STATUS_CONN_OK);
				break;
			}
		}

		//// I could not connect again, I assumed the other server crashed. 	
		if (!status) {

			//// TODO: failure model protocol... (Yanlong protocol)

	    }
		
		conn.getMessageQueue().clear();
		control.connectionClosed(conn);
	}

	/**
	 * When I'm the parent and I have to try to re-send the messages in the queue.
	 */
	private void reSendChildMsgQueue() {

		long startTime = System.currentTimeMillis();
		long duration = Settings.getReSendTimeLimit();
		Control control = Control.getInstance();

		while ((startTime - System.currentTimeMillis()) >= duration) {
			control.transferMsgQueue(conn);
			////if the old connection is closed, it means that the old queue  messages were added in the new connection queue.
			if (conn.getStatus().equals(Connection.STATUS_CONN_DISABLED)) {
				break;
			}
		}
		
		if (conn.getStatus().equals(Connection.STATUS_CONN_ERROR)) {
			//// announce that this client/server is dead? something went wrong..
			//// if is a client maybe I have to send the queue to the root... (the other protocol aim time T, set X)
		}
		
		conn.closeCon();
		conn.getMessageQueue().clear();
		control.connectionClosed(conn);
	}
}