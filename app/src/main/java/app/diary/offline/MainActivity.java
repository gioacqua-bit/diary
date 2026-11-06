package app.diary.offline;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.AtomicFile;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Hosts the diary page (assets/index.html) and gives it three things the page
 * cannot do alone: keep entries in a file in the app's private storage, save
 * exports to a location the user picks, and read a backup file for restore.
 * No network access is requested or used.
 */
public class MainActivity extends Activity {

    private static final int REQ_EXPORT = 41;
    private static final int REQ_IMPORT = 42;

    private WebView web;
    private AtomicFile store;
    private String pendingExport;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new AtomicFile(new File(getFilesDir(), "entries.json"));

        web = new WebView(this);
        web.setBackgroundColor(getColor(R.color.paper));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setBlockNetworkLoads(true);
        web.setWebChromeClient(new WebChromeClient());   // enables confirm() dialogs
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return true;                              // never navigate away
            }
        });
        web.addJavascriptInterface(new Bridge(), "AndroidDiary");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onPause() {
        // Write any text typed in the last moments before the app goes to background.
        if (web != null) web.evaluateJavascript("window.flushNow && window.flushNow()", null);
        super.onPause();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.handleBack ? window.handleBack() : false", result -> {
            if (!"true".equals(result)) finish();
        });
    }

    private void callJs(String fn, String arg) {
        runOnUiThread(() -> web.evaluateJavascript(
                "window." + fn + " && window." + fn + "(" + JSONObject.quote(arg) + ")", null));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri = (resultCode == RESULT_OK && data != null) ? data.getData() : null;

        if (requestCode == REQ_EXPORT) {
            String content = pendingExport;
            pendingExport = null;
            if (uri == null || content == null) { callJs("onExportResult", "Export cancelled."); return; }
            try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                out.write(content.getBytes(StandardCharsets.UTF_8));
                callJs("onExportResult", "Exported.");
            } catch (Exception e) {
                callJs("onExportResult", "Export failed: " + e.getMessage());
            }
        } else if (requestCode == REQ_IMPORT) {
            if (uri == null) { callJs("onImportCancelled", ""); return; }
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] b = new byte[8192];
                int n;
                while ((n = in.read(b)) > 0) buf.write(b, 0, n);
                callJs("onImportText", buf.toString("UTF-8"));
            } catch (Exception e) {
                callJs("onExportResult", "Could not read that file.");
            }
        }
    }

    /** Methods callable from the page as window.AndroidDiary.*  */
    private class Bridge {

        @JavascriptInterface
        public String load() {
            synchronized (MainActivity.this) {
                try {
                    return new String(store.readFully(), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return "";                            // first run: no file yet
                }
            }
        }

        @JavascriptInterface
        public boolean save(String json) {
            synchronized (MainActivity.this) {
                FileOutputStream out = null;
                try {
                    out = store.startWrite();
                    out.write(json.getBytes(StandardCharsets.UTF_8));
                    store.finishWrite(out);                // atomic replace
                    return true;
                } catch (Exception e) {
                    if (out != null) store.failWrite(out);
                    return false;
                }
            }
        }

        @JavascriptInterface
        public void exportFile(String filename, String mime, String content) {
            runOnUiThread(() -> {
                pendingExport = content;
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType(mime);
                i.putExtra(Intent.EXTRA_TITLE, filename);
                startActivityForResult(i, REQ_EXPORT);
            });
        }

        @JavascriptInterface
        public void importFile() {
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                startActivityForResult(i, REQ_IMPORT);
            });
        }
    }
}
