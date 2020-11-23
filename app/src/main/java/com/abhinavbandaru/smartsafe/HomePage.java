package com.abhinavbandaru.smartsafe;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.Objects;


public class HomePage extends AppCompatActivity {
    Button sosButton, logoutButton;
    public Boolean silentPanicRunning = false, countdownInterrupted = false, panicRunning = false,
            countdownRunning = false;
    Vibrator vibrator;
    boolean volumeDown = false, volumeUp = false;
    ImageButton logoButton, settingsButton, dotsButton, phoneButton, messagesButton, contactsButton;
    GoogleSignInClient mGoogleSignInClient;
    AudioManager audioManager;
    MediaPlayer mediaPlayer;
    String emergencyContact;
    private AlertDialog dialog;
    String truePass, falsePass;
    int cnt = 0, flag = 0, flag2 = 0;
    Thread countdownThread;
    private Handler handler;
    private LocationManager locationManager;
    String[] permissions = {"android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.SEND_SMS", "android.permission.INTERNET",
            "android.permission.ACCESS_COARSE_LOCATION","android.permission.CALL_PHONE"};


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homepage);
        ActionBar actionBar=getSupportActionBar();
        actionBar.hide();
        flag = 0;
        checkPermission();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        emergencyContact = "9912311177";
        truePass = "true";
        falsePass = "false";
        sosButton = findViewById(R.id.panic_button);
        mediaPlayer = MediaPlayer.create(HomePage.this, R.raw.siren);
        sosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(panicRunning || silentPanicRunning){
                    showDialogCheckPassword();
                } else {
                    panicRunnable.run();
                }
            }
        });
        messagesButton = findViewById(R.id.openMessages);
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        logoutButton = findViewById(R.id.logout);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googleSignOut();
            }
        });
        logoButton = findViewById(R.id.appLogo);
        logoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag = 1;
                showDialogCheckPassword();
            }
        });
        settingsButton = findViewById(R.id.appSettings);
        phoneButton = findViewById(R.id.callContact);
        phoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:"+emergencyContact));
                startActivity(callIntent);
            }
        });
        dotsButton = findViewById(R.id.dots);
        dotsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag++;
                if(flag%2 == 1) {
                    logoutButton.setAlpha((float) 1);
                    logoutButton.setClickable(true);
                } else {
                    logoutButton.setAlpha((float) 0);
                    logoutButton.setClickable(false);
                }
            }
        });
        contactsButton = findViewById(R.id.openContacts);
        handler = new Handler();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct != null) {
            String personName = acct.getDisplayName();
            String personEmail = acct.getEmail();
            String personId = acct.getId();
            Uri personPhoto = acct.getPhotoUrl();
        }
    }

    public void checkPermission(){
        ActivityCompat.requestPermissions(this, this.permissions, 1);
    }

    private void showDialogCheckPassword() {
        AlertDialog.Builder alert;
        alert = new AlertDialog.Builder(HomePage.this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.password_check, null);
        alert.setView(view);
        alert.setCancelable(true);
        dialog = alert.create();
        Objects.requireNonNull(dialog.getWindow()).requestFeature(Window.FEATURE_NO_TITLE);
        dialog.show();
    }

    private void googleSignOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(HomePage.this, "Signed Out", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private Runnable panicRunnable = new Runnable() {
        @Override
        public void run() {
            System.out.println("In panicRunnable " + panicRunning);
            System.out.println("In panicRunnable (not returned) " + countdownRunning);
            countdownRunning = false;
            panicRunning = true;
            sosButton.setText("stop");
            sosButton.setTextSize(50);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
            mediaPlayer = MediaPlayer.create(HomePage.this, R.raw.siren);
            messagesButton.setAlpha((float) 0);
            messagesButton.setClickable(false);
            mediaPlayer.start();
            if (ActivityCompat.checkSelfPermission(HomePage.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.checkSelfPermission(HomePage.this, Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (locationGPS != null && cnt % 5 == 0) {
                sendLocationSMS(locationGPS);
            }
            cnt++;
            handler.postDelayed(panicRunnable, 1000);
        }
    };

    public void sendLocationSMS(Location currentLocation) {
        SmsManager smsManager = SmsManager.getDefault();
        StringBuffer smsBody = new StringBuffer();
        smsBody.append("In Danger, please help. Current Location: \n");
        smsBody.append("http://maps.google.com?q=");
        smsBody.append(currentLocation.getLatitude());
        smsBody.append(",");
        smsBody.append(currentLocation.getLongitude());
        smsManager.sendTextMessage(emergencyContact, null, smsBody.toString(), null, null);
        Toast.makeText(HomePage.this, "Message sent", Toast.LENGTH_SHORT).show();
    }

    private Runnable silentPanic = new Runnable() {
        @Override
        public void run() {
            silentPanicRunning = true;
            panicRunning = false;
            if (ActivityCompat.checkSelfPermission(HomePage.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.checkSelfPermission(HomePage.this, Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (locationGPS != null && cnt%30 == 0) {
                sendLocationSMS(locationGPS);
            }
            cnt++;
            handler.postDelayed(panicRunnable, 1000);
        }
    };

    public void stopPanic(){
        panicRunning = false;
        silentPanicRunning = false;
        handler.removeCallbacks(panicRunnable);
        mediaPlayer.stop();
        sosButton.setText("sos");
        sosButton.setTextSize(70);
        cnt = 0;
        Toast.makeText(HomePage.this, "true pass!", Toast.LENGTH_SHORT).show();
        dialog.dismiss();
    }

    public void passwordCheck(View view){
        EditText password = dialog.findViewById(R.id.passcode);
        String pass = password.getText().toString();
        password.setText("");
        if(panicRunning){
            if(pass.equals(truePass)){
                stopPanic();
            }
            else if(pass.equals(falsePass)){
                stopPanic();
                silentPanic.run();
            } else{
                Toast.makeText(HomePage.this, "Wrong Password", Toast.LENGTH_SHORT).show();
            }
        }
        else if (silentPanicRunning){
            if(pass.equals(truePass)){
                stopPanic();
                Toast.makeText(HomePage.this, "Silent Panic Stopped", Toast.LENGTH_SHORT).show();
            } else{
                Toast.makeText(HomePage.this, "Wrong Password", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            volumeDown = true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            volumeUp = true;
        }
        if(volumeUp && volumeDown){
            panicRunnable.run();
        }
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
            volumeDown = false;
        } else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP){
            volumeUp = false;
        }
        return true;
    }
}
