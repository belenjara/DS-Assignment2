package messages.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.server.Connection;
import activitystreamer.util.Response;
import messages.types.ActivityBroadcast;
import messages.types.ActivityMessage;
import messages.types.Authentication;
import messages.types.Login;
import messages.types.Redirection;
import messages.types.Register;
import messages.types.ServerAnnounce;

public class MessageProcessing {
	private static final Logger log = LogManager.getLogger();

	public List<Response> processMsg(Connection conn, String msg) {
		log.info("I received a msg from the client: " + msg);
		Message message = new Message(msg);
		Response response = new Response();
		List<Response> responses = new ArrayList<Response>();				

		// First we verify if the message contains the property "command".
		message = Message.CheckMessage(message, Message.COMMAND);	
			
		String command = message.getCommand();		
		response.setMessage(null);
		
		switch(command) {					
		case Message.REGISTER:
			conn.setType(Connection.TYPE_CLIENT);
			response = new Register().doRegistration(conn, message);
			responses.add(response);
			break;
			
		case Message.LOGIN:
			conn.setType(Connection.TYPE_CLIENT);
			//// First login, if success, then check for redirect:
			//// The server will follow up a LOGIN_SUCCESS message with a REDIRECT message if the server knows of
			////any other server with a load at least 2 clients less than its own.
			response = new Login().loginProcess(conn,message);
			responses.add(response);
			
			// if login OK, then we verify if is necessary to redirect:
			Response responseRedirect = new Redirection().redirect();
	        if (responseRedirect != null) { responses.add(responseRedirect); }
			break; 

		case Message.LOGOUT:
			conn.setType(Connection.TYPE_CLIENT);		
			response = new Login().doLogout(conn, message);
			responses.add(response);
			break;
			
		case Message.AUTHENTICATE:
			conn.setType(Connection.TYPE_SERVER);
			// the server receive a authentication message. 
			Authentication authen = new Authentication();			
			response = authen.processAuthentication(conn, message);
			responses.add(response);
			break;
			
		case Message.ACTIVITY_MESSAGE:
			conn.setType(Connection.TYPE_CLIENT);
			response = new ActivityMessage().receiveActivityMsg(message, conn);
			responses.add(response);
			break;
			
		case Message.SERVER_ANNOUNCE:
			// here this server is receiving a server announce...
			conn.setType(Connection.TYPE_SERVER);
			response = new ServerAnnounce().receiveServerAnnounce(message, conn);
			responses.add(response);
			break;
			
		case Message.ACTIVITY_BROADCAST:
			conn.setType(Connection.TYPE_SERVER);
			response = new ActivityBroadcast().receiveServerBroadcast(message, conn);
			responses.add(response);
			break;
			
		case Message.AUTHENTICATION_FAIL:
			conn.setType(Connection.TYPE_SERVER);
			response.setCloseConnection(true);
			responses.add(response);
			break;
			
		case Message.AUTHENTICATION_SUCCESS:
			conn.setType(Connection.TYPE_SERVER);
			response = new Authentication().processAuthenticationSuccess(conn, message);
			responses.add(response);
			break;
			
		case Message.CLIENT_ANNOUNCE:
			conn.setType(Connection.TYPE_SERVER);
			response = new Login().processClientAnnounce(conn, message);
			responses.add(response);
			break;
			
		case Message.INVALID_MESSAGE:
			response.setMessage(message.toString());
			response.setCloseConnection(true);
			responses.add(response);
			break;
				
		// any unknown command
		default:
			response.setMessage(new Message().getInvalidMessage());
			response.setCloseConnection(true);	
			responses.add(response);
			break;
		}
		
		return responses;	
	}
}