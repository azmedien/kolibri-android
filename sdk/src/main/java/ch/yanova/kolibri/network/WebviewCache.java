package ch.yanova.kolibri.network;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by lekov on 13.12.17.
 */

public class WebviewCache {

  private static String TAG = "WebviewCache";

  public static final long ONE_SECOND = 1000L;
  public static final long ONE_MINUTE = 60L * ONE_SECOND;
  public static final long ONE_HOUR = 60L * ONE_MINUTE;
  public static final long ONE_DAY = 24 * ONE_HOUR;
  public static final long ONE_WEEK = 7 * ONE_DAY;

  private static class CacheEntry {

    public String url;
    public String fileName;
    public String mimeType;
    public String encoding;
    public long maxAgeMillis;

    private CacheEntry(String url, String fileName,
        String mimeType, String encoding, long maxAgeMillis) {

      this.url = url;
      this.fileName = fileName;
      this.mimeType = mimeType;
      this.encoding = encoding;
      this.maxAgeMillis = maxAgeMillis;
    }

    private CacheEntry(String url, String mimeType) {
      this(url, md5(url), mimeType, "UTF-8", ONE_WEEK);
    }
  }

  private Map<String, CacheEntry> cacheEntries = new HashMap<>();
  private Context context = null;
  private File rootDir = null;


  public WebviewCache(Context context) {
    this.context = context;
    this.rootDir = context.getFilesDir();
  }

  public void register(String url, String mimeType, String encoding,
      long maxAgeMillis) {

    final CacheEntry entry = new CacheEntry(url, md5(url), mimeType, encoding,
        maxAgeMillis);

    this.cacheEntries.put(url, entry);
  }

  public void register(String url, String mimeType) {
    final CacheEntry entry = new CacheEntry(url, mimeType);

    this.cacheEntries.put(url, entry);
  }


  public WebResourceResponse load(String url) {
    if (!cacheEntries.containsKey(url)) {
      register(url, getMimeTypeFromUrl(url));
    }

    CacheEntry cacheEntry = this.cacheEntries.get(url);
    File cachedFile = new File(this.rootDir.getPath() + File.separator + cacheEntry.fileName);

    if (cachedFile.exists()) {
      long cacheEntryAge = System.currentTimeMillis() - cachedFile.lastModified();
      if (cacheEntryAge > cacheEntry.maxAgeMillis) {
        cachedFile.delete();

        //cached file deleted, call load() again.
        Log.d(TAG, "Deleting from cache: " + url);
        return load(url);
      }

      //cached file exists and is not too old. Return file.
      Log.d(TAG, "Loading from cache: " + url);
      try {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          int statusCode = 200;
          String reasonPhase = "OK";
          Map<String, String> responseHeaders = new HashMap<>();
          responseHeaders.put("Access-Control-Allow-Origin", "*");
          return new WebResourceResponse(cacheEntry.mimeType, cacheEntry.mimeType, statusCode,
              reasonPhase, responseHeaders, new FileInputStream(cachedFile));
        } else {
          return new WebResourceResponse(
              cacheEntry.mimeType, cacheEntry.encoding, new FileInputStream(cachedFile));
        }
      } catch (FileNotFoundException e) {
        Log.d(TAG, "Error loading cached file: " + cachedFile.getPath() + " : "
            + e.getMessage(), e);
      }

    } else {
      try {
        downloadAndStore(url, cacheEntry);
        //now the file exists in the cache, so we can just call this method again to read it.
        return load(url);
      } catch (Exception e) {
        Log.d(TAG, "Error reading file over network: " + cachedFile.getPath(), e);
      }
    }

    return null;
  }

  private void downloadAndStore(String url, CacheEntry cacheEntry)
      throws IOException {

    URL urlObj = new URL(url);
    URLConnection urlConnection = urlObj.openConnection();
    InputStream urlInput = urlConnection.getInputStream();

    FileOutputStream fileOutputStream =
        context.openFileOutput(cacheEntry.fileName, Context.MODE_PRIVATE);

    int data = urlInput.read();
    while (data != -1) {
      fileOutputStream.write(data);
      data = urlInput.read();
    }

    urlInput.close();
    fileOutputStream.flush();
    fileOutputStream.close();
    Log.d(TAG, "Cache file: " + cacheEntry.fileName + " stored. ");
  }

  public static String md5(final String s) {
    try {
      // Create MD5 Hash
      MessageDigest digest = MessageDigest
          .getInstance("MD5");
      digest.update(s.getBytes());
      byte messageDigest[] = digest.digest();

      // Create Hex String
      StringBuilder hexString = new StringBuilder();
      for (byte aMessageDigest : messageDigest) {
        StringBuilder h = new StringBuilder(Integer.toHexString(0xFF & aMessageDigest));
        while (h.length() < 2) {
          h.insert(0, "0");
        }
        hexString.append(h);
      }
      return hexString.toString();

    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return "";
  }

  public static String getMimeTypeFromUrl(String url) {
    return getMimeType(getExtFromUrl(url));
  }

  public static String getExtFromUrl(String url) {
    return getFileExt(getLocalFileNameForUrl(url));
  }

  public static String getLocalFileNameForUrl(String url) {
    String localFileName = "";
    String[] parts = url.split("/");
    if (parts.length > 0) {
      localFileName = parts[parts.length - 1];
    }
    return localFileName;
  }


  public static String getFileExt(String fileName) {
    return fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
  }

  public static String getMimeType(String fileExtension) {
    String mimeType = "";
    switch (fileExtension) {
      case "css":
        mimeType = "text/css";
        break;
      case "js":
        mimeType = "text/javascript";
        break;
      case "png":
        mimeType = "image/png";
        break;
      case "jpg":
        mimeType = "image/jpeg";
        break;
      case "ico":
        mimeType = "image/x-icon";
        break;
      case "woff":
      case "ttf":
      case "eot":
        mimeType = "application/x-font-opentype";
        break;
    }
    return mimeType;
  }
}
