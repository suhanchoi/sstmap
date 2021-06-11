package com.example.mynavermap;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;


import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

// MainActivity 에서 static 으로 참조
import static com.example.mynavermap.MainActivity.grvX;
import static com.example.mynavermap.MainActivity.grvY;
import static com.example.mynavermap.MainActivity.grvZ;
import static com.example.mynavermap.MainActivity.gyroX;
import static com.example.mynavermap.MainActivity.gyroY;
import static com.example.mynavermap.MainActivity.gyroZ;
import static com.example.mynavermap.MainActivity.linaccX;
import static com.example.mynavermap.MainActivity.linaccY;
import static com.example.mynavermap.MainActivity.linaccZ;

import android.location.LocationManager;

// 위치 반환 import 문


public class MyService extends Service {

    public static final String TAG = "ServiceExampleTag";

    private Interpreter getTfliteInterpreter(String modelPath) { // tf 모델 인터프리터 변수
        try {
            return new Interpreter(loadModelFile(MyService.this, modelPath));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private MappedByteBuffer loadModelFile(MyService activity, String modelPath) throws IOException { // 모델 읽어오는 함수
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }



    @Override
    public void onCreate() {

        Log.i(TAG, "service onCreate"); // 로그 출력
        super.onCreate();

    }

    public int onStartCommand(Intent intent, int flags, int startId){
        Log.i(TAG, "service onStartCommand"); // 로그 출력

        if (intent == null) {
            return MyService.START_STICKY; //서비스가 종료되어도 자동으로 다시 실행
        } else {

//          핵심 코드
            Log.i(TAG, "core run"); // 로그 출력
            final Handler handler = new Handler(Looper.getMainLooper());

            boolean[] concent = new boolean[]{false,false};
            int[] mynum = new int[]{1};

            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); // 소리알림
            Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(),notification);


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                handler.postDelayed(new Runnable() {
                    @Override public void run() {

                        Log.i(TAG, "service onStartCommand start"); // 로그 출력

                        GpsTracker GpsT = new GpsTracker(MyService.this); //GpsTracker gpsTracker 추가

                        double latitude = GpsT.getLatitude();     // 위도 (가로선) 위아래 날씨 // 좌우는 같은데 높이만 다르면 위도차이 // 위도는 아래가 큼
                        double longitude = GpsT.getLongitude();   // 경도 (세로선) 좌우 시간 // 높이가 같고 좌우가 다르면 경도차이 // 경도는 오른쪽이 큼

                        // 위경도 맵핑

                        double d1latiu = 35.1787714; // 공대쪽문 노랑색
                        double d1latid = 35.1780999;  // 35.178246 <-> 구글 값 - 0.00012
                        double d1longl = 126.9113955; // 126.911559 // 구글 -  0.00016
                        double d1longr = 126.9126187;

                        double d2latiu = 35.175448; // 후문 공원 노랑색
                        double d2latid = 35.174778;
                        double d2longl = 126.91375;
                        double d2longr = 126.91495;

                        double d3latiu = 35.175377; // 복개도로  초록
                        double d3latid = 35.174707;
                        double d3longl = 126.913818;
                        double d3longr = 126.9126187;

                        double d4latiu = 35.173802; // 북구청사거리 빨강색
                        double d4latid = 35.173132;
                        double d4longl = 126.912668;
                        double d4longr = 126.913868;

                        //tf 부분
                        float [] input = new float []{grvX,grvY,grvZ,linaccX,linaccY,linaccZ,gyroX,gyroY,gyroZ};
                        float [][] output = new float[1][2];

                        // VAT 모델을 해석할 인터프리터 생성
                        Interpreter tflite = getTfliteInterpreter("VATmodel.tflite");

                        // 모델 구동.
                        tflite.run(input, output);

                        switch(mynum[0]) { // 4초간 누적 몰입도 여부 체크

                            case 1: {
                                if (output[0][0] > output[0][1]) {
                                    concent[0] = false;
                                    concent[1] = false;
                                } else {
                                    concent[0] = true;
                                }
                                break;
                            }

                            case -1: {
                                if (concent[0] == true && output[0][0] < output[0][1]) {
                                    concent[1] = true;

                                } else {
                                    concent[1] = false;
                                    concent[0] = false;
                                }
                                break;
                            }
                        }

                        if (latitude <= d1latiu && latitude >= d1latid && longitude <= d1longr && longitude >= d1longl && concent[1] == true) {
                            Toast.makeText(MyService.this, " !위험! 2단계 지역 [공대 쪽문]입니다", Toast.LENGTH_SHORT).show();
                            ringtone.play();
                        }

                        if (latitude <= d2latiu && latitude >= d2latid && longitude <= d2longr && longitude >= d2longl && concent[1] == true) {
                            Toast.makeText(MyService.this, " !위험! 2단계 지역 [후문 공원]입니다", Toast.LENGTH_SHORT).show();
                            ringtone.play();
                        }

                        if (latitude <= d3latiu && latitude >= d3latid && longitude <= d3longr && longitude >= d3longl && concent[1] == true) {
                            Toast.makeText(MyService.this, " !위험! 1단계 지역 [복개도로]입니다", Toast.LENGTH_SHORT).show();
                            ringtone.play();
                        }

                        if (latitude <= d4latiu && latitude >= d4latid && longitude <= d4longr && longitude >= d4longl && concent[1] == true) {
                            Toast.makeText(MyService.this, " !위험! 3단계 지역 [북구청 사거리]입니다", Toast.LENGTH_SHORT).show();
                            ringtone.play();
                        }

                        mynum[0] = mynum[0]*(-1); // switch문 번갈아가면서 실행

                        Log.i(TAG, "service onStartCommand end");
                        handler.postDelayed(this,2100); //1000 1초 ->  2.1 초 마다 실행
                    }
                }, 0, 1000);
            }
        }
        return super.onStartCommand(intent, flags, startId); // 재시작
    }

    @Override public IBinder onBind(Intent intent)
    { return null; }

    @Override
    public void onDestroy() {
        Log.i(TAG, "service onDestory");
    }
}













