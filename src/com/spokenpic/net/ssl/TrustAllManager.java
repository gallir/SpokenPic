/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.net.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

// From http://code.google.com/p/meneameandroid/source/browse/#svn/trunk/src/com/dcg/auth
// Se also http://stackoverflow.com/questions/1217141/self-signed-ssl-acceptance-android
public class TrustAllManager implements X509TrustManager {
	public void checkClientTrusted(X509Certificate[] cert, String authType)
			throws CertificateException {
	}

	public void checkServerTrusted(X509Certificate[] cert, String authType)
			throws CertificateException {
	}

	public X509Certificate[] getAcceptedIssuers() {
		return null;
	}
}
