package de.kopfschmerztagebuch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class MainActivity extends Activity {

    private WebView webView;
    private WebView printView; // Referenz halten, solange der Druckauftrag laeuft

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        webView.addJavascriptInterface(new Bridge(), "Android");
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        // Zuerst die Web-Oberflaeche zurueck navigieren lassen (Formular schliessen etc.)
        webView.evaluateJavascript("window.handleBack ? window.handleBack() : false",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        if (!"true".equals(value)) {
                            finish();
                        }
                    }
                });
    }

    private File dataFile() {
        return new File(getFilesDir(), "tagebuch.json");
    }

    public class Bridge {

        @JavascriptInterface
        public String loadData() {
            try {
                File f = dataFile();
                if (!f.exists()) return "";
                FileInputStream in = new FileInputStream(f);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                in.close();
                return out.toString("UTF-8");
            } catch (Exception e) {
                return "";
            }
        }

        @JavascriptInterface
        public boolean saveData(String json) {
            try {
                File tmp = new File(getFilesDir(), "tagebuch.json.tmp");
                FileOutputStream out = new FileOutputStream(tmp);
                out.write(json.getBytes("UTF-8"));
                out.getFD().sync();
                out.close();
                File dest = dataFile();
                if (dest.exists() && !dest.delete()) return false;
                return tmp.renameTo(dest);
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public void shareText(final String subject, final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_SUBJECT, subject);
                    i.putExtra(Intent.EXTRA_TEXT, text);
                    startActivity(Intent.createChooser(i, subject));
                }
            });
        }

        @JavascriptInterface
        public void printReport(final String html, final String jobName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final WebView pw = new WebView(MainActivity.this);
                    pw.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                            PrintDocumentAdapter adapter = view.createPrintDocumentAdapter(jobName);
                            PrintAttributes attrs = new PrintAttributes.Builder()
                                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                    .setMinMargins(new PrintAttributes.Margins(590, 590, 590, 590))
                                    .build();
                            pm.print(jobName, adapter, attrs);
                        }
                    });
                    printView = pw;
                    pw.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
                }
            });
        }
    }
}
