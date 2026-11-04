import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.zip.*;
import javax.net.ssl.HttpsURLConnection;

public class GradleBootstrap {
    private static final String DOWNLOAD_URL =
        "https://downloads.gradle.org/distributions/gradle-8.12.1-bin.zip";
    private static final String ZIP_NAME = "gradle-8.12.1-bin.zip";

    public static void main(String[] args) {
        try {
            Path zip = Paths.get(ZIP_NAME);
            if (!Files.exists(zip) || Files.size(zip) < 1000000L) {
                System.out.println("Downloading official Gradle 8.12.1...");
                download(zip);
            } else {
                System.out.println("Gradle archive already downloaded.");
            }
            System.out.println("Extracting Gradle...");
            extract(zip, Paths.get(".").toAbsolutePath().normalize());
            if (!Files.exists(Paths.get("gradle-8.12.1", "bin", "gradle.bat")))
                throw new IOException("gradle.bat missing after extraction");
            System.out.println("Gradle 8.12.1 is ready.");
        } catch (Exception e) {
            System.err.println("GRADLE DOWNLOAD/EXTRACT FAILED: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void download(Path target) throws Exception {
        URL url = new URL(DOWNLOAD_URL);
        HttpsURLConnection c = (HttpsURLConnection) url.openConnection();
        c.setInstanceFollowRedirects(true);
        c.setConnectTimeout(30000);
        c.setReadTimeout(60000);
        c.setRequestProperty("User-Agent", "RegionHelper-Win7-Gradle-Bootstrap/1.0");
        try {
            int code = c.getResponseCode();
            if (code < 200 || code >= 300)
                throw new IOException("HTTP " + code + " at " + c.getURL());
            long total = c.getContentLengthLong();
            try (InputStream in = c.getInputStream();
                 OutputStream out = new BufferedOutputStream(Files.newOutputStream(target))) {
                byte[] buffer = new byte[65536];
                long count = 0;
                int n;
                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                    count += n;
                    if (total > 0 && count % (5L * 1024 * 1024) < buffer.length)
                        System.out.println("Downloaded " + (count / 1048576) + " / " +
                            (total / 1048576) + " MB");
                }
            }
            if (Files.size(target) < 1000000L)
                throw new IOException("Downloaded archive is too small");
        } finally {
            c.disconnect();
        }
    }

    private static void extract(Path zip, Path dest) throws Exception {
        String root = dest.toString() + File.separator;
        try (ZipInputStream in = new ZipInputStream(
                new BufferedInputStream(Files.newInputStream(zip)))) {
            ZipEntry entry;
            byte[] buffer = new byte[65536];
            while ((entry = in.getNextEntry()) != null) {
                Path out = dest.resolve(entry.getName()).normalize();
                if (!out.toString().startsWith(root))
                    throw new IOException("Unsafe ZIP entry: " + entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Path parent = out.getParent();
                    if (parent != null) Files.createDirectories(parent);
                    try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(out))) {
                        int n;
                        while ((n = in.read(buffer)) != -1) os.write(buffer, 0, n);
                    }
                }
                in.closeEntry();
            }
        }
    }
}
