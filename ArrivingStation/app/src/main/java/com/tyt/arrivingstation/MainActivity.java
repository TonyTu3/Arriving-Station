package com.tyt.arrivingstation;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;

public class MainActivity extends AppCompatActivity {

    private GeoCoder mSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //  http://lbsyun.baidu.com/index.php?title=android-locsdk/guide/create-project/android-studio


        // TODO: 2018/9/19 检索位置编码
        //1.创建地理编码检索实例；
        mSearch = GeoCoder.newInstance();
        //2.创建地理编码检索监听者；
        OnGetGeoCoderResultListener listener = new OnGetGeoCoderResultListener() {
            public void onGetGeoCodeResult(GeoCodeResult result) {
                if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                    //没有检索到结果
                }
                Log.e("tyt", "latitude: "+result.getLocation().latitude );
                Log.e("tyt", "longitude: "+result.getLocation().longitude);
                Log.e("tyt", "truth: 40.0030650000,116.4870030000");
                //获取地理编码结果
            }

            @Override
            public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
                if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {//没有找到检索结果
                }//获取反向地理编码结果
            }
        };

        //3。设置地理编码检索监听者；
        mSearch.setOnGetGeoCodeResultListener(listener);
        //4.发起地理编码检索；
        mSearch.geocode(new GeoCodeOption().city("北京").address("地铁望京东站"));
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //5.释放地理编码检索实例；
        mSearch.destroy();
    }
}
