import java.io.Serializable;

public class Message implements Serializable {
    private final MessageType type;
    private final int stamp;
    private final int senderId;

    public Message(MessageType type, int stamp, int senderId) {
        this.type = type;
        this.stamp = stamp;
        this.senderId = senderId;
    }

    public MessageType getType() {
        return type;
    }

    public int getStamp() {
        return stamp;
    }

    public int getSenderId() {
        return senderId;
    }
}
