package com.example.greenpass;

import static com.example.greenpass.Global.PREFS_NAME;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class GreenPassActivity extends AppCompatActivity {

    String id;
    String imagePath;
    SharedPreferences usersData;
    SharedPreferences spSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_green_pass);

        spSettings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        usersData = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);

        try {
            id = getIntent().getStringExtra("id");
            updateViewsFromIdentifier(id);
        } catch (Exception e) {
            Log.e("------QrTest", e.toString());
            // if cannot display properly the Green Pass, return to the Main Activity
            Intent i=new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }

    }

    public void updateViewsFromIdentifier (String identifier) throws Exception {
        // get if we need to calculate the validity
        boolean validity = spSettings.getBoolean(Global.S_KEY_validity, true);

        // get data list: name[0];type[1];date[2];(nDose/time/dateUntil)[3];filePath[4]
        String data = usersData.getString(identifier, "error");
        List<String> dataList = Arrays.asList(data.split(";"));
        if (dataList.get(0).equals("error")) {
            throw new Exception("ID doesn't have values");
        }
        // output text
        TextView name = (TextView) findViewById(R.id.nameText);
        TextView text1 = (TextView) findViewById(R.id.text1);
        TextView text2 = (TextView) findViewById(R.id.text2);
        TextView text3 = (TextView) findViewById(R.id.text3);
        TextView text4 = (TextView) findViewById(R.id.text4);

        ConstraintLayout layout = (ConstraintLayout) findViewById(R.id.qrcodeLayout);

        name.setText(dataList.get(0)); // name

        String supp;
        switch (dataList.get(1)) {
            case "v": // VACCINE
                text1.setText("Vaccination date: " + dataList.get(2)); // vaccination date

                // if needed calculate the validity
                if (validity) {
                    supp = "";
                    if (dataList.get(3).equals("1")) {
                        supp = "Valid until 2nd Dose";
                    } else if (dataList.get(3).equals("2")) {
                        int duration = Integer.parseInt(spSettings.getString(Global.S_KEY_2dose, Integer.toString(Global.DURATION_DOSE)));
                        String expDate = Global.getExpireDate(dataList.get(2), duration, true);
                        if (Global.isExpired(expDate)) {
                            layout.setBackgroundColor(getResources().getColor(R.color.red));
                            Toast toast = Toast.makeText(this, "Green Pass Expired", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                        supp = "Valid until: " + expDate;
                    }
                    text2.setText(supp);
                }

                // number of dose
                supp = "";
                if (dataList.get(3).equals("1")) {
                    supp = "st";
                } else if (dataList.get(3).equals("2")) {
                    supp = "nd";
                } else if (dataList.get(3).equals("3")) {
                    supp = "rd";
                }
                text3.setText(dataList.get(3) + supp + " dose");
                text3.setTypeface(null, Typeface.BOLD);

                break;
            case "t": // TEST
                text1.setText("Test date: " + dataList.get(3)); // test date

                // if needed calculate the validity
                if (validity) {
                    int duration;
                    if (dataList.get(2).equals("r")) { // rapid test
                        duration = Integer.parseInt(spSettings.getString(Global.S_KEY_test_rapid, Integer.toString(Global.DURATION_TEST_RAPID)));
                        text4.setText("Rapid Test");
                    } else { // molecular test
                        duration = Integer.parseInt(spSettings.getString(Global.S_KEY_test_molecular, Integer.toString(Global.DURATION_TEST_MOLECULAR)));
                        text4.setText("Molecular Test");
                    }
                    String expDate = Global.getExpireDate(dataList.get(3), duration, false);
                    if (Global.isExpired(expDate)) {
                        layout.setBackgroundColor(getResources().getColor(R.color.red));
                        Toast toast = Toast.makeText(this, "Green Pass Expired", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    text2.setText("Valid until: " + expDate);
                }

                text3.setText("Test time: " + dataList.get(4)); // test time

                break;
            case "r": // RECOVERY
                text2.setText("Recovery date: " + dataList.get(2)); // recovery date

                if (Global.isExpired(dataList.get(3))) { // if expired
                    layout.setBackgroundColor(getResources().getColor(R.color.red));
                    Toast toast = Toast.makeText(this, "Green Pass Expired", Toast.LENGTH_SHORT);
                    toast.show();
                }

                text3.setText("Valid until: " + dataList.get(3)); // validity

                break;
            default: // if cannot recognize the type retun to the Main Activity
                throw new Exception("Green Pass type not recognized");

        }

        // output image
        ImageView qrCode = (ImageView) findViewById(R.id.qrCodeView);

        imagePath = dataList.get(dataList.size() - 1);

        Glide.with(this)
                .load(imagePath)
                .transform(new RoundedCornersTransformation(16,0))
                .into(qrCode);
    }

    public void zoomIn (View view) {
        Intent i = new Intent(GreenPassActivity.this, ZoomedQrCodeActivity.class);
        i.putExtra("imagePath", imagePath);
        startActivity(i);
    }

    public void delete (View view) {
        // remove from qrcode id list
        SharedPreferences.Editor editor = usersData.edit();
        String prevList = usersData.getString(Global.QR_LIST, "");
        String newList = "";

        // update QR LIST
        List<String> qrList = Arrays.asList(prevList.split(";"));
        boolean first = true;
        for (String qrId : qrList) {
            if (!qrId.equals(id)) {
                if (first) {
                    newList += qrId;
                    first = false;
                } else {
                    newList += ";" + qrId;
                }

            }
        }
        editor.putString(Global.QR_LIST, newList);
        editor.apply();

        // remove memory string of the Green Pass
        editor.remove(id);
        editor.apply();

        // delete image file from internal storage
        File img = new File(imagePath);
        img.delete();

        // return to main activity
        finish();
    }
}