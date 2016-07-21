package kitamura.virtualmarathonbasic;

import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, LocationManagerInterface {

    private GoogleMap mMap;

    LocationManager lm;
    private CourseManager mc;
    private ArrayList<LatLng> courseLocations;

    TextView distance, time;
    Button button;

    Timer mTimer = null;            //onClickメソッドでインスタンス生成
    Handler mHandler = new Handler();   //UI Threadへのpost用ハンドラ
    float mLaptime = 0.0f;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lm = new LocationManager(10000, this, true);
        lm.stop();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mc = new CourseManager(this);
        courseLocations = mc.getCourse();

        distance = (TextView) findViewById(R.id.distance);
        time = (TextView) findViewById(R.id.time);
        button = (Button) findViewById(R.id.button);
    }

    public void onDestroy() {
        super.onDestroy();
    }

    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.zoomTo(16));
        PolylineOptions course = new PolylineOptions();
        for (int i = 0; i < courseLocations.size(); i++)
            course.add(courseLocations.get(i));
        course.color(Color.RED);
        mMap.addPolyline(course);
    }

    public void onConnected() {
        button.setText(R.string.start);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (button.getText().equals("START")) {
                    lm.start();
                    button.setText(R.string.stop);

                    mTimer = new Timer(true);
                    mTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // mHandlerを通じてUI Threadへ処理をキューイング
                            mHandler.post(new Runnable() {
                                public void run() {

                                    //実行間隔分を加算処理
                                    mLaptime += 0.1d;

                                    //計算にゆらぎがあるので小数点第1位で丸める
                                    BigDecimal bi = new BigDecimal(mLaptime);
                                    float outputValue = bi.setScale(1, BigDecimal.ROUND_HALF_UP).floatValue();

                                    //現在のLapTime
                                    time.setText(String.format("%.1f", outputValue));
                                }
                            });
                        }
                    }, 100, 100);
                } else if (button.getText().equals("STOP")) {
                    lm.stop();
                    button.setText(R.string.start);

                    if (mTimer != null) {
                        mTimer.cancel();
                        mTimer = null;
                    }
                }
            }
        });
        showLocation();
    }

    public void onLocationChanged() {
        showLocation();
    }

    public void showLocation() {

        distance.setText(String.format("%.2f", lm.getTotalDistance()));

        Location loc = mc.getLocation(lm.getTotalDistance());
        LatLng here = new LatLng(loc.getLatitude(), loc.getLongitude());
        mMap.addMarker(new MarkerOptions().position(here).title("Marker here"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(here));
    }

    protected void onStart() {
        super.onStart();
        lm.connect();
    }

    protected void onStop() {
        super.onStop();
        lm.disconnect();
    }
}
