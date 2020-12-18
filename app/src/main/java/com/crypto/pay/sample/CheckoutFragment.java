package com.crypto.pay.sample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class CheckoutFragment extends Fragment {

    /*
     * Dummy URLs for return and cancel, these will be provided when creating the Payment.
     * Once payment is finished on Crypto.com Pay payment page, it will redirect.
     * The WebView will need to capture these redirects, refer to WebViewActivity class for more info.
     */
    public static final String RETURN_URL = "https://return.pay.crypto.dummy/";
    public static final String CANCEL_URL = "https://cancel.pay.crypto.dummy/";
    public static final int REQUEST_CODE = 0;

    private Context context;
    private Button buttonPay;

    private String payServerHost;
    private String secretKey;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_checkout, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        context = getContext();
        buttonPay = view.findViewById(R.id.button_pay);

        buttonPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonPay.setEnabled(false);

                try {

                    /*
                     * Create the Payment using Crypto.com Pay API.
                     * Refer to https://pay-docs.crypto.com/ for API documentation.
                     * In real world App implementation, the App should be sending a checkout to merchant server,
                     * and then merchant server creates the Payment (and only server holds the Crypto.com API secret key).
                     */
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                    payServerHost = sharedPreferences.getString("perf_pay_server_host", "");
                    URI payServerPaymentUri = new URI("https", payServerHost, "/api/payments", null);
                    secretKey = sharedPreferences.getString("perf_secret_key", "");

                    JSONObject json = new JSONObject();
                    json.put("amount", 5000); // amount in cents
                    json.put("currency", "USD");
                    json.put("description", "Crypto.com Hoodie (Unisex)");
                    json.put("return_url", CheckoutFragment.RETURN_URL);
                    json.put("cancel_url", CheckoutFragment.CANCEL_URL);
                    json.put("order_id", "sample_order_id"); // order id on merchant side, for reference

                    JSONObject metadata = new JSONObject(); // extra info for merchant reference, can be any string
                    metadata.put("size", "M");
                    metadata.put("color", "blue");
                    metadata.put("customer_name", "John Doe");
                    metadata.put("plugin_name", "Android App Sample");

                    json.put("metadata", metadata);

                    RequestQueue queue = Volley.newRequestQueue(context);
                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, payServerPaymentUri.toString(), json, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {

                                /*
                                 * When the Payment is created, open a WebView to display the `payment_url`.
                                 * Wait for the redirection to happen.
                                 */
                                Intent webViewIntent = new Intent(context, WebViewActivity.class);
                                webViewIntent.putExtra("payment_url", response.getString("payment_url"));
                                webViewIntent.putExtra("payment_id", response.getString("id"));
                                startActivityForResult(webViewIntent, REQUEST_CODE);

                            } catch (JSONException e) {
                                showToastMessage(e.getMessage());
                            }
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError e) {
                            showToastMessage(e.getMessage());
                        }
                    }) {

                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {

                            /*
                             * In real world App implementation, the App should be sending a checkout to merchant server,
                             * and then merchant server creates the Payment (and only server holds the Crypto.com API secret key).
                             */
                            Map<String, String> headers = new HashMap<String, String>();
                            headers.put("Authorization", "Bearer " + secretKey);
                            return headers;
                        }
                    };

                    queue.add(jsonObjectRequest);

                } catch (JSONException | URISyntaxException e) {
                    showToastMessage(e.getMessage());
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK && data != null ) {

            try {
                Bundle extras = data.getExtras();
                if (extras != null) {

                    /*
                     * WebViewActivity detects the redirects and then finish the Activity, returning to this one.
                     * The App should then inform merchant server to completes the order.
                     * In merchant server, it should get the Payment from Crypto.com Pay API again,
                     * and confirm the `status` is `succeeded`.
                     * If so, server can finish the order and the App can show an order completion message.
                     */
                    String paymentId = extras.getString("payment_id");
                    URI payServerPaymentUri = new URI("https", payServerHost, "/api/payments/" + paymentId, null);

                    RequestQueue queue = Volley.newRequestQueue(context);
                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, payServerPaymentUri.toString(), null, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                String paymentStatus = response.getString("status"); // Getting `status` of Payment
                                showToastMessage(String.format("Payment %1$s is %2$s", paymentId, paymentStatus));

                            } catch (JSONException e) {
                                showToastMessage(e.getMessage());
                            }
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError e) {
                            showToastMessage(e.getMessage());
                        }
                    }) {

                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {

                            /*
                             * In real world App implementation, only server holds the Crypto.com API secret key.
                             * The merchant server will be response to check the `status` of Payment.
                             */
                            Map<String, String> headers = new HashMap<String, String>();
                            headers.put("Authorization", "Bearer " + secretKey);
                            return headers;
                        }
                    };

                    queue.add(jsonObjectRequest);
                }
            } catch (URISyntaxException e) {
                showToastMessage(e.getMessage());
            }
        }

        buttonPay.setEnabled(true);
    }

    private void showToastMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}