package messages.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import datalists.server.RegisteredClient;

public class Message {
	//private static final Logger log = LogManager.getLogger();

	private String command;
	private String info;
	private String username;
	private String secret;
	private String hostname;
	private String id;
	private int port;
	private Integer load;
	private Integer level = -1;
	private int sequence;
	private HashMap<String, Object> activity;
	
	//// New fields
	private ArrayList<RegisteredClient> clients;
	private ArrayList<String> candidatesList;
	private String authenticatedUser;
	
	//// Constants
	public static final String COMMAND = "command";
	public static final String INFO = "info";
	public static final String USERNAME = "username";
	public static final String SECRET = "secret";
	public static final String HOSTNAME = "hostname";
	public static final String PORT = "port";
	public static final String ACTIVITY = "activity";	
	public static final String ID_SERVER = "id";
	public static final String LOAD = "load";
	public static final String ANONYMOUS = "anonymous";
	public static final String AUTHENTICATED_USER = "authenticated_user";

	public static final String INVALID_MESSAGE = "INVALID_MESSAGE";
	public static final String ERROR_JSON_INFO = "JSON parse error while parsing message";
	public static final String ERROR_COMMAND_INFO = "Unknown command received";
	public static final String ERROR_AUTH_INFO = "Server not authenticated";

	public static final String ERROR_AUTH_INFO2 = "Server already authenticated";

	public static final String ERROR_AUTH_INFO3 = "Client not logged in";

	public static final String ERROR_PROPERTIES_INFO = "the received message did not contain %s";

	public static final String REGISTER = "REGISTER";
	public static final String REGISTER_SUCCESS = "REGISTER_SUCCESS";
	public static final String REGISTER_FAILED = "REGISTER_FAILED";
	public static final String REGISTER_FAILED_INFO = "%s is already registered with the system";
	public static final String REGISTER_SUCCESS_INFO = "register success for %s";


	public static final String LOGIN = "LOGIN";
	public static final String LOGOUT = "LOGOUT";
	public static final String LOGIN_FAILED = "LOGIN_FAILED";
	public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
	public static final String LOGIN_FAILED_INFO = "attempt to login with wrong secret";
	public static final String LOGIN_SUCCESS_INFO = "logged in as user %s";

	public static final String REDIRECT = "REDIRECT";

	public static final String ACTIVITY_MESSAGE = "ACTIVITY_MESSAGE";

	public static final String SERVER_ANNOUNCE = "SERVER_ANNOUNCE";

	public static final String ACTIVITY_BROADCAST = "ACTIVITY_BROADCAST";

	public static final String AUTHENTICATE = "AUTHENTICATE";
	public static final String AUTHENTICATION_FAIL = "AUTHENTICATION_FAIL";
	public static final String AUTHENTICATION_FAIL_INFO = "the supplied secret is incorrect: %s";

//	public static final String LOCK_REQUEST = "LOCK_REQUEST";
//	public static final String LOCK_DENIED = "LOCK_DENIED";
//	public static final String LOCK_ALLOWED = "LOCK_ALLOWED";

	
	//// New messages and attributes
	public static final String AUTHENTICATION_SUCCESS = "AUTHENTICATION_SUCCESS";
	public static final String CLIENT_ANNOUNCE = "CLIENT_ANNOUNCE";
	public static final String LEVEL_UPDATE = "LEVEL_UPDATE";
	public static final String ACTIVITY_BROADCAST_RESERVE = "ACTIVITY_BROADCAST_RESERVE";
	public static final String ACTIVITY_BROADCAST_RESEND = "ACTIVITY_BROADCAST_RESEND";
		
	// not sure if I will finish this :(
	public static final String MESSAGES_CHECK_LIST = "MESSAGES_CHECK_LIST"; 
	public static final String SERVER_LOGOUT = "SERVER_LOGOUT";

	public static final String SEQUENCE = "sequence";
	public static final String CLIENTS = "clients";
	public static final String PARENT_ID = "parent_id";
	public static final String STATUS = "status";
	public static final String CANDIDATE_LIST = "candidate_list";
	public static final String LEVEL = "level";
	public static final String TIME_STAMP = "time_stamp";


