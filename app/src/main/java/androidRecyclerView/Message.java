package androidRecyclerView;

import android.view.MotionEvent;

/**
 * Classe message, un message à une id, le nom de l'envoyeur et le contenu du message.
 */
public class Message {

    protected int id;
    protected String message;
    protected String senderName;

    public Message(int id, String message, String senderName) {
        this.id = id;
        this.message = message;
        this.senderName = senderName;
    }


    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
