package com.pr0gramm.app.services.config;


import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.util.Arrays;
import java.util.List;

/**
 */
@Value.Immutable
@Gson.TypeAdapters
public abstract class Config {
    @Value.Default
    public boolean getExtraCategories() {
        return true;
    }

    @Value.Default
    public long getMaxUploadSizeNormal() {
        return 6 * 1024 * 1024;
    }

    @Value.Default
    public long getMaxUploadSizePremium() {
        return 12 * 1024 * 1024;
    }

    @Value.Default
    public boolean getSearchUsingTagService() {
        return false;
    }

    @Value.Default
    public boolean getSecretSanta() {
        return false;
    }

    @Value.Default
    public AdType getAdType() {
        return AdType.NONE;
    }

    @Value.Default
    public boolean getTrackItemView() {
        return false;
    }

    @Value.Default
    public List<String> getQuestionableTags() {
        return Arrays.asList(
                "0815", "kann weg", "heil hitler", "ban pls", "deshalb",
                "ab ins gas", "und weiter", "alles ist", "hure", "da drückste",
                "pr0paganda", "pr0gida");
    }

    public enum AdType {
        NONE,
        FEED,
        MAIN /* deprecated - dont use */;
    }
}
