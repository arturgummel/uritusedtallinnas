/* Copyright 2014 ESRI
 *
 * All rights reserved under the copyright laws of the United States
 * and applicable international laws, treaties, and conventions.
 *
 * You may freely redistribute and use this sample code, with or
 * without modification, provided you include the original copyright
 * notice and use restrictions.
 *
 * See the Sample code usage restrictions document for further information.
 *
 */


package com.gummel.uritusedtallinnas;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.esri.android.map.Callout;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapOptions;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISDynamicMapServiceLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.android.runtime.ArcGISRuntime;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.tasks.identify.IdentifyParameters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity {
    //The basemap
    MapView mMapView = null;

    // The basemap switching menu items.
    MenuItem mStreetsMenuItem = null;
    MenuItem mTopoMenuItem = null;
    MenuItem mGrayMenuItem = null;

    // Create MapOptions for each type of basemap.
    final MapOptions mTopoBasemap = new MapOptions(MapOptions.MapType.TOPO);
    final MapOptions mStreetsBasemap = new MapOptions(MapOptions.MapType.STREETS);
    final MapOptions mGrayBasemap = new MapOptions(MapOptions.MapType.GRAY);

    // flag for Internet connection status
    Boolean isInternetPresent = false;
    ArcGISDynamicMapServiceLayer dynamicLayer = null;

    private GraphicsLayer graphicsLayer = null;

    // Connection detector class
    ConnectionDetector connectionDetector;

    // Visible or not dynamic Layer
    boolean dynamicLayerVisible;
    IdentifyParameters params = null;
    //show while map is loading
    static ProgressDialog dialog;

    Callout callout = null;
    //true if showing callout
    boolean showCallout = true;
    View calloutView;

    //device current location
    GraphicsLayer curlocationgraph = null;
    LocationDisplayManager lDisplayManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ArcGISRuntime.setClientId(""); //Developer Licence code
        // creating connection detector class instance
        connectionDetector = new ConnectionDetector(getApplicationContext());
        // get Internet status
        isInternetPresent = connectionDetector.isOnline();
        // check for Internet status
        if (isInternetPresent) {

            mMapView = (MapView) findViewById(R.id.map);
            // Set the MapView to allow the user to rotate the map when as part of a pinch gesture.
            mMapView.setAllowRotationByPinch(true);

            // Enabled wrap around map.
            mMapView.enableWrapAround(true);

            // Set the Esri logo to be visible, and enable map to wrap around date line.
            mMapView.setEsriLogoVisible(false);

            dynamicLayer = new ArcGISDynamicMapServiceLayer(this.getString(R.string.WorldMapServer));
            dynamicLayerVisible = dynamicLayer.isVisible();

            mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {
                public void onStatusChanged(Object source, STATUS status) {
                    if ((source == mMapView) && (status == STATUS.INITIALIZED)) {
                        curlocationgraph = new GraphicsLayer();
                        lDisplayManager = mMapView.getLocationDisplayManager();

                        lDisplayManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
                       lDisplayManager.setLocationListener(new LocationListener() {

                            boolean locationChanged = false;

                            // find device current location
                            @Override
                            public void onLocationChanged(Location loc) {

                                if (!locationChanged) {
                                    locationChanged = true;
                                    double curlatitude = loc.getLatitude();
                                    double curlongitude = loc.getLongitude();

                                    Map<String, Object> attributes = new TreeMap<>();
                                    attributes.put("yourlocation", "You are here!");
                                    Point point = GeometryEngine.project(curlatitude, curlongitude, mMapView.getSpatialReference());
                                    SimpleMarkerSymbol simpleMarker = new SimpleMarkerSymbol(Color.RED, 13, SimpleMarkerSymbol.STYLE.CIRCLE);
                                    Graphic pointsGraph = new Graphic(point, simpleMarker, attributes);

                                    curlocationgraph.addGraphic(pointsGraph);
                                    mMapView.addLayer(curlocationgraph);

                                }

                            }

                            @Override
                            public void onProviderDisabled(String arg0) {

                            }

                            @Override
                            public void onProviderEnabled(String arg0) {
                            }

                            @Override
                            public void onStatusChanged(String arg0, int arg1,
                                                        Bundle arg2) {

                            }
                        });
                        lDisplayManager.start();

                        mMapView.addLayer(dynamicLayer);
                        dynamicLayer.setVisible(false);
                        dialog = ProgressDialog.show(MainActivity.this, getResources().getString(R.string.progressdTitle),
                                getResources().getString(R.string.progressdMessage), true);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ArrayList<String> xmls = new ArrayList<>();
                                xmls.add(getResources().getString(R.string.avalikUritus));
                                xmls.add(getResources().getString(R.string.ilutulestik));
                                xmls.add(getResources().getString(R.string.spordiUritus));
                                XmlParser xmlParser = new XmlParser(MainActivity.this, dialog);
                                xmlParser.execute(xmls);
                                try {
                                    graphicsLayer = xmlParser.get();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } catch (ExecutionException e) {
                                    e.printStackTrace();
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        dialog.dismiss();
                                    }
                                });
                            }
                        }).start();
                    }
                }
            });
            dynamicLayer.setVisible(true);

            mMapView.setOnSingleTapListener(new OnSingleTapListener() {

                private static final long serialVersionUID = 1L;

                @Override
                public void onSingleTap(final float x, final float y) {

                    if (!mMapView.isLoaded()) {
                        return;
                    }
                    if (callout != null && callout.isShowing()) {
                        callout.hide();
                    }

                    if (graphicsLayer != null) {
                        graphicsLayer.clearSelection();
                        int[]  uids = graphicsLayer.getGraphicIDs(x, y, 10);
                        if (uids != null && uids.length > 0) {
                            callout = mMapView.getCallout();
                            Point point = mMapView.toMapPoint(x, y);
                            View graphicInfo = null;

                            for (int i = 0; i < uids.length; i++) {
                                int targedId = uids[i];
                                Graphic graphic = graphicsLayer.getGraphic(targedId);
                                if (graphic.getAttributes() != null) {
                                    graphicInfo = createLayout(graphic);
                                }
                            }
                            if (graphicInfo != null && showCallout) {
                                callout.setContent(graphicInfo);
                                callout.refresh();
                                callout.show(point);
                            }
                        }

                    }
                }
            });

        } else {
            mMapView = null;
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);

            alertDialogBuilder.setTitle(this.getString(R.string.alertdTitle));
            alertDialogBuilder.setIcon(R.drawable.indicator_input_error);
            alertDialogBuilder.setMessage(this.getString(R.string.alertdMessage));

            alertDialogBuilder.setPositiveButton(this.getString(R.string.exit),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            System.exit(0);
                        }
                    });
            alertDialogBuilder.setNegativeButton(this.getString(R.string.refresh),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            refreshApplication();
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Get the basemap switching menu items.
        mStreetsMenuItem = menu.getItem(1);
        mTopoMenuItem = menu.getItem(2);
        mGrayMenuItem = menu.getItem(3);
        // Also set the topo basemap menu item to be checked, as this is the default.
        mTopoMenuItem.setChecked(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (dynamicLayerVisible) {
            dynamicLayer.setVisible(false);
        } else {
            dynamicLayer.setVisible(true);
        }
        int id = item.getItemId();
        switch (id) {
            case R.id.World_Street_Map:
                if (!mStreetsMenuItem.isChecked()) {
                    mMapView.setMapOptions(mStreetsBasemap);
                    mStreetsMenuItem.setChecked(true);
                }
                return true;
            case R.id.World_Topo:
                if (!mTopoMenuItem.isChecked()) {
                    mMapView.setMapOptions(mTopoBasemap);
                    mTopoMenuItem.setChecked(true);
                }
                return true;
            case R.id.Gray:
                if (!mGrayMenuItem.isChecked()) {
                    mMapView.setMapOptions(mGrayBasemap);
                    mGrayMenuItem.setChecked(true);
                }
                return true;
            case R.id.action_refresh:
                refreshApplication();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void refreshApplication() {
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    private ViewGroup createLayout(Graphic graphic) {
        // create a new LinearLayout in application context
        LinearLayout layout = new LinearLayout(this);
        // view height and widthwrap content
        layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        // default orientation
        layout.setOrientation(LinearLayout.VERTICAL);

        String LSP = System.getProperty("line.separator");

        StringBuilder outputText = new StringBuilder();
        StringBuilder outputmoreText = new StringBuilder();
        Iterator iterator = graphic.getAttributes().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry) iterator.next();
            String key = pair.getKey().toString();
            String text = pair.getValue().toString().trim();
            if(key.equals("yourlocation")){
                outputText.append(text);
                break;
            } else {
            text = text.replaceAll("\\<.*?\\>", "");
            text = text.replace("&auml;", "ä");
            text = text.replace("&Auml;", "Ä");
            text = text.replace("&ouml;", "ö");
            text = text.replace("&Ouml;", "Ö");
            text = text.replace("&uuml;", "ü");
            text = text.replace("&Uuml;", "Ü");
            text = text.replace("&otilde;", "õ");
            text = text.replace("&Otilde;", "Õ");

            String moreInfo = text;
            if (text.length() > 27) {
                String temp = "";
                String sentence = "";
                String[] array = text.split(" "); // split by space
                for (String word : array) {

                    if ((temp.length() + word.length()) < 30) {
                        // create a temp variable and check if length with new word exceeds textview width.
                        temp += " " + word;
                    } else {
                        sentence += temp + "\n"; // add new line character
                        temp = word;
                    }
                }
                text = sentence.replaceFirst(" ", "") + temp;
            }
            if (key.equals(this.getString(R.string.get1tag)) && text.length() > 0) {
                outputText.append(this.getString(R.string.loa_number) + ": " + text + "\n");
                outputText.append(LSP);
                outputmoreText.append(this.getString(R.string.loa_number) + ": " + moreInfo + "\n\n");
            } else if (key.equals(this.getString(R.string.get2tag)) && text.length() > 0) {
                outputText.append(this.getString(R.string.kogunemise_liik) + ": " + text + "\n");
                outputText.append(LSP);
                outputmoreText.append(this.getString(R.string.kogunemise_liik) + ": " + text + "\n\n");
            } else if (key.equals(this.getString(R.string.get3tag)) && text.length() > 0) {
                outputText.append(this.getString(R.string.yrituse_vorm) + ": " + text + "\n");
                outputText.append(LSP);
                outputmoreText.append(this.getString(R.string.yrituse_vorm) + ": " + moreInfo + "\n\n");
            } else if (key.equals(this.getString(R.string.get4tag)) && text.length() > 0) {
                outputText.append(this.getString(R.string.nimetus) + ": " + text + "\n");
                outputText.append(LSP);
                outputmoreText.append(this.getString(R.string.nimetus) + ": " + moreInfo + "\n\n");
            } else if (key.equals(this.getString(R.string.get5tag)) && text.length() > 0) {
                outputText.append(this.getString(R.string.toimumiskoht) + ": " + text + "\n");
                outputText.append(LSP);
                outputmoreText.append(this.getString(R.string.toimumiskoht) + ": " + moreInfo + "\n\n");
            } else if (key.equals(this.getString(R.string.get6tag)) && text.length() > 0) {
               /* outputText.append(this.getString(R.string.aadressi_tapsustus) + ": " + text + "\n");
                outputText.append(LSP);*/
                outputmoreText.append(this.getString(R.string.aadressi_tapsustus) + ": " + moreInfo + "\n\n");
            } else if (key.equals(this.getString(R.string.get7tag)) && text.length() > 0) {
                outputText.append(this.getString(R.string.toimumise_aeg) + ": " + text);
                outputText.append(LSP);
                outputmoreText.append(this.getString(R.string.toimumise_aeg) + ": " + moreInfo + "\n\n");
            } else if (key.equals(this.getString(R.string.get8tag)) && text.length() > 0) {
                outputmoreText.append(this.getString(R.string.alkoinfo) + ": " + moreInfo + "\n");
                outputmoreText.append(LSP);
            } else if (key.equals(this.getString(R.string.get9tag)) && text.length() > 0) {
                outputmoreText.append(this.getString(R.string.korraldaja) + ": " + moreInfo + "\n");
                outputmoreText.append(LSP);
            } else if (key.equals(this.getString(R.string.get10tag)) && text.length() > 0) {
                outputmoreText.append(this.getString(R.string.korraldaja_telefon) + ": " + moreInfo + "\n");
                outputmoreText.append(LSP);
            } else if (key.equals(this.getString(R.string.get11tag)) && text.length() > 0) {
                outputmoreText.append(this.getString(R.string.valjastamise_aeg) + ": " + moreInfo + "\n");
                outputmoreText.append(LSP);
            } else if (key.equals(this.getString(R.string.get12tag)) && text.length() > 0) {
                outputmoreText.append(this.getString(R.string.eritingimused) + ":\n" + moreInfo);
                outputmoreText.append(LSP);
            }
        }
        }

        if (outputText != null && outputText.length() > 0) {
            layout.addView(createTextView(outputText));
            showCallout = true;
        } else {
            showCallout = false;
        }
        if(outputmoreText != null && outputmoreText.length() > 0) {
            layout.addView(createTextViewForMoreInfo(outputmoreText.toString()));
        }

        return layout;
    }

    public TextView createTextView(StringBuilder outputText) {
        TextView textView = new TextView(MainActivity.this);
        textView.setText(outputText);
        textView.setTextColor(Color.BLACK);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setMovementMethod(new ScrollingMovementMethod());
        textView.setLines(10);
        textView.setVerticalScrollBarEnabled(true);

        return textView;
    }

    private TextView createTextViewForMoreInfo(final String outputText) {
        TextView textView = new TextView(MainActivity.this);
        textView.setTextColor(Color.BLUE);
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,1f));
        textView.setText(this.getString(R.string.moreInfo));
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent moreInfoIntent = new Intent(MainActivity.this, FullInfoActivity.class);
                moreInfoIntent.putExtra("graphicInfo", outputText);
                startActivity(moreInfoIntent);
            }
        });
        return textView;
    }
}
