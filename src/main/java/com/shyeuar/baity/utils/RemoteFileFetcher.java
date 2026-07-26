package com.shyeuar.baity.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@Environment(EnvType.CLIENT)
public final class RemoteFileFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger("Baity/RemoteFileFetcher");

    private static final String USER_AGENT = "Baity";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MS = 500L;

    private static volatile boolean networkInitialized;
    private static volatile SSLSocketFactory permissiveSslSocketFactory;

    private RemoteFileFetcher() {}

    public static void init() {
        if (networkInitialized) {
            return;
        }
        synchronized (RemoteFileFetcher.class) {
            if (networkInitialized) {
                return;
            }
            System.setProperty("java.net.preferIPv4Stack", "true");
            System.setProperty("java.net.useSystemProxies", "true");
            networkInitialized = true;
        }
    }

    public static String fetchText(String url, String logLabel) {
        init();
        String label = logLabel == null || logLabel.isBlank() ? url : logLabel;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            FetchAttempt result = fetchOnce(url);
            if (result.success()) {
                int bytes = result.body().length();
                if (attempt > 1) {
                    LOGGER.info("[{}] fetched after {} attempts ({} bytes)", label, attempt, bytes);
                } else {
                    LOGGER.info("[{}] fetched successfully ({} bytes)", label, bytes);
                }
                return result.body();
            }

            if (attempt < MAX_ATTEMPTS) {
                LOGGER.warn("[{}] attempt {}/{} failed: {}; retrying...", label, attempt, MAX_ATTEMPTS, result.error());
                sleepBackoff(attempt);
            } else {
                LOGGER.warn("[{}] fetch failed after {} attempts: {}", label, MAX_ATTEMPTS, result.error());
            }
        }
        return null;
    }

    private static FetchAttempt fetchOnce(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            if (connection instanceof HttpsURLConnection httpsConnection) {
                httpsConnection.setSSLSocketFactory(permissiveSslSocketFactory());
                httpsConnection.setHostnameVerifier((hostname, session) -> true);
            }
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", USER_AGENT);

            int code = connection.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            if (in == null) {
                return FetchAttempt.fail("HTTP " + code + " with empty body");
            }

            String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (code < 200 || code >= 300) {
                return FetchAttempt.fail("HTTP " + code + ": " + truncate(body, 120));
            }
            if (body.isBlank()) {
                return FetchAttempt.fail("HTTP " + code + " returned empty body");
            }
            return FetchAttempt.ok(body);
        } catch (Throwable e) {
            return FetchAttempt.fail(e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static SSLSocketFactory permissiveSslSocketFactory() {
        SSLSocketFactory cached = permissiveSslSocketFactory;
        if (cached != null) {
            return cached;
        }
        synchronized (RemoteFileFetcher.class) {
            if (permissiveSslSocketFactory != null) {
                return permissiveSslSocketFactory;
            }
            try {
                TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
                };
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, trustAllCerts, new SecureRandom());
                permissiveSslSocketFactory = context.getSocketFactory();
                return permissiveSslSocketFactory;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to initialize permissive SSL socket factory", e);
            }
        }
    }

    private static void sleepBackoff(int attempt) {
        try {
            Thread.sleep(RETRY_BACKOFF_MS * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        return trimmed.substring(0, maxLen) + "...";
    }

    private record FetchAttempt(boolean success, String body, String error) {
        static FetchAttempt ok(String body) {
            return new FetchAttempt(true, body, "");
        }

        static FetchAttempt fail(String error) {
            return new FetchAttempt(false, null, error == null ? "unknown error" : error);
        }
    }
}
