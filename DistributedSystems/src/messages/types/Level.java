package messages.types;

import java.util.ArrayList;

import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.util.Response;
import datalists.server.MyLevel;
import messages.util.Message;

public class Level {
	public Response ProcessLevelUpdate(Connection conn, Message msg) {		
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

		msgCheck = Message.CheckMessage(msg, Message.ID_SERVER);
		if (msgCheck.getCommand().equals(Message.INVALID_MESSAGE)) {
			response.setMessage(msgCheck.toString());
			response.setCloseConnection(true);
			return response;
		}

		msgCheck = Message.CheckMessage(msg, Message.LEVEL);
		if (msgCheck.getCommand().equals(Message.INVALID_MESSAGE)) {
			response.setMessage(msgCheck.toString());
			response.setCloseConnection(true);
			return response;
		}

		MyLevel level = Control.getInstance().getMyLevelDetail();
		ArrayList<String> candidates = level.getCandidateList();
		if (candidates != null && candidates.size() > 1) {
			String parentId = candidates.get(1);
			if (msg.getId().equals(parentId)) {
				level.setLevel(msg.getLevel()+1);
				Control.getInstance().setMyLevelDetail(level);
			}
		}
		
		response.setMessage(null);
		response.setCloseConnection(false);
		return response;
	}
}
