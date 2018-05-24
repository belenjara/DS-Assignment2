package messages.util;

public class MessageWrapper {
	//// True if comes from a client or other server, False if is an outgoing message.
	private boolean isFromOther;
		
	//// The message.
	private String message;
	
	private boolean closeConnection; 
	
	public MessageWrapper(boolean isFromOther, String message, boolean closeConnection) {
		super();
		this.isFromOther = isFromOther;
		this.message = message;
		this.closeConnection = closeConnection;
		System.out.println("Message adding to queue: " + message);
	}
	
	public MessageWrapper(boolean isFromOther, Message message) {
		super();
		this.isFromOther = isFromOther;
		this.message = message.toString();
	}
	
	public boolean isFromOther() {
		return isFromOther;
	}
	public void setFromOther(boolean isFromOther) {
		this.isFromOther = isFromOther;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isCloseConnection() {
		return closeConnection;
	}

	public void setCloseConnection(boolean closeConnection) {
		this.closeConnection = closeConnection;
	}
}
