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

    // inflates the parent layout when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View greenPassCardView = layoutInflater.inflate(R.layout.green_pass_card, parent, false);
        ViewHolder viewHolder = new ViewHolder(greenPassCardView);
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

        // get data from Identifier
        String data = usersData.getString(identifier, "");
        // process user data
        List<String> dataList = Arrays.asList(data.split(";"));

        // add _NAME
        dataListReturn.add(dataList.get(0));

        // add _ADDITIONAL_INFO
        List<String> addTextList = getAdditionalInfo(dataList, spSettings);
        String addText = addTextList.get(0) + " - " + addTextList.get(1);
        dataListReturn.add(addText);

        return dataListReturn;
    }

    public static List<String> getAdditionalInfo(List<String> dataList, SharedPreferences spSettings) {
        boolean validity = spSettings.getBoolean(Global.S_KEY_validity, true);
        String addText1 = "";
        String addText2 = "";
        String date = "";
        switch (dataList.get(1)) {
            case "v":
                addText1 = "Vaccine";
                break;
            case "t":
                if (dataList.get(2).equals("r")) {
                    addText1 = "Rapid Test";
                } else {
                    addText1 = "Molecular Test";
                }
                break;
            case "r":
                addText1 = "Recovery";
                break;
        }

        if (dataList.get(1).equals("v") && dataList.get(3).equals("3")) { // if 3rd dose
            addText2 = "<font color='#2ECC71'>3rd dose</font>";
        } else if (dataList.get(1).equals("v") && dataList.get(3).equals("1")) { // if 1st dose
            addText2 = "1st Dose";
        } else {
            date = "";
            if (dataList.get(1).equals("v")) { // if 2nd dose
                if (validity) {
                    int duration = Integer.parseInt(spSettings.getString(Global.S_KEY_2dose, Integer.toString(Global.DURATION_DOSE)));
                    date = Global.getExpireDate(dataList.get(2), duration, true);
                } else {
                    addText2 = "2nd Dose";
                }
            } else if (dataList.get(1).equals("t")) { // if test
                if (validity) {
                    int duration;
                    if (dataList.get(2).equals("r")) {
                        duration = Integer.parseInt(spSettings.getString(Global.S_KEY_test_rapid, Integer.toString(Global.DURATION_TEST_RAPID)));
                    } else {
                        duration = Integer.parseInt(spSettings.getString(Global.S_KEY_test_molecular, Integer.toString(Global.DURATION_TEST_MOLECULAR)));
                    }

                    date = Global.getExpireDate(dataList.get(3), duration, false);
                }
            } else if (dataList.get(1).equals("r")) { // if recovery
                date = dataList.get(3);
            }
            if (!date.equals("")) { // if date calculated
                addText2 = "Valid until: ";
                if (Global.isExpired(date)) {
                    addText2 += "<font color='#E57373'>" + date + "</font>";

                } else {
                    addText2 += "<font color='#2ECC71'>" + date + "</font>";
                }
            }
        }

        return Arrays.asList(addText1, addText2);
    }

}
