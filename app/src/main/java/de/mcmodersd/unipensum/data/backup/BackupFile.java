package de.mcmodersd.unipensum.data.backup;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.AEADBadTagException;

import de.mcmodersd.unipensum.domain.backup.BackupCleaner;
import de.mcmodersd.unipensum.domain.backup.BackupData;
import de.mcmodersd.unipensum.domain.backup.BackupInfo;

/**
 * The {@code .unipensum} file: a zip archive, packed with the strongest deflate level, so any zip tool
 * can look inside.
 * <pre>
 * manifest.json   always readable: format, database version, app version, date, and, for a protected
 *                 backup, how the key is made (iterations, salt, nonce)
 * data.json       the data, see {@link BackupJson}
 * data.enc        instead of data.json for a protected backup: the same JSON, deflated, then encrypted
 * </pre>
 * Nothing is ever unpacked to a file: the archive is read from memory, entry by entry, with a cap on
 * what it may unpack to, so a file that is made to explode is refused rather than filling the memory.
 * <p>
 * When the schema or the format changes, this class and {@link BackupJson} must keep reading the old
 * files (a test holds a fixed sample of the current form), because a backup is exactly the thing people
 * keep for years.
 */
public final class BackupFile {

    /** Layout of the file; raised only if the archive structure itself changes. */
    public static final int FORMAT = 1;
    /** Larger than any real backup by orders of magnitude; refused before anything is unpacked. */
    public static final long MAX_FILE_BYTES = 16L * 1024 * 1024;

    static final long MAX_UNPACKED_BYTES = 64L * 1024 * 1024;

    private static final String APP = "UniPensum";
    private static final String MANIFEST = "manifest.json";
    private static final String DATA_JSON = "data.json";
    private static final String DATA_ENC = "data.enc";
    private static final int MAX_ENTRIES = 16;

    private BackupFile() { }

    /** What the file says about the app that wrote it. */
    public record Meta(int schema, int appVersionCode, String appVersion, Instant exportedAt) {
    }

    // --- writing ---

    /**
     * @param password the password to protect the data with, {@code null} for none
     */
    public static byte[] write(BackupData data, Meta meta, char[] password) throws IOException, GeneralSecurityException {
        return write(data, meta, password, BackupCrypto.ITERATIONS);
    }

