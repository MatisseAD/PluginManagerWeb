package fr.matissead.pluginmanagerweb.model;

import java.time.Instant;

/**
 * Represents a GitHub release entry for a plugin.
 * Contains information about available versions from GitHub releases.
 */
public class ReleaseEntry {
    private String repo;
    private String tag;
    private String name;
    private Instant publishedAt;
    private String downloadUrl;
    private String checksum;
    private boolean isLatest;
    private boolean isPrerelease;
    private String body;
    private long assetSize;
    
    public ReleaseEntry() {
    }
    
    public ReleaseEntry(String repo, String tag, String downloadUrl) {
        this.repo = repo;
        this.tag = tag;
        this.downloadUrl = downloadUrl;
    }
    
    public String getRepo() {
        return repo;
    }
    
    public void setRepo(String repo) {
        this.repo = repo;
    }
    
    public String getTag() {
        return tag;
    }
    
    public void setTag(String tag) {
        this.tag = tag;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Instant getPublishedAt() {
        return publishedAt;
    }
    
    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
    
    public String getDownloadUrl() {
        return downloadUrl;
    }
    
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
    
    public String getChecksum() {
        return checksum;
    }
    
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
    
    public boolean isLatest() {
        return isLatest;
    }
    
    public void setLatest(boolean latest) {
        isLatest = latest;
    }
    
    public boolean isPrerelease() {
        return isPrerelease;
    }
    
    public void setPrerelease(boolean prerelease) {
        isPrerelease = prerelease;
    }
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    
    public long getAssetSize() {
        return assetSize;
    }
    
    public void setAssetSize(long assetSize) {
        this.assetSize = assetSize;
    }
}
