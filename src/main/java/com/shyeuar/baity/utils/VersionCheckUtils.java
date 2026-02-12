package com.shyeuar.baity.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;

public class VersionCheckUtils {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/raueyhs/Baity/releases/latest";
    private static final Pattern VERSION_PATTERN = Pattern.compile("v([0-9]+\\.[0-9]+\\.[0-9]+)");
    
    public static class VersionCheckResult {
        public final boolean isLatest;
        public final String latestVersion;
        public final boolean hasError;
        
        public VersionCheckResult(boolean isLatest, String latestVersion, boolean hasError) {
            this.isLatest = isLatest;
            this.latestVersion = latestVersion;
            this.hasError = hasError;
        }
    }
    
    @SuppressWarnings("deprecation")
    public static CompletableFuture<VersionCheckResult> checkVersionAsync(String currentVersion) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = URI.create(GITHUB_API_URL).toURL();
                
                if (url.getProtocol().equals("https")) {
                    javax.net.ssl.HttpsURLConnection httpsConnection = (javax.net.ssl.HttpsURLConnection) url.openConnection();
                    httpsConnection.setHostnameVerifier((hostname, session) -> true);
                    httpsConnection.setSSLSocketFactory(createTrustAllSocketFactory());
                    httpsConnection.setRequestMethod("GET");
                    httpsConnection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                    httpsConnection.setConnectTimeout(5000);
                    httpsConnection.setReadTimeout(5000);
                    
                    int responseCode = httpsConnection.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        return new VersionCheckResult(true, null, true);
                    }
                    
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(httpsConnection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    String jsonResponse = response.toString();
                    
                    String tagName = extractTagName(jsonResponse);
                    String releaseName = extractReleaseName(jsonResponse);
                    
                    String latestVersion = null;
                    if (releaseName != null) {
                        latestVersion = extractVersionFromTag(releaseName);
                    }
                    if (latestVersion == null && tagName != null) {
                        latestVersion = extractVersionFromTag(tagName);
                    }
                    if (latestVersion == null) {
                        return new VersionCheckResult(true, "Unknown error", true);
                    }
                    
                    String normalizedCurrent = normalizeVersion(currentVersion);
                    String normalizedLatest = normalizeVersion(latestVersion);
                    
                    try {
                        SemanticVersion currentSemVer = SemanticVersion.parse(normalizedCurrent);
                        SemanticVersion latestSemVer = SemanticVersion.parse(normalizedLatest);
                        
                        int comparison = currentSemVer.compareTo(latestSemVer);
                        boolean isLatest = comparison >= 0;
                        
                        return new VersionCheckResult(isLatest, latestVersion, false);
                    } catch (VersionParsingException | IllegalArgumentException e) {
                        boolean isLatest = normalizedCurrent.equals(normalizedLatest);
                        return new VersionCheckResult(isLatest, latestVersion, false);
                    }
                } else {
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                
                    int responseCode = connection.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        return new VersionCheckResult(true, null, true);
                    }
                    
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    String jsonResponse = response.toString();
                    
                    String tagName = extractTagName(jsonResponse);
                    String releaseName = extractReleaseName(jsonResponse);
                    
                    String latestVersion = null;
                    if (releaseName != null) {
                        latestVersion = extractVersionFromTag(releaseName);
                    }
                    if (latestVersion == null && tagName != null) {
                        latestVersion = extractVersionFromTag(tagName);
                    }
                    if (latestVersion == null) {
                        return new VersionCheckResult(true, "Unknown error", true);
                    }
                    
                    String normalizedCurrent = normalizeVersion(currentVersion);
                    String normalizedLatest = normalizeVersion(latestVersion);
                    
                    try {
                        SemanticVersion currentSemVer = SemanticVersion.parse(normalizedCurrent);
                        SemanticVersion latestSemVer = SemanticVersion.parse(normalizedLatest);
                        
                        int comparison = currentSemVer.compareTo(latestSemVer);
                        boolean isLatest = comparison >= 0;
                        
                        return new VersionCheckResult(isLatest, latestVersion, false);
                    } catch (VersionParsingException | IllegalArgumentException e) {
                        boolean isLatest = normalizedCurrent.equals(normalizedLatest);
                        return new VersionCheckResult(isLatest, latestVersion, false);
                    }
                }
                
            } catch (Exception e) {
                return new VersionCheckResult(true, null, true);
            }
        });
    }
    
    private static String extractTagName(String json) {
        int tagIndex = json.indexOf("\"tag_name\":");
        if (tagIndex == -1) return null;
        
        int startIndex = json.indexOf("\"", tagIndex + 11) + 1;
        int endIndex = json.indexOf("\"", startIndex);
        if (endIndex == -1) return null;
        
        return json.substring(startIndex, endIndex);
    }
    
    private static String extractReleaseName(String json) {
        int nameIndex = json.indexOf("\"name\":");
        if (nameIndex == -1) return null;
        
        int startIndex = json.indexOf("\"", nameIndex + 7) + 1;
        int endIndex = json.indexOf("\"", startIndex);
        if (endIndex == -1) return null;
        
        return json.substring(startIndex, endIndex);
    }
    
    private static String extractVersionFromTag(String tagName) {
        if (tagName == null || tagName.isEmpty()) return null;
        
        Matcher matcher = VERSION_PATTERN.matcher(tagName);
        if (matcher.find()) {
            String version = matcher.group(1);
            return "v" + version;
        }
        
        return null;
    }
    
    private static String normalizeVersion(String version) {
        if (version == null) return "";
        version = version.trim();
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }
        return version;
    }
    
    private static SSLSocketFactory createTrustAllSocketFactory() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            return sc.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

