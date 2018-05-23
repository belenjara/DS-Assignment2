package activitystreamer.server;

import activitystreamer.util.Settings;

public class Reconnection {

	Connection conn;
	
	protected static Reconnection reconn = null;

	public static Reconnection getInstance(Connection conn) {
//		if(reconn==null){
			reconn=new Reconnection(conn);
//		} 
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
		long duration = Settings.getConnTimeLimit();
		Control control = Control.getInstance();

		while ((startTime - System.currentTimeMillis()) >= duration) {
			control.reInitiateConnection(conn);	
			if (conn.getStatus().equals(Connection.STATUS_CONN_DISABLED)){
				break;
			}
		}

		//// I could not connect again, I assumed the other server crashed. 	
		if (conn.getStatus().equals(Connection.STATUS_CONN_ERROR)) {

			//// TODO: failure model protocol... (Yanlong protocol)
		}
		
		//// Old connection gone.
		conn.getMessageQueue().clear();
		conn.closeCon();
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