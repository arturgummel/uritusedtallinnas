package com.gummel.uritusedtallinnas;

import android.app.Activity;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Gummel on 04/08/2016.
 */
class XmlParser extends AsyncTask<ArrayList<String>, Element, GraphicsLayer> {

    private Activity activity;
    private ProgressDialog dialog;

    public XmlParser(Activity _activity, ProgressDialog _dialog) {
        this.activity = _activity;
        this.dialog = _dialog;
    }

    private MapView mMapView;
    private List<Point> points;

    @Override
    protected GraphicsLayer doInBackground(ArrayList<String>... url) {

        ArrayList<String> urls = url[0];
        mMapView = (MapView) this.activity.findViewById(R.id.map);
        int color = Color.BLACK;
        GraphicsLayer graphicsLayer = new GraphicsLayer();
        SAXBuilder builder = new SAXBuilder();
        List<Element> rootNodeChildList = null;
        try {
            for (int u = 0; u < urls.size(); u++) {

                Document documentLink = builder.build(urls.get(u));
                if (u == 0) {
                    color = ContextCompat.getColor(activity, R.color.colorRed);
                } else if (u == 1) {
                    color = ContextCompat.getColor(activity, R.color.colorPurple);
                } else {
                    color = ContextCompat.getColor(activity, R.color.colorOrange);
                }
                Element rootNode = documentLink.getRootElement().getChild("vastus");

                if (rootNode != null && Integer.parseInt(rootNode.getChildText("kirjeid").trim()) > 0) {
                    Element rootNodeChild = rootNode.getChild("kogunemised");
                    if (rootNodeChild != null) {
                        rootNodeChildList = rootNodeChild.getChildren("kogunemine");
                       if(rootNodeChildList.size() > 0) {
                           for (int i = 0; i < rootNodeChildList.size(); i++) {
                               Element node = rootNodeChildList.get(i);
                               Map<String, Object> attributes = new TreeMap<>();
                               Document documentFullInfo = builder.build(node.getChildText("menetlus_url_xml").trim());
                               Element node2 = documentFullInfo.getRootElement();

                               attributes.put(activity.getString(R.string.get1tag), node.getChildText("loa_number"));
                               attributes.put(activity.getString(R.string.get2tag), node.getChildText("kogunemise_liik"));
                               attributes.put(activity.getString(R.string.get3tag), node2.getChildText("yrituse_vorm"));
                               attributes.put(activity.getString(R.string.get4tag), node.getChildText("nimetus"));
                               attributes.put(activity.getString(R.string.get5tag), node.getChildText("toimumiskoht"));
                               attributes.put(activity.getString(R.string.get6tag), node.getChildText("aadressi_tapsustus"));
                               attributes.put(activity.getString(R.string.get7tag), node.getChildText("toimumise_aeg"));
                               attributes.put(activity.getString(R.string.get8tag), node2.getChildText("alkoinfo"));
                               attributes.put(activity.getString(R.string.get9tag), node2.getChildText("korraldaja"));
                               attributes.put(activity.getString(R.string.get10tag), node2.getChildText("korraldaja_telefon"));
                               attributes.put(activity.getString(R.string.get11tag), node2.getChildText("valjastamise_aeg"));
                               attributes.put(activity.getString(R.string.get12tag), node2.getChildText("eritingimused"));

                               String s = node.getChildText("ruumiobjekt_kml").trim();
                               String[] str = s.split("<coordinates>");
                               String[] str2 = str[1].split("</coordinates>");
                               String[] str3 = str2[0].split(" ");

                               points = new ArrayList<>();
                               for (String a : str3) {
                                   String[] str4 = a.split(",");
                                   Double lat = Double.parseDouble(str4[0]);
                                   Double lon = Double.parseDouble(str4[1]);
                                   createPoints(lat, lon);
                               }
                        /*       if(node.getChildText("aadress") !=null){

                            List<Element> list2 = node.getChildren("aadress");

                            for(Element ac: list2){

                                System.out.println(ac.getChildText("ehak_linnaosa"));
                            }
                        }*/
                               if (points.size() == 1) {
                                   SimpleMarkerSymbol simpleMarker = new SimpleMarkerSymbol(color, 13, SimpleMarkerSymbol.STYLE.CIRCLE);
                                   Graphic pointsGraph = new Graphic(points.get(0), simpleMarker, attributes);
                                   graphicsLayer.addGraphic(pointsGraph);
                               } else {
                                   Polygon polygonGeometry = new Polygon();
                                   Graphic polygonPointGraph = null;
                                   for (int p = 0; p < points.size(); p++) {
                                       if (p == 0) {
                                           polygonGeometry.startPath(points.get(p));
                                           SimpleMarkerSymbol simpleMarker = new SimpleMarkerSymbol(color, 13, SimpleMarkerSymbol.STYLE.CIRCLE);
                                           polygonPointGraph = new Graphic(points.get(p), simpleMarker);
                                       } else {
                                           polygonGeometry.lineTo(points.get(p));
                                       }
                                   }
                                   SimpleFillSymbol fillSymbol = new SimpleFillSymbol(ContextCompat.getColor(activity, R.color.colorLightGray), SimpleFillSymbol.STYLE.SOLID);

                                   Graphic polygonGraphic = new Graphic(polygonGeometry, fillSymbol, attributes);
                                   Graphic polygonGraph = new Graphic(polygonGeometry, new SimpleLineSymbol(ContextCompat.getColor(activity, R.color.colorLightBlue), 2));

                                   graphicsLayer.addGraphic(polygonGraphic);
                                   graphicsLayer.addGraphic(polygonGraph);
                                   if (polygonPointGraph != null) {
                                       graphicsLayer.addGraphic(polygonPointGraph);
                                   }
                               }
                           }
                         }
                    }
                }
            }
        } catch (JDOMException jdomex) {
            Log.e("JDOMException", jdomex.getMessage());
        } catch (IOException io) {
            Log.e("IOException", io.getMessage());
            io.printStackTrace();
        }
        return graphicsLayer;
    }

    //create point, latitude and longitude transfer to metric notation
    private void createPoints(double latitude, double longitude) {
        Point point = GeometryEngine.project(latitude, longitude, mMapView.getSpatialReference());
        points.add(point);
    }

    //add all graphics to map layer
    @Override
    protected void onPostExecute(GraphicsLayer graphicsLayer) {
        super.onPostExecute(graphicsLayer);
        if(graphicsLayer != null) {
            mMapView.addLayer(graphicsLayer);
        }
        try {
            if ((this.dialog != null) && this.dialog.isShowing()) {
                this.dialog.dismiss();
            }
        } catch (final IllegalArgumentException e) {
            Log.d("ArgumentException", e.getMessage());
        } catch (final Exception e) {
            Log.d("Exception", e.getMessage());
        } finally {
            this.dialog = null;
        }
    }
}