    static byte[] write(BackupData data, Meta meta, char[] password, int iterations)
            throws IOException, GeneralSecurityException {
        var json = new ByteArrayOutputStream();
        BackupJson.write(data, json);

        byte[] manifest;
        byte[] payload;
        String payloadName;
        if (password == null) {
            manifest = manifest(meta, null);
            payload = json.toByteArray();
            payloadName = DATA_JSON;
        } else {
            var params = BackupCrypto.newParams(iterations);
            // The manifest is fixed first because it is part of what the encryption protects.
            manifest = manifest(meta, params);
            payload = BackupCrypto.encrypt(password, params, manifest, deflate(json.toByteArray()));
            payloadName = DATA_ENC;
        }

        var archive = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(archive)) {
            zip.setLevel(Deflater.BEST_COMPRESSION);
            put(zip, MANIFEST, manifest);
            put(zip, payloadName, payload);
        }
        return archive.toByteArray();
    }

    private static void put(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }

    private static byte[] manifest(Meta meta, BackupCrypto.Params params) throws IOException {
        var bytes = new ByteArrayOutputStream();
        var json = new JsonWriter(new BufferedWriter(new OutputStreamWriter(bytes, StandardCharsets.UTF_8)));
        json.beginObject();
        json.name("app").value(APP);
        json.name("format").value(FORMAT);
        json.name("schema").value(meta.schema());
        json.name("appVersion").value(meta.appVersion());
        json.name("appVersionCode").value(meta.appVersionCode());
        if (meta.exportedAt() != null) {
            json.name("exportedAt").value(DateTimeFormatter.ISO_INSTANT.format(meta.exportedAt()));
        }
        if (params != null) {
            json.name("encryption").beginObject();
            json.name("cipher").value(BackupCrypto.CIPHER_NAME);
            json.name("kdf").value(BackupCrypto.KDF_NAME);
            json.name("iterations").value(params.iterations());
            json.name("salt").value(Base64.getEncoder().encodeToString(params.salt()));
            json.name("iv").value(Base64.getEncoder().encodeToString(params.iv()));
            json.endObject();
        }
        json.endObject();
        json.close();
        return bytes.toByteArray();
    }

    private static byte[] deflate(byte[] plain) throws IOException {
        var bytes = new ByteArrayOutputStream();
        var deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try (var out = new DeflaterOutputStream(bytes, deflater)) {
            out.write(plain);
        } finally {
            deflater.end();
        }
        return bytes.toByteArray();
    }

    // --- reading ---

    /** A file whose manifest has been read; the data is read, and decrypted if need be, by {@link #read}. */
    public static final class Opened {

        private final BackupInfo info;
        private final BackupCrypto.Params crypto;
        private final byte[] manifest;
        private final byte[] data;

        private Opened(BackupInfo info, BackupCrypto.Params crypto, byte[] manifest, byte[] data) {
            this.info = info;
            this.crypto = crypto;
            this.manifest = manifest;
            this.data = data;
        }

        public BackupInfo info() {
            return info;
        }

        public boolean isEncrypted() {
            return crypto != null;
        }

        /**
         * Decrypts (slow, so not on the main thread), reads and cleans the data.
         *
         * @param password the password, ignored if the backup is not protected
         * @throws BackupException {@code WRONG_PASSWORD} for a missing or wrong password or a file that was
         *                         changed afterwards, {@code DAMAGED} or {@code TOO_LARGE} for a broken one
         */
        public BackupCleaner.Result read(char[] password) throws BackupException {
            var json = data;
            if (crypto != null) {
                if (password == null || password.length == 0) throw new BackupException(BackupException.Reason.WRONG_PASSWORD);
                try {
                    json = inflate(BackupCrypto.decrypt(password, crypto, manifest, data));
                } catch (AEADBadTagException wrong) {
                    throw new BackupException(BackupException.Reason.WRONG_PASSWORD, wrong);
                } catch (GeneralSecurityException broken) {
                    throw new BackupException(BackupException.Reason.DAMAGED, broken);
                }
            }
            BackupJson.Parsed parsed;
            try {
                parsed = BackupJson.read(new ByteArrayInputStream(json));
            } catch (IOException | IllegalStateException | NumberFormatException broken) {
                throw new BackupException(BackupException.Reason.DAMAGED, broken);
            }
            return BackupCleaner.clean(parsed.data(), parsed.skipped(), parsed.adjusted());
        }
    }

    /**
     * Reads the archive and the manifest, nothing else.
     *
     * @throws BackupException {@code NOT_A_BACKUP} for any other file, {@code DAMAGED} for a truncated or
     *                         inconsistent one, {@code TOO_LARGE}, {@code UNSUPPORTED}
     */
    public static Opened open(byte[] bytes) throws BackupException {
        if (bytes.length > MAX_FILE_BYTES) throw new BackupException(BackupException.Reason.TOO_LARGE);
        // Every archive this class writes begins with the header of its first entry. Checking it here keeps
        // "some other file" apart from "a damaged backup", whatever the zip reader does with odd input.
        if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K' || bytes[2] != 3 || bytes[3] != 4) {
            throw new BackupException(BackupException.Reason.NOT_A_BACKUP);
        }

        var entries = unzip(bytes);
        var manifest = entries.get(MANIFEST);
        if (manifest == null) throw new BackupException(BackupException.Reason.NOT_A_BACKUP);

        var parsed = parseManifest(manifest);
        var data = entries.get(parsed.crypto == null ? DATA_JSON : DATA_ENC);
        if (data == null) throw new BackupException(BackupException.Reason.DAMAGED);
        return new Opened(parsed.info, parsed.crypto, manifest, data);
    }

    private static HashMap<String, byte[]> unzip(byte[] bytes) throws BackupException {
        var entries = new HashMap<String, byte[]>();
        var budget = MAX_UNPACKED_BYTES;
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            var count = 0;
            while ((entry = zip.getNextEntry()) != null) {
                if (++count > MAX_ENTRIES) throw new BackupException(BackupException.Reason.DAMAGED);
                // Every entry is read through, even the ones that are ignored, so all of them count to the cap.
                var content = readCapped(zip, budget);
                budget -= content.length;
                var name = entry.getName();
                if (!entry.isDirectory() && (name.equals(MANIFEST) || name.equals(DATA_JSON) || name.equals(DATA_ENC))) {
                    entries.put(name, content);
                }
            }
        } catch (IOException damaged) {
            throw new BackupException(BackupException.Reason.DAMAGED, damaged);
        }
        return entries;
    }

    private static byte[] inflate(byte[] packed) throws BackupException {
        try (var in = new InflaterInputStream(new ByteArrayInputStream(packed))) {
            return readCapped(in, MAX_UNPACKED_BYTES);
        } catch (IOException damaged) {
            throw new BackupException(BackupException.Reason.DAMAGED, damaged);
        }
    }

    /** Reads to the end, but gives up once more than {@code limit} bytes have come out. */
    private static byte[] readCapped(InputStream in, long limit) throws IOException, BackupException {
        var out = new ByteArrayOutputStream();
        var buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > limit) throw new BackupException(BackupException.Reason.TOO_LARGE);
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    // --- manifest ---

    private record ParsedManifest(BackupInfo info, BackupCrypto.Params crypto) {
    }

    private static ParsedManifest parseManifest(byte[] bytes) throws BackupException {
        String app = null;
        Integer format = null;
        Integer schema = null;
        var build = 0;
        var version = "";
        Instant exportedAt = null;
        BackupCrypto.Params crypto = null;

        var json = new JsonReader(new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8));
        try {
            json.beginObject();
            while (json.hasNext()) {
                switch (json.nextName()) {
                    case "app":
                        app = nextStringOrNull(json);
                        break;
                    case "format":
                        format = json.nextInt();
                        break;
                    case "schema":
                        schema = json.nextInt();
                        break;
                    case "appVersion":
                        version = orEmpty(nextStringOrNull(json));
                        break;
                    case "appVersionCode":
                        build = json.nextInt();
                        break;
                    case "exportedAt":
                        exportedAt = parseInstant(nextStringOrNull(json));
                        break;
                    case "encryption":
                        crypto = readEncryption(json);
                        break;
                    default:
                        json.skipValue();
                        break;
                }
            }
            json.endObject();
        } catch (IOException | IllegalStateException | NumberFormatException damaged) {
            throw new BackupException(BackupException.Reason.DAMAGED, damaged);
        }

        if (!APP.equals(app)) throw new BackupException(BackupException.Reason.NOT_A_BACKUP);
        if (format == null || schema == null) throw new BackupException(BackupException.Reason.DAMAGED);
        return new ParsedManifest(new BackupInfo(format, schema, build, version, exportedAt), crypto);
    }

    private static BackupCrypto.Params readEncryption(JsonReader json) throws IOException, BackupException {
        String cipher = null;
        String kdf = null;
        var iterations = 0;
        byte[] salt = null;
        byte[] iv = null;
        json.beginObject();
        while (json.hasNext()) {
            switch (json.nextName()) {
                case "cipher":
                    cipher = nextStringOrNull(json);
                    break;
                case "kdf":
                    kdf = nextStringOrNull(json);
                    break;
                case "iterations":
                    iterations = json.nextInt();
                    break;
                case "salt":
                    salt = decode(nextStringOrNull(json));
                    break;
                case "iv":
                    iv = decode(nextStringOrNull(json));
                    break;
                default:
                    json.skipValue();
                    break;
            }
        }
        json.endObject();

        // A newer version may protect the data in a way this one cannot undo.
        if (!BackupCrypto.CIPHER_NAME.equals(cipher) || !BackupCrypto.KDF_NAME.equals(kdf)) {
            throw new BackupException(BackupException.Reason.UNSUPPORTED);
        }
        var sane = iterations >= BackupCrypto.MIN_ITERATIONS && iterations <= BackupCrypto.MAX_ITERATIONS
                && salt != null && salt.length == BackupCrypto.SALT_BYTES
                && iv != null && iv.length == BackupCrypto.IV_BYTES;
        if (!sane) throw new BackupException(BackupException.Reason.DAMAGED);
        return new BackupCrypto.Params(iterations, salt, iv);
    }

    private static String nextStringOrNull(JsonReader json) throws IOException {
        if (json.peek() == JsonToken.STRING) return json.nextString();
        json.skipValue();
        return null;
    }

    private static byte[] decode(String base64) {
        if (base64 == null) return null;
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException notBase64) {
            return null;
        }
    }

    private static Instant parseInstant(String text) {
        if (text == null) return null;
        try {
            return Instant.parse(text);
        } catch (RuntimeException unreadable) {
            return null;
        }
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}