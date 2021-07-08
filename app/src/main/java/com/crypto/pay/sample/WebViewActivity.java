package com.crypto.pay.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

public class WebViewActivity extends AppCompatActivity {

    private EditText editTextUrl;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        editTextUrl = (EditText) findViewById(R.id.editTextUrl);
        webView = (WebView) findViewById(R.id.webView);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String paymentId = extras.getString("payment_id");
            String paymentUrl = extras.getString("payment_url");
            editTextUrl.setText(paymentUrl);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {

                    /*
                     * The WebView needs to capture and handle the following redirects:
                     * 1. if URLs contains "monaco://", it is a URL scheme to open Crypto.com App
                     * 2. if it is redirecting to `return_url`, then the Payment is succeeded
                     * 3. if it is redirection to `cancel_url`, then the Payment is cancelled or failed
                     * 4. for other page navigation, make sure it is done within the WebView instead of outside it
                     */
                    if (url.contains("monaco://")) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivity(intent);
                        } else {
                            Toast.makeText(webView.getContext(), R.string.no_apps_to_handle_intent, Toast.LENGTH_SHORT).show();
                        }

                    } else if (url.equals(CheckoutFragment.RETURN_URL)) {
                        Intent intent = new Intent();
                        intent.putExtra("payment_id", paymentId);
                        WebViewActivity.this.setResult(RESULT_OK, intent);
                        WebViewActivity.this.finish();
                    } else if (url.equals(CheckoutFragment.CANCEL_URL)) {
                        WebViewActivity.this.setResult(RESULT_CANCELED);
                        WebViewActivity.this.finish();
                    } else {
                        view.loadUrl(url);
                    }

                    editTextUrl.setText(url);
                    return true;
                }
            });

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                    String debugMessage = consoleMessage.message() + " -- From line "
                            + consoleMessage.lineNumber() + " of "
                            + consoleMessage.sourceId();

                    Toast.makeText(webView.getContext(), debugMessage, Toast.LENGTH_SHORT).show();
                    return super.onConsoleMessage(consoleMessage);
                }
            });

            /*
             * Enable JavaScript and load `payment_url` when creating the WebView.
             */
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.loadUrl(paymentUrl);
        }
    }
}