	public Message() {
	}

	public Message(int sequence) {
		this.sequence = sequence;
	}

	public Message(String msg) {
		this.prepareMessage(msg);
	}	

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getSecret() {
		return secret;
	}
	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public HashMap<String, Object> getActivity() {
		return activity;
	}

	public void setActivity(HashMap<String, Object> activity) {
		this.activity = activity;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Integer getLoad() {
		return load;
	}

	public void setLoad(Integer load) {
		this.load = load;
	}

	public ArrayList<String> getCandidatesList() {
		return candidatesList;
	}

	public void setCandidatesList(ArrayList<String> candidatesList) {
		this.candidatesList = candidatesList;
	}

	public ArrayList<RegisteredClient> getClients() {
		return clients;
	}

	public void setClients(ArrayList<RegisteredClient> clients) {
		this.clients = clients;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}
	
	public String getAuthenticatedUser() {
		return authenticatedUser;
	}

	public void setAuthenticatedUser(String authenticatedUser) {
		this.authenticatedUser = authenticatedUser;
	}

	public String getInvalidMessage() {
		this.command = INVALID_MESSAGE;
		this.info = ERROR_COMMAND_INFO;

		return this.toString();
	}

	public static Message CheckMessage(Message msg, String property) {
		JSONParser parser = new JSONParser();
		JSONObject jsonMsg = null;
		Message message = new Message();
		try {
			jsonMsg = (JSONObject) parser.parse(msg.toString());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			// INVALID MSG
			message.setCommand(INVALID_MESSAGE);
			message.setInfo(ERROR_JSON_INFO);
			return message;
		}

		if (!jsonMsg.containsKey(property) || jsonMsg.get(property) == null || jsonMsg.get(property).equals("")){
			message.setCommand(INVALID_MESSAGE);
			message.setInfo(String.format(ERROR_PROPERTIES_INFO, property));
			return message;
		}

		return msg;
	}

	private void prepareMessage(String msg) {
		JSONParser parser = new JSONParser();
		JSONObject jsonMsg = null;
		try {
			jsonMsg = (JSONObject) parser.parse(msg);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			// INVALID MSG
			this.setCommand(INVALID_MESSAGE);
			this.setInfo(ERROR_JSON_INFO);
			return;
		}

		if (jsonMsg.containsKey(COMMAND)) {
			this.command = jsonMsg.get(COMMAND).toString();
		}

		if (jsonMsg.containsKey(ID_SERVER)) {
			this.id = jsonMsg.get(ID_SERVER).toString();
		}

		if (jsonMsg.containsKey(INFO)) {
			this.info = jsonMsg.get(INFO).toString();
		}

		if (jsonMsg.containsKey(USERNAME)) {
			this.username = jsonMsg.get(USERNAME).toString();
		}

		if (jsonMsg.containsKey(SECRET)) {
			this.secret = jsonMsg.get(SECRET).toString();
		}

		if (jsonMsg.containsKey(HOSTNAME)) {
			this.hostname = jsonMsg.get(HOSTNAME).toString();
		}

		if (jsonMsg.containsKey(PORT)) {
			this.port = Integer.parseInt(jsonMsg.get(PORT).toString());
		}

		if (jsonMsg.containsKey(LOAD)) {
			this.load = Integer.parseInt(jsonMsg.get(LOAD).toString());
		}
		
		if (jsonMsg.containsKey(AUTHENTICATED_USER)) {
			this.authenticatedUser = jsonMsg.get(AUTHENTICATED_USER).toString();
		}

		if (jsonMsg.containsKey(ACTIVITY)) {
			this.activity = new HashMap<String, Object>();
			JSONObject jsonAct = null;
			try {
				jsonAct = (JSONObject)jsonMsg.get(ACTIVITY);

				for (Object key : jsonAct.keySet()) {
					String keyStr = (String)key;
					Object keyvalue = jsonAct.get(keyStr);

					this.activity.put(keyStr, keyvalue);
				}	
			}catch (Exception e) {
				// INVALID MSG
				this.setCommand(INVALID_MESSAGE);
				this.setInfo(ERROR_JSON_INFO);
				return;
			}
		}

		if (jsonMsg.containsKey(LEVEL)) {
			this.setLevel(Integer.parseInt(jsonMsg.get(LEVEL).toString()));
		}

		if (jsonMsg.containsKey(CANDIDATE_LIST)) {		
			JSONArray carray = (JSONArray) jsonMsg.get(CANDIDATE_LIST);

			this.candidatesList = new ArrayList<String>();
			for(Object c : carray) {
				candidatesList.add(c.toString());
			}	
		}

		if (jsonMsg.containsKey(CLIENTS)) {
			JSONArray jarray = (JSONArray) jsonMsg.get(CLIENTS);
			this.clients = new ArrayList<RegisteredClient>();

			for(Object a : jarray) {
				JSONObject jclient = (JSONObject) a;
				RegisteredClient rc = new RegisteredClient();

				if (jclient.containsKey(USERNAME)) {
					rc.setUsername(jclient.get(USERNAME).toString());
				}

				if (jclient.containsKey(SECRET)) {
					rc.setSecret(jclient.get(SECRET).toString());
				}

				if (jclient.containsKey(STATUS)) {
					rc.setStatus(jclient.get(STATUS).toString());
				}

				if (jclient.containsKey(PARENT_ID)) {
					rc.setParentId(jclient.get(PARENT_ID).toString());
				}

				this.clients.add(rc);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public String toString() {
		JSONObject jsonMsg = new JSONObject();

		if (this.activity != null && !this.activity.isEmpty()) {
			JSONObject jsonAct = new JSONObject();

			for(String k : this.activity.keySet()) {
				jsonAct.put(k, this.activity.get(k));
			}

			jsonMsg.put(ACTIVITY, jsonAct);
		}

		if (this.port > 0) {
			jsonMsg.put(PORT, this.port);
		}

		if (this.hostname != null && !this.hostname.equals("")) {
			jsonMsg.put(HOSTNAME, this.hostname);
		}

		if (this.secret != null && !this.secret.equals("")) {
			jsonMsg.put(SECRET, this.secret);
		}

		if (this.username != null && !this.command.equals("")) {
			jsonMsg.put(USERNAME, this.username);
		}

		if (this.info != null && !this.info.equals("")) {
			jsonMsg.put(INFO, this.info);
		}

		if (this.command != null && !this.command.equals("")) {
			jsonMsg.put(COMMAND, this.command);
		}

		if (this.id != null && !this.id.equals("")) {
			jsonMsg.put(ID_SERVER, this.id);
		}

		if (this.load != null) {
			jsonMsg.put(LOAD, this.load);
		}

		if (this.sequence > 0) {
			jsonMsg.put(SEQUENCE, this.sequence);
		}

		if (this.level > -1) {
			jsonMsg.put(LEVEL, this.level);
		}
		
		if (this.authenticatedUser != null && !this.authenticatedUser.equals("")) {
			jsonMsg.put(AUTHENTICATED_USER, this.authenticatedUser);
		}

		if (this.candidatesList != null && this.candidatesList.size() > 0) {
			JSONArray candlist = new JSONArray();
			for(String c : this.candidatesList) {
				candlist.add(c);
			}
			
			jsonMsg.put(CANDIDATE_LIST, candlist);
		}

		if (this.clients != null && this.clients.size() > 0) {
			JSONArray cliList = new JSONArray();
			for(Object c : this.clients) {
				
				JSONObject cjson = (JSONObject)c;
				
				RegisteredClient client = new RegisteredClient();
				
				if (cjson.containsKey(USERNAME)) {
					client.setUsername(cjson.get(USERNAME).toString());
				}

				if (cjson.containsKey(SECRET)) {
					client.setSecret(cjson.get(SECRET).toString());
				}

				if (cjson.containsKey(STATUS)) {
					client.setStatus(cjson.get(STATUS).toString());
				}

				if (cjson.containsKey(PARENT_ID)) {
					client.setParentId(cjson.get(PARENT_ID).toString());
				}
				
				cliList.add(c);
			}
			
			jsonMsg.put(CLIENTS, cliList);
		}

		Date date = new Date();	
		jsonMsg.put(TIME_STAMP, date.toString());

		return jsonMsg.toJSONString();
	}	
}
