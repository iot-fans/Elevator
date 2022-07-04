package run.aloop.elevator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import java.util.HashMap;

import Moka7.S7;
import Moka7.S7Client;


public class MainActivity extends AppCompatActivity implements DAQEvent, TextWatcher {

    private DAQ daq;
    private DataStruct[] ds = {
      new DataStruct(S7.S7AreaDB, 1, 0)
              .add(new DataStruct.Variable("current_floor", DataStruct.Variable.Type.SHORT))
              .add(new DataStruct.Variable("target_floor", DataStruct.Variable.Type.SHORT))
    };
    private TextView txtCurrentFloor, txtTargetFloor, txtErrorTip;
    private Button[] btnFloor;
    private View errContainer;
    private Handler handler = new Handler();

    public MainActivity() throws Exception {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        txtCurrentFloor = findViewById(R.id.current_floor);
        txtTargetFloor = findViewById(R.id.target_floor);
        txtErrorTip = findViewById(R.id.error_tip);
        errContainer = findViewById(R.id.error_container);

        daq = new DAQ(ds, this);
        LoadConfig();
        daq.start();

        LinearLayout layout = findViewById(R.id.floors);
        btnFloor = new Button[layout.getChildCount()];
        for (int i = 0; i < layout.getChildCount(); i++) {
            btnFloor[i] = (Button) layout.getChildAt(i);
            btnFloor[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v instanceof Button) {
                        int targetFloor = Integer.parseInt(((Button) v).getText().toString(), 10);
                        daq.write("target_floor", targetFloor);
                    }
                }
            });
        }
        txtCurrentFloor.addTextChangedListener(this);
        txtTargetFloor.addTextChangedListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivityForResult(intent, 0);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LoadConfig();
    }

    @Override
    protected void onDestroy() {
        daq.close();
        super.onDestroy();
    }

    private void LoadConfig() {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        String address = preference.getString("address", "");
        String mode = preference.getString("mode", "TSAP");
        String localTSAP = preference.getString("local_TSAP", "1000");
        String remoteTSAP = preference.getString("local_TSAP", "1000");
        String rack = preference.getString("rack", "0");
        String slot = preference.getString("slot", "0");
        daq.setParams(address, mode.equals("TSAP"), parseInt(localTSAP, 16, 0x1000),
                parseInt(remoteTSAP, 16, 0x1000), parseInt(rack, 10, 0),
                parseInt(slot, 10, 0));
    }

    private int parseInt(String v, int radix, int defaultValue) {
        try {
             return Integer.parseInt(v, radix);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "配置项 " + v + " 解析错误", Toast.LENGTH_LONG).show();
        }
        return defaultValue;
    }

    private void showError(String text) {
        if (daq.isConnected())
            return;
        errContainer.setVisibility(View.VISIBLE);
        txtErrorTip.setText(text);
        txtCurrentFloor.setText("-");
        txtTargetFloor.setText("-");
    }

    private void updateButtonState() {
        String curFloor = txtCurrentFloor.getText().toString();
        String targetFloor = txtTargetFloor.getText().toString();
        if (curFloor.equals("-") || targetFloor.equals("-") || curFloor.equals(targetFloor)) {
            for (Button btn : btnFloor) {
                btn.getBackground().clearColorFilter();
                btn.setTextColor(Color.BLACK);
            }
            return;
        }
        for (Button btn : btnFloor) {
            if (targetFloor.equals(btn.getText().toString())) {
                btn.getBackground().setColorFilter(0xFF0C84FF, PorterDuff.Mode.MULTIPLY);
                btn.setTextColor(Color.WHITE);
            } else {
                btn.getBackground().clearColorFilter();
                btn.setTextColor(Color.BLACK);
            }
        }
    }

    @Override
    public void OnConnectFailed(final int code) {
        System.out.println("Connect failed: " + S7Client.ErrorText(code));
        handler.post(new Runnable() {
            @Override
            public void run() {
                showError("Connect to " + daq.getAddress() +  " failed: " + S7Client.ErrorText(code));
            }
        });
    }

    @Override
    public void OnReadFailed(final int code) {
        System.out.println("Read failed: " + S7Client.ErrorText(code));
        handler.post(new Runnable() {
            @Override
            public void run() {
                showError("Read failed: "  + S7Client.ErrorText(code));
            }
        });
    }

    @Override
    public void OnConnected() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                errContainer.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void OnWriteFailed(final int code) {
        System.out.println("Write failed: " + S7Client.ErrorText(code));
        handler.post(new Runnable() {
            @Override
            public void run() {
                showError("Write failed: " + S7Client.ErrorText(code));
            }
        });
    }

    @Override
    public void OnDataReceived(final HashMap<String, Object> data) {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                Integer currentFloor = (Integer) data.get("current_floor");
                if (currentFloor != null)
                    txtCurrentFloor.setText(String.valueOf(currentFloor));
                Integer targetFloor = (Integer) data.get("target_floor");
                if (targetFloor != null)
                    txtTargetFloor.setText(String.valueOf(targetFloor));
            }
        });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        updateButtonState();
    }
}
