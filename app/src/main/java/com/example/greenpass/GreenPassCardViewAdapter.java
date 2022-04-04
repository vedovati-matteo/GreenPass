package com.example.greenpass;

import static com.example.greenpass.Global.PREFS_NAME;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GreenPassCardViewAdapter extends RecyclerView.Adapter<GreenPassCardViewAdapter.ViewHolder>{

    private List<String> mData;
    private LayoutInflater mInflater;
    private Context context;

    // data is passed into the constructor
    GreenPassCardViewAdapter(Context context, List<String> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.context = context;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.green_pass_card, parent, false);
        ViewHolder viewHolder = new ViewHolder(listItem);
        return viewHolder;
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String identifier = mData.get(position);
        List<String> data = getDataFromIdentifier(identifier);

        holder.id = identifier;
        holder.nameText.setText(data.get(0));
        holder.addText.setText(Html.fromHtml(data.get(1)));

        holder.constraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(context, GreenPassActivity.class);
                i.putExtra("id", identifier);
                context.startActivity(i);
            }
        });

    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder {
        String id;
        ConstraintLayout constraintLayout;
        TextView nameText;
        TextView addText;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.nameText);
            addText = itemView.findViewById(R.id.addText);
            constraintLayout = itemView.findViewById(R.id.cardConstraintLayout);
        }
    }

    // get from the identifier the data needed to be displayed in the card
    public List<String> getDataFromIdentifier(String identifier) {

        List<String> dataListReturn = new ArrayList<>();
        // get users data
        SharedPreferences usersData = context.getSharedPreferences(PREFS_NAME, 0);
        // get settings data
        SharedPreferences spSettings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean validity = spSettings.getBoolean(Global.S_KEY_validity, true);

        // get data from Identifier
        String data = usersData.getString(identifier, "");
        // process user data
        List<String> dataList = Arrays.asList(data.split(";"));

        // add _NAME
        dataListReturn.add(dataList.get(0));

        // add _ADDITIONAL_INFO
        String addText = "";
        String date = "";
        switch (dataList.get(2)) {
            case "v":
                addText = "Vaccine";
                break;
            case "t":
                addText = "Test";
                break;
            case "r":
                addText = "Recovery";
                break;
        }

        if (dataList.get(2).equals("v") && dataList.get(4).equals("3")) { // if 3rd dose
            addText += " - <font color='#2ECC71'>3rd dose</font>";
        } else if (dataList.get(2).equals("v") && dataList.get(4).equals("1")) { // if 1st dose
            addText += " - 1st Dose";
        } else {
            date = "";
            if (dataList.get(2).equals("v")) { // if 2nd dose
                if (validity) {
                    int duration = Integer.parseInt(spSettings.getString(Global.S_KEY_2dose, Integer.toString(Global.DURATION_DOSE)));
                    date = Global.getExpireDate(dataList.get(3), duration, true);
                } else {
                    addText += " - 2nd Dose";
                }
            } else if (dataList.get(2).equals("t")) { // if test
                if (validity) {
                    int duration = Integer.parseInt(spSettings.getString(Global.S_KEY_test, Integer.toString(Global.DURATION_TEST)));
                    date = Global.getExpireDate(dataList.get(3), duration, false);
                } else {
                    addText += " - Test";
                }
            } else if (dataList.get(2).equals("r")) { // if recovery
                date = dataList.get(4);
            }
            if (!date.equals("")) { // if date calculated
                addText += " - Valid until: ";
                if (Global.isExpired(date)) {
                    addText += "<font color='#E57373'>" + date + "</font>";

                } else {
                    addText += "<font color='#2ECC71'>" + date + "</font>";
                }
            }
        }
        dataListReturn.add(addText);

        return dataListReturn;
    }

}
