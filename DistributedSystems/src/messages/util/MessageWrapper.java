package messages.util;

public class MessageWrapper {
	//// True if comes from a client or other server, False if is an outgoing message.
	private boolean isFromOther;
	
	//// The message.
	private String message;
	
	public MessageWrapper(boolean isFromOther, String message) {
		super();
		this.isFromOther = isFromOther;
		this.message = message;
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
}
