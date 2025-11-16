package fr.matissead.pluginmanagerweb.github;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.matissead.pluginmanagerweb.config.GitHubConfig;
import fr.matissead.pluginmanagerweb.model.ReleaseEntry;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client for interacting with the GitHub API.
 * Handles fetching releases and downloading assets.
 */
public class GitHubClient {
    private static final Logger logger = LoggerFactory.getLogger(GitHubClient.class);
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private final OkHttpClient httpClient;
    private final GitHubConfig config;
    private final Gson gson;
    
    public GitHubClient(GitHubConfig config) {
        this.config = config;
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * Fetches releases for a GitHub repository.
     * @param repo Repository in format "owner/repo"
     * @return List of releases
     */
    public List<ReleaseEntry> getReleases(String repo) {
        List<ReleaseEntry> releases = new ArrayList<>();
        String url = GITHUB_API_BASE + "/repos/" + repo + "/releases";
        
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json");
        
        if (config.hasToken()) {
            requestBuilder.header("Authorization", "Bearer " + config.getToken());
        }
        
        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Failed to fetch releases for {}: HTTP {}", repo, response.code());
                return releases;
            }
            
            ResponseBody body = response.body();
            if (body == null) {
                logger.error("Empty response body for releases from {}", repo);
                return releases;
            }
            
            JsonArray releasesArray = gson.fromJson(body.string(), JsonArray.class);
            boolean isFirstRelease = true;
            
            for (JsonElement element : releasesArray) {
                JsonObject releaseObj = element.getAsJsonObject();
                ReleaseEntry release = parseRelease(repo, releaseObj);
                
                // Mark first non-prerelease as latest
                if (isFirstRelease && !release.isPrerelease()) {
                    release.setLatest(true);
                    isFirstRelease = false;
                }
                
                releases.add(release);
            }
            
            logger.info("Fetched {} releases for {}", releases.size(), repo);
            
        } catch (IOException e) {
            logger.error("Error fetching releases for " + repo, e);
        }
        
        return releases;
    }
    
    private ReleaseEntry parseRelease(String repo, JsonObject releaseObj) {
        ReleaseEntry release = new ReleaseEntry();
        release.setRepo(repo);
        release.setTag(releaseObj.get("tag_name").getAsString());
        release.setName(releaseObj.get("name").getAsString());
        release.setPrerelease(releaseObj.get("prerelease").getAsBoolean());
        
        String publishedAtStr = releaseObj.get("published_at").getAsString();
        release.setPublishedAt(Instant.parse(publishedAtStr));
        
        if (releaseObj.has("body") && !releaseObj.get("body").isJsonNull()) {
            release.setBody(releaseObj.get("body").getAsString());
        }
        
        // Extract first .jar asset as download URL
        JsonArray assets = releaseObj.getAsJsonArray("assets");
        for (JsonElement assetElement : assets) {
            JsonObject asset = assetElement.getAsJsonObject();
            String name = asset.get("name").getAsString();
            if (name.endsWith(".jar")) {
                release.setDownloadUrl(asset.get("browser_download_url").getAsString());
                release.setAssetSize(asset.get("size").getAsLong());
                break;
            }
        }
        
        return release;
    }
    
    /**
     * Downloads a plugin jar file from GitHub.
     * @param downloadUrl URL to download from
     * @param targetFile File to save to
     * @return true if download successful
     */
    public boolean downloadAsset(String downloadUrl, File targetFile) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(downloadUrl);
        
        if (config.hasToken()) {
            requestBuilder.header("Authorization", "Bearer " + config.getToken());
        }
        
        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Failed to download asset: HTTP {}", response.code());
                return false;
            }
            
            ResponseBody body = response.body();
            if (body == null) {
                logger.error("Empty response body when downloading asset");
                return false;
            }
            
            // Ensure parent directory exists
            targetFile.getParentFile().mkdirs();
            
            try (InputStream is = body.byteStream();
                 FileOutputStream fos = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            
            logger.info("Downloaded asset to: {}", targetFile.getAbsolutePath());
            return true;
            
        } catch (IOException e) {
            logger.error("Error downloading asset from " + downloadUrl, e);
            return false;
        }
    }
    
    /**
     * Checks if a newer version is available for a plugin.
     */
    public ReleaseEntry getLatestRelease(String repo) {
        List<ReleaseEntry> releases = getReleases(repo);
        return releases.stream()
                .filter(r -> !r.isPrerelease())
                .findFirst()
                .orElse(null);
    }
}
