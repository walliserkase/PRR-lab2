import java.io.Serializable;

public class Message implements Serializable {
    private final MessageType type;
    private final int stamp;
    private final int senderId;
    private final int value;

    public Message(MessageType type, int stamp, int senderId) {
        this.type = type;
        this.stamp = stamp;
        this.senderId = senderId;
        this.value = -1;
    }

    public Message(MessageType type, int stamp, int senderId, int value) {
        this.type = type;
        this.stamp = stamp;
        this.senderId = senderId;
        this.value = value;
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

    public int getValue() {
        return value;
    }
}
