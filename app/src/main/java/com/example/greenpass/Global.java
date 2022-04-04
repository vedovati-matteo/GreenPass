package com.example.greenpass;

import android.util.Log;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Global {
    // name data in SharedPreferences
    public static String PREFS_NAME = "userData";
    public static String QR_LIST = "qrId";
    public static String WIDGET_ID = "widget";

    // settings SharedPreferences
    public static String S_KEY_validity = "validity";
    public static String S_KEY_test = "test_val";
    public static String S_KEY_2dose = "dose_val";
    public static String S_KEY_resetBtn = "defaultBtn";

    // code for selecting image
    public static final int SELECT_IMAGE = 1;
    public static final int SELECT_PDF = 1212;

    public static final int PDF_WIDTH = 2024*2;

    // buffer size
    public static final int BUFFER_SIZE = 1024;

    // duration 2nd dose
    public static final int DURATION_DOSE = 6;
    // duration test
    public static final int DURATION_TEST = 3;

    // function that return the expiration date giving starting date and number of month/day
    public static String getExpireDate(String date, int num, boolean month) {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                LocalDate d = LocalDate.parse(date, formatter);
                LocalDate expDate;


                if (month) {
                    expDate = d.plusMonths(num);
                } else {
                    expDate = d.plusDays(num);
                }
                return expDate.format(formatter);
            } catch (Exception e) {
                Log.e("------QrTest", e.toString());
            }
        }
        return "";
    }

    // check if a date has already passed
    public static boolean isExpired(String date) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate expDate = LocalDate.parse(date, formatter);
            return expDate.isBefore(LocalDate.now());
        }
        return false;
    }

}
