import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;

    public enum MessageType {
        TEXT,
        JOIN,
        LEAVE,
        GROUP_CREATE,
        GROUP_JOIN,
        GROUP_LEAVE,
        GROUP_MESSAGE,
        GROUPCHAT_MESSAGE,
        PRIVATE_MESSAGE,
        USER_ID_CREATE,
        ERROR,
        LIST_OF_NAMES
    }

    private MessageType type;
    private String sender;
    private List<String> receivers;
    private String content;
    private String receiver;

    //     Constructor for Username creating
    public Message(MessageType type, String content) {
        sender = content;
        this.type = type;
        receivers = null;
        this.content = content;
    }

    //     Constructor for joining or leaving
    public Message(MessageType type, String sender, String content) {
        this.sender = sender;
        this.type = type;
        this.content = content;
        receivers = null;
    }

    //      Constructor for a group message / to all
    public Message(MessageType type, String sender, List<String> receivers, String content) {
        this.sender = sender;
        this.receivers = receivers;
        this.type = type;
        this.content = content;

    }

    //      Constructor for Transferring all clients on server
    public Message(MessageType type, List<String> clientsOnServer) {
        this.type = type;
        receivers = clientsOnServer;
        content = null;
        sender = "Server";
    }

    //  Constructor for Private Messaging
    public Message(MessageType type, String sender, String receiver, String content) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        List<String> oneReceiver = new ArrayList<>();
        oneReceiver.add(receiver);
        receivers = oneReceiver;
    }

    public MessageType getType() {
        return type;
    }
    public String getSender() {
        return sender;
    }
    public void setSender(String sender) {
        this.sender = sender;
    }
    public List<String> getReceivers() {
        return receivers;
    }
    public void setReceivers() {
        this.receivers = receivers;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public String getReceiver() { return receiver; }
    public void setReceiver() {this.receiver = receiver; }

    public String toString() {

        String messageString = "Type: " + type + "\n" + "Sender: " + sender + "\n";

        if (receivers != null) {
            messageString += "Receivers: "; // TODO
        }

        messageString += "Content: " + content;
        if (type == MessageType.USER_ID_CREATE) {
            messageString = sender + " changed their username to " + content; // username change String
        }

        return messageString;
    }
}
