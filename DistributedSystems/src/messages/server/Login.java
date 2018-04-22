package messages.server;

import java.util.ArrayList;

import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.util.Response;
import connections.server.RegisteredClient;

public class Login {
	
	
	public Response loginProcess(Connection conn, Message msg) {
		Response response = new Response();
		String username = msg.getUsername();
		String anonymous = "anonymous";
		String enteredSecret = msg.getSecret();
		ArrayList<RegisteredClient> registeredClients = Control.getInstance().getRegisteredClients();
		String storedSecret = findSecret(username,registeredClients);
		
		/* if it is anonymous, success
		 * if the secret for an entered username is wrong or username can't be found, send "LOGIN_FAILED" and close the connection
		 * if both username and secret are right, then "LOGIN_SUCCESS"
		 */
		if(username.equals(anonymous)) {
			msg.setCommand(Message.LOGIN_SUCCESS);
			msg.setInfo(String.format(Message.LOGIN_SUCCESS_INFO, username));
			response.setCloseConnection(false);
			//System.out.println("login success as anonymous!");
			
		}else if (enteredSecret.equals(storedSecret)) {
			msg.setCommand(Message.LOGIN_SUCCESS);
			msg.setInfo(String.format(Message.LOGIN_SUCCESS_INFO, username));
			response.setCloseConnection(false);
			//System.out.println("login success!");
			conn.setAuth(true);
		}else {
			msg.setCommand(Message.LOGIN_FAILED);
			msg.setInfo(Message.LOGIN_FAILED_INFO);
			response.setCloseConnection(true);
			//System.out.println("login fail!");
		}
		
		
		response.setMessage(msg.toString());
		return response;
	}

	//find the secret for an entered username, if the username isn't stored, then return secret as null
	public String findSecret (String username,ArrayList<RegisteredClient> registeredClients) {
		
		for(int i = 0; i < registeredClients.size(); i++) {
			//find username
			if(registeredClients.get(i).getUsername().equals(username)) {
				return registeredClients.get(i).getSecret();
			}
			
		}
		return null;
	}
}