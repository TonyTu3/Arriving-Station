package com.tyt.arrivingstation;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.baidu.mapapi.utils.DistanceUtil;
import com.tyt.arrivingstation.service.LocationService;

import java.util.ArrayList;
import java.util.List;

import static com.baidu.mapapi.utils.CoordinateConverter.CoordType.GPS;

public class MainActivity extends AppCompatActivity implements OnGetGeoCoderResultListener, OnGetSuggestionResultListener {
    private static final String TAG = "mainactivitytag";
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private GeoCoder mSearch;
    private BDLocation curLocation;
    private BDLocation desLocation;
    private LocationService locationService;
    private SuggestionSearch mSuggestionSearch;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> countries;
    private Overlay overlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);

        locationService = new LocationService(this);
        locationService.registerListener(new MyLocationListener());
        locationService.start();

        mSuggestionSearch = SuggestionSearch.newInstance();
        mSuggestionSearch.setOnGetSuggestionResultListener(this);

        final AutoCompleteTextView auto = (AutoCompleteTextView) findViewById(R.id.auctv);
        auto.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable editable) {
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                    adapter.notifyDataSetInvalidated();
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSuggestionSearch.requestSuggestion(new SuggestionSearchOption().city("北京").keyword(s.toString()).citylimit(true));
            }
        });

        auto.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int index, long arg3) {
                getLongLan(countries.get(index) != null ? countries.get(index) : "");
            }
        });


        countries = new ArrayList<>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, countries) {
            private Filter f;
            @NonNull
            @Override
            public Filter getFilter() {
                if (f == null) {
                    f = new Filter() {
                        @Override
                        protected synchronized FilterResults performFiltering(CharSequence c) {
                            ArrayList<Object> suggestions = new ArrayList<Object>();
                            for (String adr : countries) {
                                suggestions.add(adr);
                            }
                            Log.d("test", countries.toString());
                            FilterResults filterResults = new FilterResults();
                            filterResults.values = suggestions;
                            filterResults.count = suggestions.size();
                            return filterResults;
                        }

                        @Override
                        protected synchronized void publishResults(CharSequence c, FilterResults results) {
                            if (results.count > 0) {
                                adapter.notifyDataSetChanged();
                            } else {
                                adapter.notifyDataSetInvalidated();
                            }
                        }
                    };
                }
                return f;
            }
        };
        auto.setAdapter(adapter);

    }


    /**
     * 设置中心点
     */
    private void setUserMapCenter(LatLng cenpt) {
    //定义地图状态
        MapStatus mMapStatus = new MapStatus.Builder().target(cenpt).zoom(14).build();
    //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
    //改变地图状态
        mBaiduMap.setMapStatus(mMapStatusUpdate);

    }

    /**
     * 测距
     */
    private double getDistance() {
        LatLng curLatLng = new LatLng(curLocation.getLatitude(), curLocation.getLongitude());
        LatLng desLatLng = new LatLng(desLocation.getLatitude(), desLocation.getLongitude());

        //转换坐标
        CoordinateConverter converter = new CoordinateConverter().from(GPS).coord(desLatLng);
        LatLng desLatLng1 = converter.convert();

        addPin(desLatLng);
        double distance = DistanceUtil.getDistance(curLatLng, desLatLng1);
        setUserMapCenter(desLatLng);
        Log.e(TAG, "距离: " + distance);
        return distance;
    }

    private void addPin(LatLng desLatLng) {
        if (overlay != null)
            overlay.remove();
        //定义Maker坐标点
//        LatLng point = new LatLng(39.944251, 116.494996);
        //构建Marker图标
        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.mipmap.pin2);
        //构建MarkerOption，用于在地图上添加Marker
        MarkerOptions option = new MarkerOptions()
                .position(desLatLng) //必传参数
                .icon(bitmap) //必传参数
                .draggable(true)
                //设置平贴地图，在地图中双指下拉查看效果
                .flat(true)
                .alpha(0.5f);
        //在地图上添加Marker，并显示
        option.animateType(MarkerOptions.MarkerAnimateType.jump);
        overlay = (Marker) mBaiduMap.addOverlay(option);
    }


    /**
     * 检索位置编码
     */
    private void getLongLan(String s) {
        //1.创建地理编码检索实例；
        mSearch = GeoCoder.newInstance();
        //2.创建地理编码检索监听者；
        //3。设置地理编码检索监听者；
        mSearch.setOnGetGeoCodeResultListener(this);
        //4.发起地理编码检索；
        mSearch.geocode(new GeoCodeOption().city("北京").address(s));
    }

    /**
     * 输入建议列表
     */
    @Override
    public void onGetSuggestionResult(SuggestionResult suggestionResult) {
        synchronized (this) {
            countries.clear();
            adapter.notifyDataSetChanged();
            List<SuggestionResult.SuggestionInfo> allSuggestions = suggestionResult.getAllSuggestions();
            if (allSuggestions != null) {
                for (SuggestionResult.SuggestionInfo info : allSuggestions) {
                    if (info != null)
                        countries.add(info.key);
                }
            }
        }
    }


    /**
     * 地址-->经纬度坐标
     */
    @Override
    public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
        if (geoCodeResult == null || geoCodeResult.error != SearchResult.ERRORNO.NO_ERROR) {//没有找到检索结果
            Toast.makeText(this, "无法找到目的地经纬度", Toast.LENGTH_SHORT).show();
        } else if (geoCodeResult.getLocation() != null) {
            desLocation = new BDLocation();
            desLocation.setLatitude(geoCodeResult.getLocation().latitude);
            desLocation.setLongitude(geoCodeResult.getLocation().longitude);
            double distance = getDistance();

            if (distance < 1000) {
//                LocationApplication.mVibrator.;
            }
            Toast.makeText(this, "相距" + (int) distance + "米", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "desLocation: " + desLocation.getLatitude() + "--" + desLocation.getLongitude());
        }
    }

    /**
     * 逆向查询： 经纬度坐标->地址
     **/
    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {

    }

    private class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            curLocation = location;
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }
            Log.e(TAG, "curLocation: " + location.getLatitude() + "--" + location.getLongitude());
            // 此处设置开发者获取到的方向信息，顺时针0-360
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection()).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        mBaiduMap.setMyLocationEnabled(false);
//        mLocationClient.stop();
        mMapView.onDestroy();
        mMapView = null;
        mSearch.destroy();
        locationService.unregisterListener();
        super.onDestroy();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
            return moveTaskToBack(true);
        }
        return super.onKeyDown(keyCode, event);
    }
}
