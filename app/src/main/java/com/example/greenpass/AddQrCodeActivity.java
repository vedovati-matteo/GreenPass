package com.example.greenpass;

import static com.example.greenpass.Global.BUFFER_SIZE;
import static com.example.greenpass.Global.PREFS_NAME;
import static com.example.greenpass.Global.SELECT_IMAGE;
import static com.example.greenpass.Global.SELECT_PDF;
import static com.example.greenpass.Global.PDF_WIDTH;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.Inflater;

import nl.minvws.encoding.Base45;

import COSE.Encrypt0Message;
import COSE.Message;
import com.google.iot.cbor.CborMap;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONObject;


public class AddQrCodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_qr_code);
    }

    // pressed IMAGE button
    public void getImage(View view) {
        Intent chooseFile = new Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(chooseFile, SELECT_IMAGE);
    }

    // pressed PDF button
    public void getPdf(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        startActivityForResult(intent, SELECT_PDF);
    }

    // pressed CAMERA button
    public void getCamera(View view) {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan the Green Pass");
        integrator.setCaptureActivity(CaptureActivityPortrait.class);
        integrator.initiateScan();
    }

    // when file is selected
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri fileUri;

        try {
            switch (requestCode) {
                case SELECT_IMAGE:
                    // IMAGE
                    if (resultCode == -1) {
                        fileUri = data.getData();
                        // read qrcode
                        InputStream is = new BufferedInputStream(getContentResolver().openInputStream(fileUri));
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        String content = scanQRImage(bitmap);


                        // extract and store data
                        getDataFromQrcode(content, bitmap);
                    }
                    break;
                case SELECT_PDF:
                    // PDF
                    if (resultCode == -1) {
                        fileUri = data.getData();

                        // Create the page renderer
                        ParcelFileDescriptor descriptor = getContentResolver().openFileDescriptor(fileUri, "r", null);
                        PdfRenderer pdfRenderer = new PdfRenderer(descriptor);

                        // Open first page of pdf
                        PdfRenderer.Page page = pdfRenderer.openPage(0);

                        // Render the page to bitmap
                        Bitmap pageBitmap = Bitmap.createBitmap(PDF_WIDTH, (int) (PDF_WIDTH / page.getWidth() * page.getHeight()), Bitmap.Config.ARGB_8888);
                        page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                        // Scan bitmap for qrcode
                        String content = scanQRImage(pageBitmap);

                        // recreate bitmap from decoded string
                        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
                        BitMatrix bitMatrix;
                        Map<EncodeHintType, Object> hintMap = new HashMap<EncodeHintType, Object>();
                        hintMap.put(EncodeHintType.MARGIN, new Integer(1));
                        bitMatrix = multiFormatWriter.encode(content, BarcodeFormat.QR_CODE,500,500, hintMap);
                        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                        Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
                        // extract and store data
                        getDataFromQrcode(content, bitmap);
                    }
                    break;
                case IntentIntegrator.REQUEST_CODE:
                    // CAMERA
                    IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                    if (scanResult != null) {
                        // handle scan result
                        String content = scanResult.getContents();

                        // create bitmap from decoded string
                        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
                        BitMatrix bitMatrix;
                        Map<EncodeHintType, Object> hintMap = new HashMap<EncodeHintType, Object>();
                        hintMap.put(EncodeHintType.MARGIN, new Integer(1));
                        if (scanResult.getFormatName().equals("QR_CODE")) {
                            bitMatrix = multiFormatWriter.encode(scanResult.getContents(), BarcodeFormat.QR_CODE,500,500, hintMap);
                        } else {
                            throw new Exception("Camera didn't scan a QRCODE");
                        }
                        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                        Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
                        // extract and store data
                        getDataFromQrcode(content, bitmap);
                    }
            }
        } catch (Exception e) {
            Log.e("------QrTest", e.toString());
        }

        // return to main activity
        finish();
    }

    // scan image for qrcode
    public static String scanQRImage(Bitmap bMap) throws ChecksumException, NotFoundException, FormatException {
        String contents = null;

        int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Reader reader = new MultiFormatReader();

        Result result = reader.decode(bitmap);
        contents = result.getText();

        return contents;
    }

    private void getDataFromQrcode(String content, Bitmap bitmap) throws Exception {
        // decode qrcode
        // remove prefix
        String withoutPrefix = content.substring(4);
        // decode base45
        byte[] bytecompressed = Base45.getDecoder().decode(withoutPrefix);

        // decompress String
        Inflater inflater = new Inflater();
        inflater.setInput(bytecompressed);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(bytecompressed.length);
        byte[] buffer = new byte[BUFFER_SIZE];
        while (!inflater.finished()) {
            final int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }

        // Decode COSE message and create CborObject MAP
        Message a = Encrypt0Message.DecodeFromBytes(outputStream.toByteArray());
        CborMap cborMap = CborMap.createFromCborByteArray(a.GetContent());

        // create JsonObject
        JSONObject jsonData = new JSONObject(cborMap.toJsonString());
        jsonData = jsonData.getJSONObject("-260").getJSONObject("1");

        // __GET attributes needed
        // get name
        String name = jsonData.getJSONObject("nam").getString("fn") + " " + jsonData.getJSONObject("nam").getString("gn");

        String type, identifier, storeString = name;

        if (jsonData.has("v")) {
            // vaccinated
            type = "v";
            JSONObject v = jsonData.getJSONArray("v").getJSONObject(0);
            // get vaccination date
            String dateVac = LocalDate.parse(v.getString("dt")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            // get dose number
            String nDose = v.getString("sd");
            // get unique identifier
            identifier = v.getString("ci");

            storeString += ";" + type + ";" + dateVac + ";" + nDose;

        } else if (jsonData.has("t")) {
            // tested
            type = "t";
            JSONObject t = jsonData.getJSONArray("t").getJSONObject(0);
            // get type of test
            String testType = "";
            if (t.has("ma") && !t.getString("ma").equals("")) { // is a rapid test
                testType = "r";
            } else { // is a molecular test
                testType = "m";
            }

            // get test date and time
            ZonedDateTime dateTime = ZonedDateTime.parse(t.getString("sc"));
            String date = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            // get unique identifier
            identifier = t.getString("ci");

            storeString += ";" + type + ";" + testType + ";" + date + ";" + time;

        } else if (jsonData.has("r")) {
            // recovery
            type = "r";
            JSONObject r = jsonData.getJSONArray("r").getJSONObject(0);
            // get date of recovery
            String dateFrom = LocalDate.parse(r.getString("df")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            // get date until is valid
            String dateUntil = LocalDate.parse(r.getString("du")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            // get unique identifier
            identifier = r.getString("ci");

            storeString += ";" + type + ";" + dateFrom + ";" + dateUntil;

        } else {
            throw new Exception("json structure not right");
        }

        storeData(identifier, storeString, bitmap); // store data
    }

    private void storeData(String identifier, String storeString, Bitmap bitmap) {
        // store data: name[0];DoB[1];type[2];date[3];(nDose/time/dateUntil)[4];filePath[5]
        String filePath = saveToInternalStorage(bitmap); // save qrcode image to internal storage
        SharedPreferences usersData = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = usersData.edit();
        editor.putString(identifier, storeString + ";" + filePath);
        editor.apply();

        // add identifier to the qrcode id list
        String strList = usersData.getString(Global.QR_LIST, "");
        if (strList.equals("")) {
            strList = identifier;
        } else {
            strList += ";" + identifier;
        }
        editor.putString(Global.QR_LIST, strList);
        editor.apply();
    }

    private String saveToInternalStorage(Bitmap bitmapImage){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/GreenPass/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        String uniqueFileName = UUID.randomUUID().toString();
        File mypath = new File(directory,uniqueFileName + ".png");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mypath.getAbsolutePath();
    }

}