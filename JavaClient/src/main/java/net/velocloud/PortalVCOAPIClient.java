package net.velocloud;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PortalVCOAPIClient {
    Boolean VERIFY_SSL = Boolean.parseBoolean(System.getenv("VERIFY_SSL"));
    CloseableHttpClient httpClient = null;
    BasicCookieStore cookieStore = null;
    public PortalVCOAPIClient() {
        cookieStore = new BasicCookieStore();
        if (VERIFY_SSL == true) {
            httpClient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
        } else {
            HttpClientBuilder b = HttpClientBuilder.create().setDefaultCookieStore(cookieStore);

            // setup a Trust Strategy that allows all certificates.
            SSLContext sslContext = null;
            try {
                sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy()
                {
                    public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
                    {
                    return true;
                    }
                }).build();
            } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1) {
                e1.printStackTrace();
            }
            b.setSSLContext(sslContext);

            // don't check Hostnames, either.
            //      -- use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
            HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;

            // here's the special part:
            //      -- need to create an SSL Socket Factory, to use our weakened "trust strategy";
            //      -- and create a Registry, to register it.
            //
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create().register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", sslSocketFactory).build();

            // now, we create connection-manager using our Registry.
            //      -- allows multi-threaded use
            PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            b.setConnectionManager(connMgr);
            CloseableHttpClient closeableHttpClient = b.build();
            httpClient = closeableHttpClient;
        }
    }

    public <T> T callApi(String method, JSONObject params, T returnType) {
        String url = "https://" + System.getenv("VC_HOSTNAME") + "/portal/rest/";
        if (method.contains("login")) {
            url = "https://" + System.getenv("VC_HOSTNAME") + "/portal/";
        }
        
        HttpPost post = new HttpPost(url + method);
        try {
            post.setEntity(new StringEntity(params.toString()));
            CloseableHttpResponse response = this.httpClient.execute(post);
            if (method.contains("login")) {
                try {
                    Header[] h = response.getAllHeaders();
                    for (Header header: h) {
                        if (header.getName().equals("Set-Cookie")) {
                            CookieParser cookie = new CookieParser(header.getValue());
                            BasicClientCookie cookieObj = cookie.getCookieObject();
                            try {
                                this.cookieStore.addCookie(cookieObj);
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                    System.out.println(e);
                }
            }
            String result = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() == 200) {
                return (T) (new JSONParser()).parse(result);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return (T) new JSONObject();
    }

    public void authenticate(String username, String password, Boolean isOperator) {
        if (!isOperator) {
            isOperator = true;
        }
        String path = isOperator ? "operatorLogin" : "enterpriseLogin";
        JSONObject obj = new JSONObject();
        obj.put("username", username);
        obj.put("password", password);
        
        this.callApi("login/" + path, obj, new JSONObject());
    }
}