package messages.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.util.Response;
import activitystreamer.util.Settings;
import messages.util.Message;
import messages.util.MessageWrapper;

public class Authentication {

	private static final Logger log = LogManager.getLogger();

	/**
	 * When this server receives an authentication from other server.
	 * @param conn
	 * @param msg
	 * @return
	 */
	public Response processAuthentication(Connection conn, Message msg) {
		Response response = new Response();
		Control connMan = Control.getInstance();
		Boolean isAuth = connMan.serverIsAuthenticated(conn);
		//check if the server had already successfully authenticated
		if (isAuth) {
			msg.setCommand(Message.INVALID_MESSAGE);
			msg.setInfo(Message.ERROR_AUTH_INFO2);
			response.setCloseConnection(true);
			response.setMessage(msg.toString());
			return response;
		}

		msg = Message.CheckMessage(msg, Message.SECRET);
		if (msg.getCommand().equals(Message.INVALID_MESSAGE)) {
			response.setCloseConnection(true);
			response.setMessage(msg.toString());
			return response;
		}	

		// check the secret
		if (!msg.getSecret().equals(Settings.getSecret())) {
			msg.setCommand(Message.AUTHENTICATION_FAIL);
			msg.setInfo(String.format(Message.AUTHENTICATION_FAIL_INFO, msg.getSecret() ));
			response.setCloseConnection(true);
			response.setMessage(msg.toString());
			return response;
		}

		// we added the Id of the server as a new property in AUTHENTICATE message
		msg = Message.CheckMessage(msg, Message.ID_SERVER);
		if (msg.getCommand().equals(Message.INVALID_MESSAGE)) {
			response.setCloseConnection(true);
			response.setMessage(msg.toString());
			return response;
		}	 
		
		// do nothing if the authentication succeeded.
		conn.setAuth(true);
		conn.setIdClientServer(msg.getId());
		response.setMessage(null); //// here we have to add the new response of authentication....
		response.setCloseConnection(false);
		return response;
	}

	/**
	 * Sent from one server to another always and only as the first message when connecting.
	 * @param conn
	 */
	public void doAuthentication(Connection conn) {
		Message msg = new Message();
		msg.setCommand(Message.AUTHENTICATE);
		msg.setSecret(Settings.getSecret());			
		// we added the Id of the server as a new property in AUTHENTICATE message
		msg.setId(conn.getIdClientServer());

		String msgStr = msg.toString();
		log.info("Sending authentication msg: " + msgStr);

		//Create the message to be placed on the threads queue
		MessageWrapper msgForQueue = new MessageWrapper(false, msgStr, false);	
		////Place the message on the client's / or other server's queue
		conn.getMessageQueue().add(msgForQueue);

		//// we don't use this write method directly, 
		//// we put every message in the queue of the connection to be send (see Connection.java)	
		////conn.writeMsg(msgStr);
	}

}