package jp.aira813.hipchatnotifier.client;

import jp.aira813.hipchatnotifier.dto.HipchatNotificationRequest;
import jp.aira813.hipchatnotifier.exception.HipchatNotifierException;
import org.apache.commons.lang.CharEncoding;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;

public class HipchatClient {

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

    public void exec(HipchatNotificationRequest request) throws HipchatNotifierException {
        ArrayList<Header> headers = new ArrayList<Header>();
        headers.add(new BasicHeader("Content-type", "application/x-www-form-urlencoded"));
        HttpClient httpClient = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setDefaultHeaders(headers).build();

        HttpEntity resEntity = null;
        System.out.println(request.toUri());
        HttpPost post = new HttpPost(request.toUri());
        try {
            post.setEntity(new UrlEncodedFormEntity(request.toParams(), CharEncoding.UTF_8));
            HttpResponse response = httpClient.execute(post);
            resEntity = response.getEntity();
            int status = response.getStatusLine().getStatusCode();
            if (request.isTest()) {
                if (status != HttpStatus.SC_ACCEPTED) {
                    throw new HipchatNotifierException("api error! status:" + status);
                }
            } else {
                if (status != HttpStatus.SC_NO_CONTENT) {
                    throw new HipchatNotifierException("api error! status:" + status);
                }
            }
        } catch (IOException e) {
            throw new HipchatNotifierException("api error!", e);
        } finally {
            try {
                EntityUtils.consume(resEntity);
            } catch (IOException e) {
            }
        }
    }

}
