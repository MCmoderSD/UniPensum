package de.mcmodersd.unipensum.domain.backup;

import static de.mcmodersd.unipensum.domain.backup.BackupInfo.Compatibility.*;

import java.time.Instant;

public record BackupInfo(int format, int schema, int appVersionCode, String appVersion, Instant exportedAt) {

    public enum Compatibility {
        SAME, OLDER, NEWER
    }

    public Compatibility compatibility(int currentFormat, int currentSchema, int currentAppVersionCode) {
        int byFormat = Integer.compare(format, currentFormat);
        if (byFormat != 0) return byFormat > 0 ? NEWER : OLDER;
        int bySchema = Integer.compare(schema, currentSchema);
        if (bySchema != 0) return bySchema > 0 ? NEWER : OLDER;
        int byBuild = Integer.compare(appVersionCode, currentAppVersionCode);
        if (byBuild != 0) return byBuild > 0 ? NEWER : OLDER;
        return Compatibility.SAME;
    }
}