package com.example.mynavermap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.overlay.CircleOverlay;

// 위치 반환
import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;

// 센서
import android.hardware.SensorEvent;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private FusedLocationSource locationSource;

    //위치 반환
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;

    // 센서값

    public SensorManager mySensorManager; // 센서 매니저 // public 으로 변경

    public SensorEventListener gyroListener; // 센서 리스너
    public Sensor myGyroscope; // 센서

    public SensorEventListener linaccListener; // 센서 리스너
    public Sensor myLinearAcc; // 센서

    public SensorEventListener grvListener; // 센서 리스너
    public Sensor myGravity; // 센서

    public static float grvX,grvY,grvZ; // 저장할때 소수점 처리 // 모든 입력값에 * 0.1 소수점 4째자리 까지 총 숫자 5개 0.0000
    public static float linaccX,linaccY,linaccZ;
    public static float gyroX,gyroY,gyroZ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);  // 현재 위치

        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this); // 현재위치 버튼 추가


        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        } else {
        }

        // 센서 매니저 생성
        mySensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // 3가지 센서를 사용하겠다고 등록
        myGyroscope = mySensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        myLinearAcc = mySensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        myGravity = mySensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);


//         센서 이벤트 리스너
        gyroListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) { // 센서 값이 변화할 때
                gyroX = (float)(sensorEvent.values[0]);
                gyroY = (float)(sensorEvent.values[1]);
                gyroZ = (float)(sensorEvent.values[2]);
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };

        //선형가속도 리스너
        linaccListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) { // 센서 값이 변화할 때
                linaccX = (float)(sensorEvent.values[0]);
                linaccY = (float)(sensorEvent.values[1]);
                linaccZ = (float)(sensorEvent.values[2]);
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };

        //중력 리스너
        grvListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) { // 센서 값이 변화할 때
                grvX = (float)(sensorEvent.values[0]*0.1);
                grvY = (float)(sensorEvent.values[1]*0.1);
                grvZ = (float)(sensorEvent.values[2]*0.1);
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };

// 백그라운드 서비스 구현
        Intent intent = new Intent(getApplicationContext(), MyService.class);
            startService(intent);

    }// ONCREATE 끝

    public void onResume() {
        super.onResume();
        mySensorManager.registerListener(gyroListener, myGyroscope,SensorManager.SENSOR_DELAY_UI);
        mySensorManager.registerListener(grvListener, myGravity,SensorManager.SENSOR_DELAY_UI);
        mySensorManager.registerListener(linaccListener, myLinearAcc,SensorManager.SENSOR_DELAY_UI);
    }

//    public void onPause() { //센서 중지 금지
//        super.onPause();
//        mySensorManager.unregisterListener(gyroListener);
//        mySensorManager.unregisterListener(grvListener);
//       mySensorManager.unregisterListener(linaccListener);
//    }

//    public void onStop() { // 센서 중단 금지
//        super.onStop();
//    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);  // 현재 위치 표시
        naverMap.setLocationSource(locationSource);
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(true); // 현재 위치 추적 버튼 등록

        // 위험 단계별 지역 카테고리 따라 맵핑
        CircleOverlay circle1 = new CircleOverlay(); // 위험 3단계 표현 - 북구청 쪽
        circle1.setCenter(new LatLng(35.1735882959154,126.913364783667));
        circle1.setRadius(50);
        circle1.setColor(Color.RED); //빨강
        circle1.setMap(naverMap);

        CircleOverlay circle2 = new CircleOverlay(); // 위험 2단계 표현 - 공원 쪽
        circle2.setCenter(new LatLng(35.1763942037927,126.914495293922));
        circle2.setRadius(50);
        circle2.setColor(Color.YELLOW); // 노랑
        circle2.setMap(naverMap);

        CircleOverlay circle3 = new CircleOverlay(); // 위험 1단계 표현 - 복개도로 쪽
        circle3.setCenter(new LatLng(35.1752500109453,126.914321673325));
        circle3.setRadius(50);
        circle3.setColor(Color.GREEN); // 초록
        circle3.setMap(naverMap);

        CircleOverlay circle4 = new CircleOverlay(); // 위험 2단계 표현 - 공대 쪽문 쪽
        circle4.setCenter(new LatLng(35.178525,126.912109));
        circle4.setRadius(50);
        circle4.setColor(Color.YELLOW); // 노랑
        circle4.setMap(naverMap);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults);
                return;
        }
    }

    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하시겠습니까?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
                        return;
                    }
                }

                break;
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}