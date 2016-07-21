package kitamura.virtualmarathonbasic;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

interface LocationManagerInterface {
    void onConnected();
    void onLocationChanged();
}

/**
 * Location Manager
 * 位置情報を管理するモジュール
 * @author Yasuhiko Kitamura
 * @version 1.3
 */
public class LocationManager implements GoogleApiClient.ConnectionCallbacks,  GoogleApiClient.OnConnectionFailedListener, LocationListener {
    Activity activity;
    protected Location currentLocation = null;
    private static Location lastLocation = null;
    protected GoogleApiClient mGoogleApiClient;

    protected LocationRequest mLocationRequest;
    long interval = 10000;

    boolean onFlag = false;
    boolean debug = false;

    private static float totalDistance = 0;

    /**
     * コンストラクタ
     * @param in 位置情報計測の間隔
     * @param act アクティビティ
     * @param dbg デバッグ用フラグ
     */
    LocationManager(long in, Activity act, boolean dbg) {
        activity = act;
        interval = in;
        debug = dbg;
        buildGoogleApiClient();
    }

    protected synchronized void buildGoogleApiClient() {
        //GoogleApiClientの初期化
        mGoogleApiClient = new GoogleApiClient.Builder(activity)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        //パラメータの設定
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        //位置測定更新間隔
        mLocationRequest.setInterval(interval);
        //最速の位置測定更新間隔（位置測定モードに応じてsetIntervalの値との間で更新する）
        mLocationRequest.setFastestInterval(interval/2);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        if(onFlag) return;
        //位置測定の許可が得られているかの確認
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(activity, "Check Permission", Toast.LENGTH_LONG).show();
            //確認が得られていない場合は確認を得る
            ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        if (mGoogleApiClient.isConnected()) {
            //位置測定を行う
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        onFlag = true;
    }

    protected void stopLocationUpdates() {
        if(!onFlag) return;
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        onFlag = false;
    }

    public void onConnected(@Nullable Bundle bundle) {
        //位置測定の許可が得られているかの確認
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //確認が得られていない場合は確認を得る
            ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        while (currentLocation == null) {
            currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }

        if(debug) Toast.makeText(activity, "OnConnected", Toast.LENGTH_LONG).show();
        ((LocationManagerInterface) activity).onConnected();
    }

    public void onLocationChanged(Location location) {
        if(debug) Toast.makeText(activity, "OnLocationChanged", Toast.LENGTH_LONG).show();
        currentLocation = location;

        if (lastLocation == null) {
            lastLocation = currentLocation;
            return;
        }
        float[] results = new float[3];
        Location.distanceBetween(lastLocation.getLatitude(), lastLocation.getLongitude(), currentLocation.getLatitude(), currentLocation.getLongitude(), results);
        totalDistance += results[0];

        ((LocationManagerInterface) activity).onLocationChanged();
    }

    /**
     * GoogleApiClientとの接続
     */
    public void connect() {
        mGoogleApiClient.connect();
    }

    /**
     * GoogleApiClientとの切断
     */
    public void disconnect() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * 位置情報の取得
     * @return Location 位置情報
     */
    public Location getLocation() {
        return currentLocation;
    }

    /**
     * 走行距離の取得
     * @return totalDistance 走行距離
     */
    public float getTotalDistance() {
        return totalDistance;
    }

    /**
     * 位置情報測定の開始
     */
    public void start(){
        startLocationUpdates();
    }

    /**
     * 位置情報測定の中止
     */
    public void stop(){
        stopLocationUpdates();
    }

    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }
}