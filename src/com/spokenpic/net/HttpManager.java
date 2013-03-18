/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.net;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import com.spokenpic.net.ssl.TrustAllSSLSocketFactory;

public class HttpManager {
	// From http://code.google.com/p/meneameandroid/source/browse/trunk/src/com/dcg/util/HttpManager.java
	private static final DefaultHttpClient sClient;
	static {

		// Set basic data
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		HttpProtocolParams.setHttpElementCharset(params, "UTF-8");
		HttpProtocolParams.setUseExpectContinue(params, true);
		HttpProtocolParams.setUserAgent(params, "SpokenPic");

		// Make pool
		ConnPerRoute connPerRoute = new ConnPerRouteBean(12);
		ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);
		ConnManagerParams.setMaxTotalConnections(params, 20);

		// Set timeout
		HttpConnectionParams.setStaleCheckingEnabled(params, false);
		HttpConnectionParams.setConnectionTimeout(params, 15 * 1000);
		HttpConnectionParams.setSoTimeout(params, 15 * 1000);
		HttpConnectionParams.setSocketBufferSize(params, 8192);

		// Some client params
		HttpClientParams.setRedirecting(params, false);

		// Register http/s shemas!
		SchemeRegistry schReg = new SchemeRegistry();
		schReg.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		
		// TODO: Warn: we are trusting all certificates
		//registry.register(new Scheme("https", (trustAll ? new FakeSocketFactory() : SSLSocketFactory.getSocketFactory()), 443));
		try {
			schReg.register(new Scheme("https", new TrustAllSSLSocketFactory(), 443));
		} catch (Exception e) {
			schReg.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
			
		}
		ClientConnectionManager conMgr = new ThreadSafeClientConnManager(
				params, schReg);

		sClient = new DefaultHttpClient(conMgr, params);
	}

	private HttpManager() {
	}

	public static HttpResponse execute(HttpHead head) throws IOException {
		return sClient.execute(head);
	}

	public static HttpResponse execute(HttpHost host, HttpGet get)
			throws IOException {
		return sClient.execute(host, get);
	}

	public static HttpResponse execute(HttpGet get) throws IOException {
		return sClient.execute(get);
	}

	public static HttpResponse execute(HttpPost post) throws IOException {
		return sClient.execute(post);
	}

	public static HttpResponse execute(HttpPut put) throws IOException {
		return sClient.execute(put);
	}

	public static HttpResponse execute(HttpDelete del) throws IOException {
		return sClient.execute(del);
	}

	public static synchronized CookieStore getCookieStore() {
		return sClient.getCookieStore();
	}

}
