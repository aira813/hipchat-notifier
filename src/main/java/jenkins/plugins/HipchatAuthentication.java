package jenkins.plugins;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;

public class HipchatAuthentication extends AbstractDescribableImpl<HipchatAuthentication> {

    private String name;
    private String token;
    private String room;

    @DataBoundConstructor
    public HipchatAuthentication(String name, String token, String room) {
        this.name = name;
        this.token = token;
        this.room = room;
    }

    @Exported
    public String getName() {
        return name;
    }

    @Exported
    public String getToken() {
        return token;
    }

    @Exported
    public String getRoom() {
        return room;
    }

    @Override
    public String toString() {
        return "HipcahtAuthentication[token:" + token
            + " room:" + room
            + " name:" + name
            + "]";
    }

    @Extension
    public static class HipchatAuthenticationDescriptor extends Descriptor<HipchatAuthentication> {

        @Override
        public String getDisplayName() {
            return Messages.Setting_Api();
        }

        public FormValidation doCheckName(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error(Messages.Error_NoContent());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckToken(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error(Messages.Error_NoContent());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckRoom(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error(Messages.Error_NoContent());
            }
            return FormValidation.ok();
        }

        public FormValidation doTestAuthentication(
            @QueryParameter("room") final String room,
            @QueryParameter("token") final String token) {
            if (StringUtils.isBlank(room) || StringUtils.isBlank(token)) {
                return FormValidation.error("input error.");
            }

            String url = Messages.Api_Url();
            HipchatNotificationRequest request = new HipchatNotificationRequest(url, token, room);

            HipchatClient client = new HipchatClient();
            try {
                client.execForTest(request);
            } catch (Exception e) {
                return FormValidation.error("can not connect to hipchat. check token and room." + e.getMessage());
            }
            return FormValidation.ok("OK!");
        }

    }

}
