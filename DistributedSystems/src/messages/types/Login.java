package messages.types;

import java.util.ArrayList;

import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.util.Response;
import activitystreamer.util.Settings;
import datalists.server.RegisteredClient;
import messages.util.Message;

public class Login {

	/**
	 * When this server receives a login message from a client.
	 * @param conn
	 * @param msg
	 * @return
	 */
	public Response loginProcess(Connection conn, Message msg) {
		Response response = new Response();
		String username = msg.getUsername();
		String anonymous = Message.ANONYMOUS;
		String enteredSecret = msg.getSecret();
		ArrayList<RegisteredClient> registeredClients = Control.getInstance().getRegisteredClients();
		String storedSecret = findSecret(username,registeredClients);

		Message msgCheck = new Message();
		msgCheck = Message.CheckMessage(msg, Message.COMMAND);

		if (msgCheck.getCommand().equals(Message.INVALID_MESSAGE)) {
			response.setMessage(msgCheck.toString());
			response.setCloseConnection(true);
			return response;
		}

		msgCheck = new Message();
		msgCheck = Message.CheckMessage(msg, Message.USERNAME);

		if (msgCheck.getCommand().equals(Message.INVALID_MESSAGE)) {
			response.setMessage(msgCheck.toString());
			response.setCloseConnection(true);
			return response;
		}

		if (!username.equals(anonymous)) {
			msgCheck = new Message();
			msgCheck = Message.CheckMessage(msg, Message.SECRET);
			if (msgCheck.getCommand().equals(Message.INVALID_MESSAGE)) {
				response.setMessage(msgCheck.toString());
				response.setCloseConnection(true);
				return response;
			}
		}

		/* if it is anonymous, success
		 * if the secret for an entered username is wrong or username can't be found, send "LOGIN_FAILED" and close the connection
		 * if both username and secret are right, then "LOGIN_SUCCESS"
		 */
		msg = new Message();
		if(username.equals(anonymous)) {
			msg.setCommand(Message.LOGIN_SUCCESS);
			msg.setInfo(String.format(Message.LOGIN_SUCCESS_INFO, username));
			response.setCloseConnection(false);
			conn.setIdClientServer(username);
			conn.setAuth(true);
			//System.out.println("login success as anonymous!");

		}else if (enteredSecret.equals(storedSecret)) {
			msg.setCommand(Message.LOGIN_SUCCESS);
			msg.setInfo(String.format(Message.LOGIN_SUCCESS_INFO, username));
			response.setCloseConnection(false);
			conn.setIdClientServer(username);
			//System.out.println("login success!");
			conn.setAuth(true);
		}else {
			msg.setCommand(Message.LOGIN_FAILED);
			msg.setInfo(Message.LOGIN_FAILED_INFO);
			response.setCloseConnection(true);
			//System.out.println("login fail!");
		}

		if (msg.getCommand().equals(Message.LOGIN_SUCCESS)) {
			//// Client announce
			Message clientAnnounce = new Message();
			clientAnnounce.setCommand(Message.CLIENT_ANNOUNCE);
			clientAnnounce.setSecret(Settings.getSecret());		
			ArrayList<RegisteredClient> clients = new ArrayList<RegisteredClient>();
			RegisteredClient regclient = new RegisteredClient();
			regclient.setUsername(username);
			regclient.setParentId(Settings.getIdServer());
			regclient.setSecret(enteredSecret);
			regclient.setStatus("IN");
			clients.add(regclient);		
			clientAnnounce.setClients(clients);

			Control.getInstance().broadcastServers(clientAnnounce.toString(), null);
		}

		response.setMessage(msg.toString());
		return response;
	}

	public Response doLogout(Connection conn, Message msg) {

		Response response = new Response();
		response.setCloseConnection(true);

		//// Client announce
		Message clientAnnounce = new Message();
		clientAnnounce.setCommand(Message.CLIENT_ANNOUNCE);
		clientAnnounce.setSecret(Settings.getSecret());		
		ArrayList<RegisteredClient> clients = new ArrayList<RegisteredClient>();
		RegisteredClient regclient = new RegisteredClient();
		regclient.setUsername(conn.getIdClientServer());
		regclient.setParentId(Settings.getIdServer());
		//regclient.setSecret(enteredSecret);
		regclient.setStatus("OUT");
		clients.add(regclient);		
		clientAnnounce.setClients(clients);

		Control.getInstance().broadcastServers(clientAnnounce.toString(), null);
		
		return response;
	}
	
	public Response processClientAnnounce(Connection conn, Message msg) {
		Response response = new Response();
		Message msgCheck = new Message();
		
		Control connMan = Control.getInstance();
		Boolean isAuth = connMan.serverIsAuthenticated(conn); 
		
		//"validate authentication" is only from other servers
		if (!isAuth) {
			Message message = new Message();
			message.setCommand(Message.INVALID_MESSAGE);
			message.setInfo(Message.ERROR_AUTH_INFO);
			response.setCloseConnection(true);
			response.setMessage(message.toString());
			return response;
		}
			
		msgCheck = Message.CheckMessage(msg, Message.COMMAND);
		if (msgCheck.getCommand().equals(Message.INVALID_MESSAGE)) {
			response.setMessage(msgCheck.toString());
			response.setCloseConnection(true);
			return response;
		}

		msgCheck = new Message();
		msgCheck = Message.CheckMessage(msg, Message.CLIENTS);
		if (msgCheck.getCommand().equals(Message.INVALID_MESSAGE)) {
			response.setMessage(msgCheck.toString());
			response.setCloseConnection(true);
			return response;
		}
		
		ArrayList<RegisteredClient> clients = msg.getClients();		
		ArrayList<RegisteredClient> regClients = connMan.getRegisteredClients();		
		boolean clientFound;
		for(RegisteredClient c : clients) {	
			clientFound= false;
			for (RegisteredClient rc :regClients) {
				if (c.getUsername().equals(rc.getUsername())) {
					
					if (c.getSecret() != null) {
						rc.setSecret(c.getSecret());
					}
					rc.setStatus(c.getStatus());
					
					if (c.getParentId() != null) {
						rc.setParentId(c.getParentId());
					}
					clientFound = true;
					break;
				}
			}
			
			if (!clientFound) {
				regClients.add(c);
			}	
		}
		
		response.setCloseConnection(false);
		response.setMessage(null);
		return response;
	}

	//find the secret for an entered username, if the username isn't stored, then return secret as null
	private String findSecret (String username,ArrayList<RegisteredClient> registeredClients) {

		for(int i = 0; i < registeredClients.size(); i++) {
			//find username
			if(registeredClients.get(i).getUsername().equals(username)) {
				return registeredClients.get(i).getSecret();
			}

		}
		return null;
	}
}
