package messages.types;
import java.util.ArrayList;
import java.util.List;
import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.util.Response;
import activitystreamer.util.Settings;
import datalists.server.RegisteredClient;
import messages.util.Message;
public class Register {

	public Register () {
	}

	public Response doRegistration(Connection conn, Message message) {	
		Response response = new Response();
		Message messageResp = new Message();
		response.setCloseConnection(false);

		Message responseMsg = Message.CheckMessage(message, Message.USERNAME);	
		if (responseMsg.getCommand().equals(Message.INVALID_MESSAGE)) {
			response.setCloseConnection(true);
			response.setMessage(responseMsg.toString());
			return response;
		}

		responseMsg = Message.CheckMessage(message, Message.SECRET);	
		if (responseMsg.getCommand().equals(Message.INVALID_MESSAGE)) {
			response.setCloseConnection(true);
			response.setMessage(responseMsg.toString());
			return response;
		}

		if (check_message(message.getUsername()) == false || check_message(message.getSecret())==false) {
			messageResp.setCommand(Message.INVALID_MESSAGE);
			messageResp.setInfo("Username or Secret cannot be empty");
			response.setMessage(messageResp.toString());
			response.setCloseConnection(true);
		}

		List<RegisteredClient> registeredClients = Control.getInstance().getRegisteredClients();

		// This server does not knows the client.
		if (check_client(registeredClients, message.getUsername()) == false) {
			RegisteredClient client = new RegisteredClient();		
			client.setUsername(message.getUsername());
			client.setSecret(message.getSecret());
			Control.getInstance().addRegisteredClients(client) ;
			messageResp.setCommand(Message.REGISTER_SUCCESS);
			messageResp.setInfo(String.format(Message.REGISTER_SUCCESS_INFO, message.getUsername()));
			response.setMessage(messageResp.toString());
			response.setCloseConnection(false);
			
			conn.setIdClientServer(message.getUsername());
			
			
			//// Client announce
			Message clientAnnounce = new Message();
			clientAnnounce.setCommand(Message.CLIENT_ANNOUNCE);
			ArrayList<RegisteredClient> clients = new ArrayList<RegisteredClient>();
			RegisteredClient regclient = new RegisteredClient();
			regclient.setUsername(message.getUsername());
			regclient.setParentId(Settings.getIdServer());
			regclient.setSecret(message.getSecret());
			//regclient.setStatus("R");
			clients.add(regclient);		
			clientAnnounce.setClients(clients);
			
			Control.getInstance().broadcastServers(clientAnnounce.toString(), null);	
			
		}	// This server knows the client.
		else if(check_client(registeredClients, message.getUsername()) == true){
			messageResp.setCommand(Message.REGISTER_FAILED);
			messageResp.setInfo(String.format(Message.REGISTER_FAILED_INFO, message.getUsername()));
			response.setMessage(messageResp.toString());
			response.setCloseConnection(true);
			System.out.println("Error");
		}

		return response;
	}		

	public Boolean check_message(String message) {
		if (message != null && !message.equals("")) {
			return true;
		} else {
			return false;
		}
	}


	public Boolean check_client (List<RegisteredClient> clientsList, String usernameToFind) {

		for(RegisteredClient c : clientsList) {
			if (c.getUsername().equals(usernameToFind)) {
				return true;
			}
		}
		return false; 
	}
}
