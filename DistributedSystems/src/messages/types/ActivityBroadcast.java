package messages.types;

import java.util.ArrayList;

import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.util.Response;
import activitystreamer.util.Settings;
import datalists.server.RegisteredClient;
import messages.util.Message;
import messages.util.MessageWrapper;

public class ActivityBroadcast {

	//Prepare the server to properly handle received Activity Broadcast messages
	public Response receiveServerBroadcast(Message message, Connection conn) {
		//Did I receive this message from others server already? Then Discard... Loop Control (PENDIENTE)
		Response response = new Response();
		response.setCloseConnection(false);
		Control connMan = Control.getInstance();
		Boolean isAuth = connMan.serverIsAuthenticated(conn); 

		//"validate authentication" is only from other servers
		if (!isAuth) {
			Message msg = new Message();
			msg.setCommand(Message.INVALID_MESSAGE);
			msg.setInfo(Message.ERROR_AUTH_INFO);
			response.setCloseConnection(true);
			response.setMessage(msg.toString());
			return response;
		}

		//Validate structure of this message
		Response valid = validateMessage(message);
		if (valid.getCloseConnection()) {
			Message msg = new Message();
			msg.setCommand(Message.INVALID_MESSAGE);
			msg.setInfo(valid.getMessage());
			response.setCloseConnection(true);
			response.setMessage(msg.toString());
			return response;
		}

		//Send the exact same message to everyone BUT the one who sent it (skip the server that already sent to me)
		connMan.broadcastAll(message.toString(), conn);

		return response;

	}	

	/**
	 * If a activity broadcast has to be re-send to a client that did not receive the message before.
	 * I have to see if a have that client connected to this server to send the message, otherwise I just ignore this message. 
	 * @param message
	 * @param conn
	 * @return
	 */
	public Response ProcessActivityBroadcastResend(Message message, Connection conn) {
		Response response = new Response();
		response.setCloseConnection(false);
		response.setMessage(null);
		Control connMan = Control.getInstance();
		Boolean isAuth = connMan.serverIsAuthenticated(conn); 

		//"validate authentication" is only from other servers
		if (!isAuth) {
			Message msg = new Message();
			msg.setCommand(Message.INVALID_MESSAGE);
			msg.setInfo(Message.ERROR_AUTH_INFO);
			response.setCloseConnection(true);
			response.setMessage(msg.toString());
			return response;
		}

		Message responseMsg = Message.CheckMessage(message, Message.CLIENTS);	
		if (responseMsg.getCommand().equals(Message.INVALID_MESSAGE)) {
			response.setCloseConnection(true);
			response.setMessage(responseMsg.toString());
			return response;
		}
		
		responseMsg = Message.CheckMessage(message, Message.ID_MESSAGE);	
		if (responseMsg.getCommand().equals(Message.INVALID_MESSAGE)) {
			response.setCloseConnection(true);
			response.setMessage(responseMsg.toString());
			return response;
		}
		
		responseMsg = Message.CheckMessage(message, Message.AUTHENTICATED_USER);	
		if (responseMsg.getCommand().equals(Message.INVALID_MESSAGE)) {
			response.setCloseConnection(true);
			response.setMessage(responseMsg.toString());
			return response;
		}

		responseMsg = Message.CheckMessage(message, Message.ACTIVITY);	
		if (responseMsg.getCommand().equals(Message.INVALID_MESSAGE)) {
			response.setCloseConnection(true);
			response.setMessage(responseMsg.toString());
			return response;
		}		
		
		Message msgToReSend = new Message();
		msgToReSend.setCommand(Message.ACTIVITY_BROADCAST);
		msgToReSend.setAuthenticatedUser(message.getAuthenticatedUser());
		msgToReSend.setActivity(message.getActivity());	
		MessageWrapper messageToQueue = new MessageWrapper(false, msgToReSend, false);
	
		ArrayList<RegisteredClient> clients = message.getClients();
		for(RegisteredClient client : clients) {
			for (Connection c : connMan.getConnections()){
				if (c.getType().equals(Connection.TYPE_CLIENT) && c.isOpen() && c.getIdClientServer().equals(client.getUsername())) {
					c.getMessageQueue().add(messageToQueue);
				}
			}
		}	
		
		// BROADCAST the original message AGAIN between servers.
		connMan.broadcastServers(message.toString(), conn);
		
		/*{
    "command": "ACTIVITY_BROADCAST_RESEND",   (***)
    "authenticated _user" : "tim"
	"Id_message" : 123,  
	"clients": 
	[
        {
            "username": "aaron",
            "secret": "xxxx"
        },
	{
            "username": "tim",
            "secret": "xxxx"
        }
    ]
    "activity": {}        
	}
		 */

		return response;
	}

	public Response ProcessActivityBroadcastReserve(Message message, Connection conn) {
		Response response = new Response();
		response.setCloseConnection(false);
		response.setMessage(null);
		
		Boolean isAuth = Control.getInstance().serverIsAuthenticated(conn); 

		//"validate authentication" is only from other servers
		if (!isAuth) {
			Message msg = new Message();
			msg.setCommand(Message.INVALID_MESSAGE);
			msg.setInfo(Message.ERROR_AUTH_INFO);
			response.setCloseConnection(true);
			response.setMessage(msg.toString());
			return response;
		}
		
		int myLevel = Control.getInstance().getMyLevelDetail().getLevel();
		if (myLevel == 0 || myLevel == 1) {
			// I keep the message
		}
		
		// BROADCAST the original message AGAIN between servers.
		Control.getInstance().broadcastServers(message.toString(), conn);
		
		/*{
	    "command": "ACTIVITY_BROADCAST_RESERVE",    (***)
	    "secret": "fmnmpp3ai91qb3gc2bvs14g3ue",
	    "authenticated_user": "clare"    -> the original sender (client),
	"time_stamp":"dd/mm/yyyy hh:mm:ss",
	"Client" : {
	            "username": "aaron",
	            "secret": "xxxx"
	         },
	    "activity": {},
	    "Id_message" : 123     
	}
	*/
		
		return response;
	}
	
	

	//This is to validate the body of this Message
	private Response validateMessage(Message msg) {
		Response response = new Response();
		response.setCloseConnection(false);

		//Any other validation for the Activity Message?
		Message responseMsg = Message.CheckMessage(msg, Message.ACTIVITY);	
		if (responseMsg.getCommand().equals(Message.INVALID_MESSAGE)) {
			response.setCloseConnection(true);
			response.setMessage(responseMsg.toString());
			return response;
		}

		return response;
	}

}
