package com.soulbrz.ywqz;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class Api {

    public static final String PREF_NAME = "ywqz_settings";
    public static final String KEY_BASE_URL = "base_url";
    public static final String KEY_PATH_GET = "path_get";
    public static final String KEY_PATH_POST = "path_post";
    public static final String KEY_LAST_CLICK = "last_click_time";
    public static final String KEY_LAST_RESPONSE = "last_post_response";

    public static final String DEFAULT_BASE_URL = "http://127.0.0.1:8080";
    public static final String DEFAULT_PATH_GET = "/get";
    public static final String DEFAULT_PATH_POST = "/post";

    public static final long COOLDOWN_MS = 10 * 1000L;
    private static final int TIMEOUT_MS = 5000;

    private Api() {
    }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static String getBaseUrl(Context context) {
        String url = prefs(context).getString(KEY_BASE_URL, DEFAULT_BASE_URL);
        if (url == null) {
            return "";
        }
        url = url.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    public static String getPath(Context context, String key, String def) {
        String path = prefs(context).getString(key, def);
        if (path == null) {
            return def;
        }
        path = path.trim();
        if (path.length() == 0) {
            return def;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }

    public static String buildUrl(Context context, String pathKey, String pathDefault) {
        String base = getBaseUrl(context);
        if (base.length() == 0) {
            Toast.makeText(context, R.string.tip_set_url, Toast.LENGTH_SHORT).show();
            return null;
        }
        return base + getPath(context, pathKey, pathDefault);
    }

    public static long cooldownRemaining(Context context) {
        long last = prefs(context).getLong(KEY_LAST_CLICK, 0L);
        long remain = last + COOLDOWN_MS - System.currentTimeMillis();
        return remain > 0 ? remain : 0L;
    }

    public static boolean isInCooldown(Context context) {
        return cooldownRemaining(context) > 0;
    }

    public static void markClicked(Context context) {
        prefs(context).edit().putLong(KEY_LAST_CLICK, System.currentTimeMillis()).apply();
    }

    public static String getLastResponse(Context context) {
        String value = prefs(context).getString(KEY_LAST_RESPONSE, "");
        return value == null ? "" : value;
    }

    public static void setLastResponse(Context context, String value) {
        prefs(context).edit().putString(KEY_LAST_RESPONSE, value).apply();
    }

    public static String request(String url, String method, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setUseCaches(false);
        if (body != null) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes("UTF-8"));
            os.flush();
            os.close();
        }
        InputStream is;
        try {
            is = conn.getResponseCode() < 400 ? conn.getInputStream() : conn.getErrorStream();
        } catch (Exception e) {
            is = conn.getErrorStream();
        }
        String result = readAll(is);
        conn.disconnect();
        return result;
    }

    private static String readAll(InputStream is) throws Exception {
        if (is == null) {
            return "";
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        is.close();
        return bos.toString("UTF-8");
    }

    public static String errorMessage(Exception e) {
        String msg = e.getMessage();
        return msg == null || msg.length() == 0 ? e.toString() : msg;
    }
}
