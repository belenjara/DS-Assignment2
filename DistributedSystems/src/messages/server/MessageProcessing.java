package messages.server;

import java.util.ArrayList;
import java.util.List;

import activitystreamer.server.Connection;
import activitystreamer.util.Response;

public class MessageProcessing {
	
	public List<Response> processMsg(Connection conn, String msg) {
		Message message = new Message(msg);
		
		String command = message.getCommand();
		
		List<Response> responses = new ArrayList<Response>();
				
		Response response = new Response();
		response.setMessage(null);
		
		switch(command) {					
		case Message.REGISTER:
			conn.setType(Connection.TYPE_CLIENT);
			Register register = new Register(message);
			 response = register.doRegistration(conn, message);
			 responses.add(response);
			break;
		
		case Message.LOCK_REQUEST:
			conn.setType(Connection.TYPE_SERVER);
			//receiveLockRequest
			Lock lockrequest = new Lock(message.getUsername(), message.getSecret());
			response = lockrequest.receiveLockRequest(conn, message);
			responses.add(response);
			break;
			break;
			
		case Message.LOGIN:
			conn.setType(Connection.TYPE_CLIENT);
			// TODO: login
			//// First login, if success, then check for redirect:
			//// The server will follow up a LOGIN_SUCCESS message with a REDIRECT message if the server knows of
			////any other server with a load at least 2 clients less than its own.

			response = new Login().loginProcess(conn,message);
			responses.add(response);
			
			// if login OK, then:
			Response responseRedirect = new Redirection().redirect();
	        if (responseRedirect != null) { responses.add(responseRedirect); } 
			break;
			
		case Message.LOGOUT:
			conn.setType(Connection.TYPE_CLIENT);
			
			response.setCloseConnection(true);
			responses.add(response);
			break;
			
		case Message.AUTHENTICATE:
			conn.setType(Connection.TYPE_SERVER);
			// the server receive a authentication message. 
			Authentication authen = new Authentication();
			//authen.processAuthentication();  
			
			response = authen.processAuthentication(conn,message);
			responses.add(response);
			break;
			
		case Message.ACTIVITY_MESSAGE:
			conn.setType(Connection.TYPE_CLIENT);
			response = new ActivityMsg().receiveActivityMsg(message, conn);
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
			
		case Message.LOCK_ALLOWED:
			conn.setType(Connection.TYPE_SERVER);
			Lock lockAllowed = new Lock(message.getUsername(), message.getSecret());
			response = lockAllowed.receiveLock_allowed(message);
			responses.add(response);
			break;
			
	
			
		case Message.LOCK_DENIED:
			conn.setType(Connection.TYPE_SERVER);
			Lock lockDenied = new Lock(message.getUsername(), message.getSecret());
			response = lockDenied.receiveLockDenied(message);
			responses.add(response);
			break;
			
		case Message.INVALID_MESSAGE:
			response.setMessage(message.toString());
			response.setCloseConnection(true);
			responses.add(response);
			break;
				
			//error
		default:
			response.setMessage(new Message().getInvalidMessage());
			response.setCloseConnection(true);	
			responses.add(response);
			break;
		}
		
		return responses;	
	}
}