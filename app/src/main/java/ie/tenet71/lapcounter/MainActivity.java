package ie.tenet71.lapcounter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;

import java.util.Locale;

/**
 * Shows the lap counter page full screen and gives it native vibration,
 * spoken feedback and keep-screen-on through the "LapNative" bridge.
 */
public class MainActivity extends Activity {

    private WebView webView;
    private TextToSpeech tts;
    private volatile boolean ttsReady = false;
    private volatile boolean ttsStarting = false;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initTts();

        webView = new WebView(this);
        webView.setBackgroundColor(0xFF0E0F11);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setTextZoom(100);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new Bridge(), "LapNative");
        setContentView(webView);

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl("file:///android_asset/index.html");
        }
        goFullScreen();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) goFullScreen();
    }

    @SuppressWarnings("deprecation")
    private void goFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        webView.evaluateJavascript("(window.lcBack && window.lcBack()) ? 'handled' : 'exit'", result -> {
            if (result == null || !result.contains("handled")) {
                MainActivity.super.onBackPressed();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        webView.destroy();
        super.onDestroy();
    }

    /** Starts the text-to-speech engine. Called again later if the first start failed. */
    private void initTts() {
        if (tts != null) {
            try { tts.shutdown(); } catch (Exception ignored) { }
        }
        ttsReady = false;
        ttsStarting = true;
        tts = new TextToSpeech(this, status -> {
            ttsStarting = false;
            if (status == TextToSpeech.SUCCESS) {
                int r = tts.setLanguage(Locale.UK);
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault());
                }
                ttsReady = true;
            }
        });
    }

    /**
     * Plays a vibration tagged as an alarm. Untagged app vibrations are treated as "media"
     * vibrations on newer Android/Samsung phones, which are often switched off or muted in
     * silent/vibrate modes; alarm vibrations are not.
     */
    @SuppressWarnings("deprecation")
    private void playVibration(Vibrator v, VibrationEffect effect) {
        if (Build.VERSION.SDK_INT >= 33) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM));
        } else {
            v.vibrate(effect, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
        }
    }

    @SuppressWarnings("deprecation")
    private Vibrator vibrator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vm != null ? vm.getDefaultVibrator() : null;
        }
        return (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    /** Methods the page can call as window.LapNative.*. */
    private class Bridge {

        /** Pattern as a JSON array of milliseconds: [on, off, on, ...]. */
        @JavascriptInterface
        public void vibrate(String patternJson) {
            try {
                JSONArray a = new JSONArray(patternJson);
                if (a.length() == 0) return;
                Vibrator v = vibrator();
                if (v == null || !v.hasVibrator()) return;
                if (a.length() == 1) {
                    playVibration(v, VibrationEffect.createOneShot(Math.max(1, a.getLong(0)), VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    long[] timings = new long[a.length() + 1];
                    timings[0] = 0;
                    for (int i = 0; i < a.length(); i++) timings[i + 1] = a.getLong(i);
                    playVibration(v, VibrationEffect.createWaveform(timings, -1));
                }
            } catch (Exception ignored) {
            }
        }

        @JavascriptInterface
        public void speak(String text) {
            if (text == null) return;
            if (tts != null && ttsReady) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lap");
            } else if (!ttsStarting) {
                // The voice engine wasn't ready (e.g. it failed to start with the app): try again.
                runOnUiThread(MainActivity.this::initTts);
            }
        }

        @JavascriptInterface
        public void keepScreenOn(boolean on) {
            runOnUiThread(() -> {
                if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        }

        /** Closes the app and removes it from the recent apps list. */
        @JavascriptInterface
        public void exitApp() {
            runOnUiThread(MainActivity.this::finishAndRemoveTask);
        }
    }
}
