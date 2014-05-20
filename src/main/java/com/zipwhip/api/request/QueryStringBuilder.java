package com.zipwhip.api.request;

import com.zipwhip.util.StringUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * Date: Jul 18, 2009
 * Time: 10:42:41 AM
 * <p/>
 * Simplifies the work of creating a HTTP parameter list. Calling {@code build} will return a request query string
 * including the leading '?'. If no params have been added the result will be an empty string.
 *
 * Optionally supports encoding the parameters with UTF-8 encoding.
 * <p/>
 * Since this class is a builder it should be used once and thrown away.
 */
public class QueryStringBuilder {

    private final StringBuilder sb = new StringBuilder("?");

    public QueryStringBuilder() {

    }

    public QueryStringBuilder(Map<String, Object> params) {
        this.params(params);
    }

    /**
     * Add map of params to the list. If any of the param values are an instance of
     * {@code java.util.Collection} then they will be treated as a list of params.
     * <p/>
     * For example ?param=1&param=2&param=3.
     * <p/>
     * This method will NOT encode the parameters.
     *
     * @param values A map of Objects to be converted into a URL query string.
     * @return The resulting query string.
     */
    public QueryStringBuilder params(Map<String, Object> values) {
        return params(values, false);
    }

    /**
     * Add map of params to the list. If any of the param values are an instance of
     * {@code java.util.Collection} then they will be treated as a list of params.
     * <p/>
     * For example ?param=1&param=2&param=3.
     *
     * @param values A map of Objects to be converted into a URL query string.
     * @param encode If {@code true} then encode the params, otherwise don't.
     * @return The resulting query string.
     */
    public QueryStringBuilder params(Map<String, Object> values, boolean encode) {

        if (values == null) {
            return this;
        }

        for (String key : values.keySet()) {

            Object value = values.get(key);

            if (value == null) {
                // don't put nulls in there
                continue;
            }

            if (value instanceof Collection) {

                for (Object object : (Collection) value) {
                    param(key, String.valueOf(object), encode);
                }

            } else {

                param(key, String.valueOf(value), encode);
            }
        }

        return this;
    }

    /**
     * Add a param to the list.
     *
     * @param key The param key.
     * @param value The param value.
     * @return The resulting query string.
     */
    public QueryStringBuilder param(String key, String value) {
        return param(key, value, false);
    }

    public QueryStringBuilder param(String key, String value, boolean encode) {

        if (StringUtil.isNullOrEmpty(key)) {
            return this;
        }

        String string;

        if (encode) {
            try {
                string = URLEncoder.encode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                string = value;
            }
        } else {
            string = StringUtil.replaceAll(value, " ", "+");
        }

        sb.append(key);
        sb.append("=");

        if (string != null) {
            sb.append(string);
        }

        sb.append("&");

        return this;
    }

    /**
     * Produce the resulting param string.
     * If no params have been added the result will be an empty string.
     *
     * @return The resulting param string.
     */
    public String build() {
        String result = sb.toString();
        return result.substring(0, result.length() - 1);
    }

    public static String getUrl(String url, Map<String, Object> params) {
        QueryStringBuilder rb = new QueryStringBuilder();

        rb.params(params, true);

        return url + rb.build();
    }

}
