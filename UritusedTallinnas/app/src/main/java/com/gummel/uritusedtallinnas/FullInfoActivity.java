package com.gummel.uritusedtallinnas;


import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.esri.core.map.Graphic;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by Gummel on 04/12/2016.
 */
public class FullInfoActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullinfo);

        // create a new LinearLayout in application context
        LinearLayout layout = (LinearLayout) findViewById(R.id.fullInfo);

        //get info from graphic and set to textview
        String outputMoreText = (String) getIntent().getSerializableExtra("graphicInfo");
        layout.addView(createTextView(outputMoreText));
    }

    private TextView createTextView(String outputText) {
        TextView textView = new TextView(this);
        textView.setText(outputText);
        textView.setTextColor(Color.BLACK);

        textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setMovementMethod(new ScrollingMovementMethod());
        return textView;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_fullinfo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();
        switch (id) {
            case R.id.action_goback:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
