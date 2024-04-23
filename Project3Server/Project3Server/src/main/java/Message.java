import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;

    public enum MessageType {
        REGULAR_MOVE,
        MISSILE_MOVE,
        HAIL_MARY_MOVE,
        MISS,
        HIT,
        AVAILABLE_PLAYERS,
        ERROR,
        LEAVE,
        GAME_FOUND,
        LIST_OF_NAMES,
        PLAYER_LOOKING_FOR_GAME
    }

    private MessageType type;
    private String sender;
    private String content;
    private String receiver;
    private String username;

    public Message() {}

    //     Constructor for Username creating
    public Message(MessageType type, String content) {
        sender = content;
        this.type = type;
        this.content = content;
    }

    //     Constructor for joining or leaving
    public Message(MessageType type, String sender, String content) {
        this.sender = sender;
        this.type = type;
        this.content = content;
    }

    //      Constructor for a group message / to all
    public Message(MessageType type, String sender, List<String> receivers, String content) {
        this.sender = sender;
        this.type = type;
        this.content = content;

    }

    //      Constructor for Transferring all clients on server
    public Message(MessageType type, List<String> clientsOnServer) {
        this.type = type;
        content = null;
        sender = "Server";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public MessageType getMessageType() {
        return type;
    }

    public void setMessageType(MessageType type) {
        this.type = type;
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
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public String getReceiver() { return receiver; }
    public void setReceiver() {this.receiver = receiver; }

    public String toMeessageString() {
        String messageString = "Type: " + type + "\n" + "Sender: " + sender + "\n";
        messageString += "Content: " + content;
        return messageString;
    }
}
