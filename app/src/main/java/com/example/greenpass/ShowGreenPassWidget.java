package com.example.greenpass;

import static com.example.greenpass.Global.PREFS_NAME;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.Arrays;
import java.util.List;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;


public class ShowGreenPassWidget extends AppWidgetProvider {

    private static final String OnClickNext = "onClickNext";
    private static final String OnClickPrev = "onClickPrev";
    private static final String OnClickImage = "openQrCodeActivity";


    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        // handle OnClick
        if (OnClickPrev.equals(intent.getAction()) || OnClickNext.equals(intent.getAction())){ // Previous and Next Button
            // get data
            SharedPreferences usersData = context.getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
            String currentId = usersData.getString(Global.WIDGET_ID, "");
            String idsStr = usersData.getString(Global.QR_LIST, "");
            List<String> idList = Arrays.asList(idsStr.split(";"));

            // getNeededPos
            int prevPos = getNeededPos(currentId, idList, OnClickNext.equals(intent.getAction()));

            // edit Identifier in Widget
            SharedPreferences.Editor editor = usersData.edit();
            editor.putString(Global.WIDGET_ID, idList.get(prevPos));
            editor.apply();

            // force update of widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), ShowGreenPassWidget.class.getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

            onUpdate(context, appWidgetManager, appWidgetIds);

        } else if (OnClickImage.equals((intent.getAction()))) { // Image clicked
            Intent i = new Intent(context, GreenPassActivity.class);

            SharedPreferences usersData = context.getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
            String id = usersData.getString(Global.WIDGET_ID, "");

            i.putExtra("id", id);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        // get first Identifier
        SharedPreferences usersData = context.getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        String qrListStr = usersData.getString(Global.QR_LIST, "");
        String identifier = "";
        if (!qrListStr.equals("")) {
            List<String> qrList = Arrays.asList(qrListStr.split(";"));
            identifier = qrList.get(0);
        }

        // save Identifier in Widget
        SharedPreferences.Editor editor = usersData.edit();
        editor.putString(Global.WIDGET_ID, identifier);
        editor.apply();

    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        SharedPreferences usersData = context.getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = usersData.edit();
        editor.remove(Global.WIDGET_ID);
        editor.apply();

    }

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.show_green_pass_widget);

        SharedPreferences usersData = context.getApplicationContext().getSharedPreferences(PREFS_NAME, 0);

        // get identifier of qrcode selected
        String identifier = usersData.getString(Global.WIDGET_ID, "");
        String dataStr = usersData.getString(identifier, "");

        if (!dataStr.equals("")) {
            // set name
            List<String> dataList = Arrays.asList(dataStr.split(";"));
            views.setTextViewText(R.id.nameText, dataList.get(0));

            // load image
            String imagePath = dataList.get(5);

            AppWidgetTarget appWidgetTarget = new AppWidgetTarget(context, R.id.qrCodeView, views, appWidgetId) {
                @Override
                public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                    super.onResourceReady(resource, transition);
                }
            };

            Glide.with(context.getApplicationContext())
                    .asBitmap()
                    .load(imagePath)
                    .transform(new RoundedCornersTransformation(4,0))
                    .into(appWidgetTarget);

        }

        //  setup onClickListeners
        views.setOnClickPendingIntent(R.id.prevBtn, getPendingSelfIntent(context, OnClickPrev));
        views.setOnClickPendingIntent(R.id.nextBtn, getPendingSelfIntent(context, OnClickNext));
        views.setOnClickPendingIntent(R.id.qrCodeView, getPendingSelfIntent(context, OnClickImage));

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    // get next/prev position
    protected int getNeededPos(String currId, List<String> idList, boolean next) {
        int pos = 0;

        for (String entry : idList) {
            if (entry.equals(currId)) {
                break;
            }
            pos++;
        }

        int neededPos;
        if (next) {
            neededPos = (pos + 1) % idList.size();
        } else {
            neededPos = (pos - 1) % idList.size();
            if (neededPos < 0) neededPos += idList.size();
        }

        return  neededPos;
    }

}