package jp.aira813.hipchatnotifier;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jp.aira813.hipchatnotifier.client.HipchatClient;
import jp.aira813.hipchatnotifier.dto.HipchatNotificationRequest;
import jp.aira813.hipchatnotifier.exception.HipchatNotifierException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import java.util.List;

public class NotifyHipchatBuilder extends Builder {
    private final String message;
    private final String color;
    private final boolean html;
    private final boolean notify;
    private final String from;
    private final String roomName;

    @DataBoundConstructor
    public NotifyHipchatBuilder(
        String message,
        String color,
        boolean html,
        boolean notify,
        String from,
        String roomName) {
        this.message = message;
        this.color = color;
        this.html = html;
        this.notify = notify;
        this.from = from;
        this.roomName = roomName;
    }

    public String getMessage() {
        return message;
    }

    public String getColor() {
        return color;
    }

    public boolean isHtml() {
        return html;
    }

    public boolean isNotify() {
        return notify;
    }

    public String getFrom() {
        return from;
    }

    public String getRoomName() {
        return roomName;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {

        HipchatAuthentication authentication = getAuthentication();
        if (authentication == null) {
            listener.getLogger().println("[ERROR] Hipcaht Notifier. Can't find authentication. roomName:" + roomName);
            return false;
        }

        String url = Messages.Api_Url();
        HipchatNotificationRequest request = new HipchatNotificationRequest(url, authentication.getToken(), authentication.getRoom(), message);
        request.setColor(HipchatNotificationRequest.COLOR.valueOf(color));
        request.setFrom(from);
        request.setHtml(html);
        request.setNotify(notify);

        HipchatClient client = new HipchatClient();
        try {
            client.exec(request);
        } catch (HipchatNotifierException e) {
            listener.getLogger().println("[ERROR] Hipcaht Notifier." + e.getMessage());
            return false;
        }
        return true;
    }

    private HipchatAuthentication getAuthentication() {
        for (HipchatAuthentication authentication : getDescriptor().getAuthentications()) {
            if (roomName != null && roomName.equals(authentication.getName())) {
                return authentication;
            }
        }
        return null;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private HipchatAuthentication[] authentications = new HipchatAuthentication[] {};

        public DescriptorImpl() {
            load();
        }

        @Exported
        public HipchatAuthentication[] getAuthentications() {
            return authentications;
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject o) {
            List<HipchatAuthentication> authTokens = req.bindJSONToList(HipchatAuthentication.class, o.get("authentications"));
            this.authentications = authTokens.toArray(new HipchatAuthentication[authTokens.size()]);
            save();
            return true;
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.Setting_Api();
        }

        public ListBoxModel doFillColorItems() {
            ListBoxModel items = new ListBoxModel();
            for (HipchatNotificationRequest.COLOR color : HipchatNotificationRequest.COLOR.values()) {
                items.add(color.name(), color.name());
            }
            return items;
        }

        public ListBoxModel doFillRoomNameItems() {
            ListBoxModel items = new ListBoxModel();
            for (HipchatAuthentication hipchatAuthentication : authentications) {
                items.add(hipchatAuthentication.getName(), hipchatAuthentication.getName());
            }
            return items;
        }

    }
}
