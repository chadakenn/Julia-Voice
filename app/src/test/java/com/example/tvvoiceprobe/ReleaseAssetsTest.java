package com.example.tvvoiceprobe;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReleaseAssetsTest {
    @Test public void acceptsTheCanonicalReleaseAsset() {
        ReleaseAssets.validateAssetUrl(
                "https://github.com/chadakenn/Kellies-Voice/releases/download/v0.6/app-release.apk",
                "v0.6", "app-release.apk");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsRetiredRepoDownloadUrls() {
        ReleaseAssets.validateAssetUrl(
                "https://github.com/chadakenn/Julia-Voice/releases/download/v0.6/update.json",
                "v0.6", "update.json");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsALookalikeRepository() {
        ReleaseAssets.validateAssetUrl(
                "https://github.com/chadakenn/Kellies-Voice.evil/releases/download/v0.6/app-release.apk",
                "v0.6", "app-release.apk");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAnExtraQuery() {
        ReleaseAssets.validateAssetUrl(
                "https://github.com/chadakenn/Kellies-Voice/releases/download/v0.6/app-release.apk?raw=1",
                "v0.6", "app-release.apk");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsADifferentFilenameThanTheTagClaims() {
        ReleaseAssets.validateAssetUrl(
                "https://github.com/chadakenn/Kellies-Voice/releases/download/v0.6/app-release.apk",
                "v0.6", "update.json");
    }

    @Test public void stableTagsMatchTheUpdater() {
        assertTrue(ReleaseAssets.stableTag("v0.6"));
        assertTrue(ReleaseAssets.stableTag("v10.12"));
        assertTrue(ReleaseAssets.stableTag("v0.12.1"));
        assertFalse(ReleaseAssets.stableTag("v0.12-overlay-layout"));
        assertFalse(ReleaseAssets.stableTag("0.6"));
        assertFalse(ReleaseAssets.stableTag(null));
    }
}
