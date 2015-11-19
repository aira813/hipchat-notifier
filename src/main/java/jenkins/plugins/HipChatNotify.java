package jenkins.plugins;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HipChatNotify extends Notifier {
    private static final Logger LOG = Logger.getLogger(HipChatNotify.class.getName());

    private static final Pattern WIN_ENV_VAR_REGEX = Pattern.compile("%([a-zA-Z0-9_]+)%");
    private static final Pattern UNIX_ENV_VAR_REGEX = Pattern.compile("\\$([a-zA-Z0-9_]+)");
    public static final String UNIX_SEP = "/";
    public static final String WINDOWS_SEP = "\\";

    private final String message;
    private final String color;
    private final boolean html;
    private final boolean notify;
    private final String from;
    private final String roomName;
    private final boolean markFlag;
    private final boolean fromCommand;

    @DataBoundConstructor
    public HipChatNotify(
        String message,
        String color,
        boolean html,
        boolean notify,
        String from,
        String roomName,
        boolean markFlag,
        boolean fromCommand) {
        this.message = message;
        this.color = color;
        this.html = html;
        this.notify = notify;
        this.from = from;
        this.roomName = roomName;
        this.markFlag = markFlag;
        this.fromCommand = fromCommand;
    }

    public String getMessage() {
        return message;
    }

    public String getColor() {
        return color;
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

    public boolean isHtml() {
        return html;
    }

    public boolean isMarkFlag() {
        return markFlag;
    }

    public boolean isFromCommand() {
        return fromCommand;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        HipchatAuthentication authentication = getAuthentication();
        if (authentication == null) {
            listener.getLogger().println("[ERROR] Hipcaht Notifier. Can't find authentication. roomName:" + roomName);
            return !markFlag;
        }

        String postMessage;
        if(isFromCommand()) {
            postMessage = executeCommand(build, launcher, listener);
        }else {
            postMessage = message;
        }

        if(message == null){
            listener.getLogger().println("[ERROR] Hipcaht Notifier. Can't find message. roomName:" + roomName);
            return !markFlag;
        }

        HipchatNotificationRequest request = new HipchatNotificationRequest(
            Messages.Api_Url(), authentication.getToken(), authentication.getRoom());
        request.setMessage(postMessage);
        request.setColor(HipchatNotificationRequest.COLOR.valueOf(color));
        request.setFrom(from);
        request.setHtml(html);
        request.setNotify(notify);

        HipchatClient client = new HipchatClient();
        boolean isSuccess = client.exec(request);
        build.getResult();
        return !markFlag || isSuccess;
    }

    private String executeCommand(AbstractBuild build, Launcher launcher, BuildListener listener) {

        String cmdLine = convertSeparator(message, (launcher.isUnix() ? UNIX_SEP : WINDOWS_SEP));

        if (launcher.isUnix()) {
            cmdLine = convertEnvVarsToUnix(cmdLine);
        } else {
            cmdLine = convertEnvVarsToWindows(cmdLine);
        }

        ArgumentListBuilder args = new ArgumentListBuilder();
        if (cmdLine != null) {
            args.addTokenized((launcher.isUnix()) ? "./" + cmdLine : cmdLine);
        }

        if (!launcher.isUnix()) {
            args = args.toWindowsCommand();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            EnvVars env = build.getEnvironment(listener);
            env.putAll(build.getBuildVariables());
            final Proc proc = launcher.decorateFor(build.getBuiltOn()).launch()
                .cmds(args).envs(env).stdout(baos).pwd(build.getWorkspace()).start();
            if (proc.join() != 0) {
                return null;
            }
            return baos.toString();

        } catch (Exception e) {
            e.printStackTrace(listener.fatalError("execute shell error."));
            return null;
        } finally {
            try {
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String convertSeparator(String cmdLine, String newSeparator) {
        String match = "[/" + Pattern.quote("\\") + "]";
        String replacement = Matcher.quoteReplacement(newSeparator);

        Pattern words = Pattern.compile("\\S+");
        Pattern urls = Pattern.compile("(https*|ftp|git):");
        StringBuffer sb = new StringBuffer();
        Matcher m = words.matcher(cmdLine);
        while (m.find()) {
            String item = m.group();
            if (!urls.matcher(item).find()) {
                // Not sure if File.separator is right if executing on slave with OS different from master's one
                //String cmdLine = commandLine.replaceAll("[/\\\\]", File.separator);
                m.appendReplacement(sb, Matcher.quoteReplacement(item.replaceAll(match, replacement)));
            }
        }
        m.appendTail(sb);

        return sb.toString();
    }

    /**
     * Convert Windows-style environment variables to UNIX-style.
     * E.g. "script --opt=%OPT%" to "script --opt=$OPT"
     *
     * @param cmdLine The command line with Windows-style env vars to convert.
     * @return The command line with UNIX-style env vars.
     */
    public static String convertEnvVarsToUnix(String cmdLine) {
        if (cmdLine == null) {
            return null;
        }

        StringBuffer sb = new StringBuffer();

        Matcher m = WIN_ENV_VAR_REGEX.matcher(cmdLine);
        while (m.find()) {
            m.appendReplacement(sb, "\\$$1");
        }
        m.appendTail(sb);

        return sb.toString();
    }

    /**
     * Convert UNIX-style environment variables to Windows-style.
     * E.g. "script --opt=$OPT" to "script --opt=%OPT%"
     *
     * @param cmdLine The command line with Windows-style env vars to convert.
     * @return The command line with UNIX-style env vars.
     */
    public static String convertEnvVarsToWindows(String cmdLine) {
        if (cmdLine == null) {
            return null;
        }

        StringBuffer sb = new StringBuffer();

        Matcher m = UNIX_ENV_VAR_REGEX.matcher(cmdLine);
        while (m.find()) {
            m.appendReplacement(sb, "%$1%");
        }
        m.appendTail(sb);

        return sb.toString();
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

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

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
            if (items.size() == 0) {
                items.add("( No Rooms )");
            }
            return items;
        }

        public FormValidation doCheckRoomName(@QueryParameter String value) {
            if (authentications.length == 0) {
                return FormValidation.error(Messages.Error_NoRooms());
            }
            for (HipchatAuthentication authentication : authentications) {
                if (authentication.getName() != null && authentication.getName().equals(value)) {
                    return FormValidation.ok();
                }
            }
            return FormValidation.error(Messages.Error_NoContent());
        }

    }
}
