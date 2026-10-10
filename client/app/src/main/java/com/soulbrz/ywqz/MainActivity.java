package com.soulbrz.ywqz;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private TextView label;
    private ImageButton btnMain;
    private String postResponse = "";

    private final Runnable enableMainButton = new Runnable() {
        @Override
        public void run() {
            btnMain.setEnabled(true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        label = (TextView) findViewById(R.id.tv_label);
        btnMain = (ImageButton) findViewById(R.id.btn_main);
        ImageButton btnSettings = (ImageButton) findViewById(R.id.btn_settings);

        btnMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPost();
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });

        applyCooldown();
    }

    @Override
    protected void onResume() {
        super.onResume();
        doGet();
    }

    @Override
    protected void onDestroy() {
        btnMain.removeCallbacks(enableMainButton);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }

    public String getPostResponse() {
        return postResponse;
    }

    private void applyCooldown() {
        long remain = Api.cooldownRemaining(this);
        btnMain.removeCallbacks(enableMainButton);
        if (remain > 0) {
            btnMain.setEnabled(false);
            btnMain.postDelayed(enableMainButton, remain);
        } else {
            btnMain.setEnabled(true);
        }
    }

    private void showCooldownTip() {
        long seconds = (Api.cooldownRemaining(this) + 999) / 1000;
        Toast.makeText(this, getString(R.string.cooldown_tip, seconds), Toast.LENGTH_SHORT).show();
    }

    private void doPost() {
        if (Api.isInCooldown(this)) {
            showCooldownTip();
            applyCooldown();
            return;
        }
        final String url = Api.buildUrl(this, Api.KEY_PATH_POST, Api.DEFAULT_PATH_POST);
        if (url == null) {
            return;
        }
        Api.markClicked(this);
        applyCooldown();
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    long timestamp = System.currentTimeMillis();
                    String body = "{\"timestamp\":" + timestamp + "}";
                    return Api.request(url, "POST", body);
                } catch (Exception e) {
                    return Api.errorMessage(e);
                }
            }

            @Override
            protected void onPostExecute(String result) {
                postResponse = result;
                label.setText(MainActivity.this.getCountFromResult(result));
                Api.setLastResponse(MainActivity.this, result);
            }
        }.execute();
    }

    private void doGet() {
        final String url = Api.buildUrl(this, Api.KEY_PATH_GET, Api.DEFAULT_PATH_GET);
        if (url == null) {
            label.setText("");
            return;
        }
        label.setText(R.string.loading);
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    return Api.request(url, "GET", null);
                } catch (Exception e) {
                    return Api.errorMessage(e);
                }
            }

            @Override
            protected void onPostExecute(String result) {
                label.setText(MainActivity.this.getCountFromResult(result));
            }
        }.execute();
    }

    private String getCountFromResult(String result) {
        String count = "";
        try {
            JSONObject jsonObject = new JSONObject(result);
            count = jsonObject.get("data").toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }
    private void showSettingsDialog() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        content.setPadding(pad, dp(8), pad, 0);

        final EditText inputUrl = addField(content, R.string.label_url, R.string.hint_url,
                Api.getBaseUrl(this));
        final EditText inputGet = addField(content, R.string.label_path_get, R.string.hint_path_get,
                Api.getPath(this, Api.KEY_PATH_GET, Api.DEFAULT_PATH_GET));
        final EditText inputPost = addField(content, R.string.label_path_post, R.string.hint_path_post,
                Api.getPath(this, Api.KEY_PATH_POST, Api.DEFAULT_PATH_POST));

        new AlertDialog.Builder(this)
                .setTitle(R.string.settings)
                .setView(content)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveSettings(inputUrl.getText().toString(),
                                inputGet.getText().toString(),
                                inputPost.getText().toString());
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private EditText addField(LinearLayout parent, int labelRes, int hintRes, String value) {
        TextView labelView = new TextView(this);
        labelView.setText(labelRes);

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setSingleLine(true);
        input.setHint(hintRes);
        input.setText(value);

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        parent.addView(labelView, params);
        parent.addView(input, params);
        return input;
    }

    private void saveSettings(String url, String pathGet, String pathPost) {
        Api.prefs(this).edit()
                .putString(Api.KEY_BASE_URL, url.trim())
                .putString(Api.KEY_PATH_GET, pathGet.trim())
                .putString(Api.KEY_PATH_POST, pathPost.trim())
                .apply();
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
        doGet();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }
}
