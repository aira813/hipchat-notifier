package jenkins.plugins;

import org.apache.commons.lang.CharEncoding;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HipchatClient {

    private static final Logger LOGGER = Logger.getLogger(HipchatClient.class.getName());
    RequestConfig requestConfig;

    public HipchatClient() {
        this(3000, 3000);
    }

    public HipchatClient(int socketTimeout, int connectTimeout) {
        requestConfig = RequestConfig.custom()
            .setConnectTimeout(connectTimeout)
            .setSocketTimeout(socketTimeout)
            .build();
    }

    public boolean exec(HipchatNotificationRequest request) {
        Integer status = post(request.toUri(), request.toParams());
        if (status != HttpStatus.SC_NO_CONTENT) {
            LOGGER.log(Level.WARNING, "api error! status:" + status);
            return false;
        }
        return true;
    }

    public boolean execForTest(HipchatNotificationRequest request) {
        Integer status = post(request.toUri() + "&auth_test=true", request.toParams());
        if (status != HttpStatus.SC_ACCEPTED) {
            LOGGER.log(Level.WARNING, "api error! status:" + status);
            return false;
        }
        return true;
    }

    public Integer post(String uri, List<NameValuePair> param) {
        ArrayList<Header> headers = new ArrayList<Header>();
        headers.add(new BasicHeader("Content-type", "application/x-www-form-urlencoded"));
        HttpClient httpClient = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setDefaultHeaders(headers).build();
        HttpEntity resEntity = null;
        HttpPost post = new HttpPost(uri);
        try {
            post.setEntity(new UrlEncodedFormEntity(param, CharEncoding.UTF_8));
            HttpResponse response = httpClient.execute(post);
            resEntity = response.getEntity();
            return response.getStatusLine().getStatusCode();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "api error!", e);
        } finally {
            try {
                EntityUtils.consume(resEntity);
            } catch (IOException e) {
            }
        }
        return null;
    }

}
