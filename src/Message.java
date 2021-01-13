import java.io.Serializable;

public class Message implements Serializable {

    private int fromID;
    private String type;
    private int timestamp;
    public boolean ifInCS;


    public Message(int fromID, String type, int timestamp) {
        this.fromID = fromID;
        this.type = type;
        this.timestamp = timestamp;
        this.ifInCS = false;
    }

    public int getFromID() {
        return fromID;
    }

    public void setFromID(int fromID) {
        this.fromID = fromID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }


    @Override
    public String toString() {
        return "Message{" +
                "fromID=" + fromID +
                ", type='" + type + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }


}
