package jenkins.plugins;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

public class HipchatNotificationRequest {

    private String url;
    private String token;
    private String roomId;
    private String message;

    private COLOR color = COLOR.yellow;
    private boolean notify;
    private boolean html;
    private String from;

    public enum COLOR {
        yellow,
        green,
        red,
        purple,
        gray,
        random
    }

    public HipchatNotificationRequest(String url, String token, String roomId) {
        this.url = url;
        this.token = token;
        this.roomId = roomId;
    }

    public void setColor(COLOR color) {
        this.color = color;
    }

    public void setNotify(boolean notify) {
        this.notify = notify;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setHtml(boolean html) {
        this.html = html;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<NameValuePair> toParams() {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("message", message));
        params.add(new BasicNameValuePair("color", color.name()));
        params.add(new BasicNameValuePair("notify", String.valueOf(notify)));
        params.add(new BasicNameValuePair("message_format", html ? "html" : "text"));
        params.add(new BasicNameValuePair("from", from));
        return params;
    }

    public String toUri() {
        return String.format("%s/room/%s/notification?auth_token=%s", url, roomId, token);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
