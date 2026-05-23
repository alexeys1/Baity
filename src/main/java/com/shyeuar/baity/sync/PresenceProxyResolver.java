package com.shyeuar.baity.sync;

import com.shyeuar.baity.config.ConfigManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class PresenceProxyResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger("Baity/PresenceProxy");
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final int[] PROBE_PORTS = new int[]{7892, 7891, 7890};

    public enum ProxySource {
        DIRECT,
        MANUAL,
        JVM,
        SYSTEM,
        PROBE
    }

    public record ResolvedRoute(ProxySource source, Proxy proxy, String host, int port) {
        public static ResolvedRoute direct() {
            return new ResolvedRoute(ProxySource.DIRECT, null, "", 0);
        }

        public static ResolvedRoute of(ProxySource source, String host, int port) {
            if (host == null || host.isBlank() || port <= 0) {
                return direct();
            }
            InetSocketAddress addr = new InetSocketAddress(host.trim(), port);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
            return new ResolvedRoute(source, proxy, host.trim(), port);
        }
    }

    private static volatile ResolvedRoute sessionRoute = ResolvedRoute.direct();
    private static volatile boolean sessionReady;

    private PresenceProxyResolver() {
    }

    public static void resetSession() {
        sessionRoute = ResolvedRoute.direct();
        sessionReady = false;
    }

    public static boolean establishSession(String healthUrl) {
        resetSession();
        if (healthUrl == null || healthUrl.isBlank()) {
            sessionReady = true;
            return false;
        }

        if (hasManualProxyInConfig()) {
            ResolvedRoute manual = manualRouteFromConfig();
            if (healthCheck(healthUrl, manual)) {
                return adoptSessionRoute(manual);
            }
        }

        if (healthCheck(healthUrl, ResolvedRoute.direct())) {
            return adoptSessionRoute(ResolvedRoute.direct());
        }

        ResolvedRoute jvm = readJvmProxyRoute();
        if (jvm.source() == ProxySource.JVM && healthCheck(healthUrl, jvm)) {
            return adoptSessionRoute(jvm);
        }

        ResolvedRoute system = readSystemProxyRoute(healthUrl);
        if (system.source() == ProxySource.SYSTEM && healthCheck(healthUrl, system)) {
            return adoptSessionRoute(system);
        }

        for (int port : PROBE_PORTS) {
            ResolvedRoute probe = ResolvedRoute.of(ProxySource.PROBE, "127.0.0.1", port);
            if (!healthCheck(healthUrl, probe)) {
                continue;
            }
            return adoptSessionRoute(probe);
        }

        sessionRoute = ResolvedRoute.direct();
        sessionReady = true;
        LOGGER.warn("[PresenceProxy] no reachable route for health check");
        return false;
    }

    private static boolean adoptSessionRoute(ResolvedRoute route) {
        persistWorkingRoute(route);
        sessionRoute = route.source() == ProxySource.PROBE && hasManualProxyInConfig()
                ? manualRouteFromConfig()
                : route;
        sessionReady = true;
        LOGGER.info("[PresenceProxy] session route={} {}:{}", route.source(), route.host(), route.port());
        return true;
    }

    private static void persistWorkingRoute(ResolvedRoute route) {
        if (route.source() == ProxySource.DIRECT) {
            ConfigManager.baityPresenceProxyHost = "";
            ConfigManager.baityPresenceProxyPort = 0;
            ConfigManager.baityPresenceProxySource = "none";
        } else if (route.host() != null && !route.host().isBlank() && route.port() > 0) {
            ConfigManager.baityPresenceProxyHost = route.host();
            ConfigManager.baityPresenceProxyPort = route.port();
            boolean keepManual = route.source() == ProxySource.MANUAL
                    || "manual".equalsIgnoreCase(ConfigManager.baityPresenceProxySource);
            ConfigManager.baityPresenceProxySource = keepManual ? "manual" : "auto";
        }
        ConfigManager.requestSave();
    }

    public static HttpURLConnection openConnection(String url, int attempt) throws Exception {
        URI uri = URI.create(url);
        ResolvedRoute route = routeForAttempt(attempt);
        HttpURLConnection conn = (HttpURLConnection) (route.proxy() == null
                ? uri.toURL().openConnection()
                : uri.toURL().openConnection(route.proxy()));
        if (route.proxy() != null) {
            applyProxyAuthIfConfigured(conn);
        }
        return conn;
    }

    private static boolean healthCheck(String url, ResolvedRoute route) {
        HttpURLConnection connection = null;
        try {
            URI uri = URI.create(url);
            connection = (HttpURLConnection) (route.proxy() == null
                    ? uri.toURL().openConnection()
                    : uri.toURL().openConnection(route.proxy()));
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("x-baity-token", BaityPresenceSync.syncReadToken());
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                connection.setRequestProperty("x-baity-uuid", player.getUUID().toString());
            }
            if (route.proxy() != null) {
                applyProxyAuthIfConfigured(connection);
            }
            int code = connection.getResponseCode();
            if (isReachableStatus(code)) {
                try (InputStream ignored = connection.getInputStream()) {
                }
                return true;
            }
        } catch (Exception ignored) {
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return false;
    }

    private static ResolvedRoute routeForAttempt(int attempt) {
        if (attempt > 0 && ConfigManager.baityPresenceProxyFallbackDirect) {
            return ResolvedRoute.direct();
        }
        if (!sessionReady) {
            if (hasManualProxyInConfig()) {
                return manualRouteFromConfig();
            }
            return ResolvedRoute.direct();
        }
        return sessionRoute;
    }

    private static boolean hasManualProxyInConfig() {
        String host = ConfigManager.baityPresenceProxyHost == null ? "" : ConfigManager.baityPresenceProxyHost.trim();
        return !host.isEmpty() && ConfigManager.baityPresenceProxyPort > 0;
    }

    private static ResolvedRoute manualRouteFromConfig() {
        return ResolvedRoute.of(ProxySource.MANUAL, ConfigManager.baityPresenceProxyHost, ConfigManager.baityPresenceProxyPort);
    }

    private static ResolvedRoute readJvmProxyRoute() {
        String host = System.getProperty("https.proxyHost");
        String portStr = System.getProperty("https.proxyPort");
        if (host == null || host.isBlank()) {
            host = System.getProperty("http.proxyHost");
            portStr = System.getProperty("http.proxyPort");
        }
        int port = parsePort(portStr);
        if (host == null || host.isBlank() || port <= 0) {
            return ResolvedRoute.direct();
        }
        return ResolvedRoute.of(ProxySource.JVM, host, port);
    }

    private static ResolvedRoute readSystemProxyRoute(String urlForUri) {
        try {
            ProxySelector selector = ProxySelector.getDefault();
            if (selector == null) {
                return ResolvedRoute.direct();
            }
            URI uri = URI.create(urlForUri);
            List<Proxy> proxies = selector.select(uri);
            if (proxies == null) {
                return ResolvedRoute.direct();
            }
            for (Proxy proxy : proxies) {
                if (proxy == null || proxy.type() != Proxy.Type.HTTP) {
                    continue;
                }
                if (!(proxy.address() instanceof InetSocketAddress addr)) {
                    continue;
                }
                String host = addr.getHostString();
                int port = addr.getPort();
                if (host != null && !host.isBlank() && port > 0) {
                    return ResolvedRoute.of(ProxySource.SYSTEM, host, port);
                }
            }
        } catch (Exception ignored) {
        }
        return ResolvedRoute.direct();
    }

    private static int parsePort(String portStr) {
        if (portStr == null || portStr.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(portStr.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static void applyProxyAuthIfConfigured(HttpURLConnection conn) {
        String raw = ConfigManager.baityPresenceProxyAuth == null ? "" : ConfigManager.baityPresenceProxyAuth.trim();
        if (raw.isEmpty()) {
            return;
        }
        String token = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty("Proxy-Authorization", "Basic " + token);
    }

    private static boolean isReachableStatus(int code) {
        if (code == 407) {
            return false;
        }
        return code >= 200 && code < 500;
    }
}