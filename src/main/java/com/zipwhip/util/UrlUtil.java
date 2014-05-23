package com.zipwhip.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 10/11/11
 * Time: 12:42 PM
 */
public class UrlUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(UrlUtil.class);

    /**
     * Get an authenticated URL for posting to Zipwhip.
     *
     * @param host       The host portion of the url.
     * @param apiVersion The Zipwhip API version.
     * @param method     The method to be called on the Zipwhip API.
     * @param params     A string of query params.
     * @param sessionKey The user's sessionKey.
     * @return A Zipwhip URL that is signed.
     * @throws Exception If an error occurs creating or signing the URL.
     */
    public static String getSignedUrl(String host, String apiVersion, String method, String params, String sessionKey) throws Exception {
        return getSignedUrl(host, apiVersion, method, params, sessionKey, null);
    }

    /**
     * Get an authenticated URL for posting to Zipwhip.
     *
     * @param host          The host portion of the url.
     * @param apiVersion    The Zipwhip API version.
     * @param method        The method to be called on the Zipwhip API.
     * @param params        A string of query params.
     * @param authenticator A SignTool to use for signing the URL.
     * @return A Zipwhip URL that is signed.
     * @throws Exception If an error occurs creating or signing the URL.
     */
    public static String getSignedUrl(String host, String apiVersion, String method, String params, SignTool authenticator) throws Exception {
        return getSignedUrl(host, apiVersion, method, params, null, authenticator);
    }

    /**
     * Get an authenticated URL for posting to Zipwhip.
     *
     * @param host          The host portion of the url.
     * @param apiVersion    The Zipwhip API version.
     * @param method        The method to be called on the Zipwhip API.
     * @param params        A string of query params.
     * @param sessionKey    The user's sessionKey.
     * @param authenticator A SignTool to use for signing the URL.
     * @return A Zipwhip URL that is signed.
     * @throws Exception If an error occurs creating or signing the URL.
     */
    public static String getSignedUrl(String host, String apiVersion, String method, String params, String sessionKey, SignTool authenticator) throws Exception {

        StringBuilder builder = new StringBuilder();
        builder.append(params);

        String connector = "&";

        if (StringUtil.isNullOrEmpty(params)) {
            connector = "?";
        }

        if (StringUtil.exists(sessionKey)) {
            builder.append(connector);
            builder.append("session=");
            builder.append(sessionKey);
            connector = "&";
        }

        if (authenticator != null && StringUtil.exists(authenticator.getApiKey())) {
            builder.append(connector);
            builder.append("apiKey=");
            builder.append(authenticator.getApiKey());
        }

        builder.append(connector);
        builder.append("date=");
        builder.append(System.currentTimeMillis());

        String url = apiVersion + method + builder.toString();
        String signature = getSignature(authenticator, url);

        if (StringUtil.exists(signature)) {
            builder.append("&signature=");
            builder.append(signature);
        }

        url = host + apiVersion + method + builder.toString();
        LOGGER.debug("Signed url: " + url);

        return url;
    }

    /**
     * Sign a URL.
     *
     * @param authenticator The SignTool to use in signing.
     * @param url           The URL to sign
     * @return The encrypted secret of an empty string.
     * @throws Exception Id an error occurs signing the URL.
     */
    private static String getSignature(SignTool authenticator, String url) throws Exception {

        if (authenticator == null) {
            return StringUtil.EMPTY_STRING;
        }

        String result = authenticator.sign(url);
        LOGGER.debug("Signing: " + url);

        return result;
    }

    /**
     * Build a String representing a Zipwhip URL. No validation is done ensure this is a valid URL.
     *
     * @param host       The host portion of the url.
     * @param apiVersion The Zipwhip API version.
     * @param method     The method to be called on the Zipwhip API.
     * @return A non-validated Zipwhip URL.
     */
    private static String getUrl(String host, String apiVersion, String method) {
        return host + apiVersion + method;
    }

    /**
     * Converts a String collection to a string of values separated by a separator
     *
     * @param collection - string collection
     * @param separator  - separator
     * @return
     */
    public static String collectionToString(Collection<String> collection, final char separator) {
        if (CollectionUtil.isNullOrEmpty(collection)) return null;

        final StringBuilder builder = new StringBuilder();
        for (String string : collection) {
            if (StringUtil.exists(string)) builder.append(string).append(separator);
        }

        final String result = builder.length() > 0 ? builder.substring(0, builder.length() - 1) : null;
        return StringUtil.isNullOrEmpty(result) ? null : result;
    }

}
