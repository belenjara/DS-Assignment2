package messages.types;

import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.util.Response;
import messages.util.Message;


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
