package com.example.greenpass;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    SharedPreferences usersData;
    GreenPassCardViewAdapter adapter;
    RecyclerView recyclerView;
    List<String> qrCodeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // get all the Green Pass identifiers into a list
        usersData = getApplicationContext().getSharedPreferences(Global.PREFS_NAME, 0);
        String qrCodeListStr = usersData.getString(Global.QR_LIST, "");
        // if list is empty do nothing
        if (qrCodeListStr.equals("")) return;
        qrCodeList = Arrays.asList(qrCodeListStr.split(";"));

        // create Green Pass cards in RecyclerView
        recyclerView = findViewById(R.id.cardList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GreenPassCardViewAdapter(this, qrCodeList);
        recyclerView.setAdapter(adapter);
        // Drag and Drop handler for the cards
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END, 0) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {

            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();

            // swap the identifiers in the list
            Collections.swap(qrCodeList, fromPosition, toPosition);
            String qrIdStr = "";
            boolean first = true;
            for (String id : qrCodeList) {
                if (first) {
                    first = false;
                } else {
                    qrIdStr += ";";
                }
                qrIdStr += id;
            }
            // update the SharedPreferences QR_LIST
            SharedPreferences.Editor editor = usersData.edit();
            editor.putString(Global.QR_LIST, qrIdStr);
            editor.apply();

            // swap cards position in the RecyclerView
            recyclerView.getAdapter().notifyItemMoved(fromPosition, toPosition);

            return false;
        }

        @Override // swipe left/right
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

        }
    };

    // onClick add Green Pass button
    public void addGp(View view) {
        Intent i = new Intent(MainActivity.this, AddQrCodeActivity.class);
        startActivity(i);
    }

    // onClick for opening the popup menu
    public void showMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            // onClick event for the elements in the menu
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.getWidget:
                        getWidget();
                        break;
                    case R.id.settings:
                        Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(i);
                        break;
                }
                return true;
            }
        });
        popup.show();
    }

    public void getWidget() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AppWidgetManager mAppWidgetManager = getSystemService(AppWidgetManager.class);

            ComponentName myProvider = new ComponentName(this, ShowGreenPassWidget.class);

            Bundle b = new Bundle();
            b.putString("ggg", "ggg");
            if (mAppWidgetManager.isRequestPinAppWidgetSupported()) {
                Intent pinnedWidgetCallbackIntent = new Intent(this, ShowGreenPassWidget.class);
                PendingIntent successCallback = PendingIntent.getBroadcast(this, 0,
                        pinnedWidgetCallbackIntent, 0);

                mAppWidgetManager.requestPinAppWidget(myProvider, b, successCallback);
            }
        }
    }

}