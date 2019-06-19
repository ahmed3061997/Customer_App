package com.fc.mis.charitable.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.Toolbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import com.fc.mis.charitable.R;
import com.fc.mis.charitable.models.Event;
import com.fc.mis.charitable.models.SwipeDismissTouchListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class EventActivity extends AppCompatActivity {
    private static final int PICK_COVER_IMAGE = 100;
    private static final int PICK_IMAGE = 101;

    // toolbar
    private Toolbar mToolbar;

    private TextInputEditText mTitle;
    private TextInputEditText mBody;
    private TextInputEditText mLocation;
    private TextInputEditText mTime;
    private AppCompatImageView mCoverImg;
    private LinearLayoutCompat mImagesListLayout;

    private ProgressDialog mProgress;

    private Event mEvent;
    private List<Uri> mImageList;
    private String mCurrentUserId;
    private DatabaseReference mEventsDatabase;
    private Calendar mCalendar;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        // toolbar
        mToolbar = (Toolbar) findViewById(R.id.event_toolbar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Event Details");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);


        // UI
        mTitle = (TextInputEditText) findViewById(R.id.event_title_field);
        mBody = (TextInputEditText) findViewById(R.id.event_body_field);
        mLocation = (TextInputEditText) findViewById(R.id.event_location_field);
        mTime = (TextInputEditText) findViewById(R.id.event_time_field);
        mCoverImg = (AppCompatImageView) findViewById(R.id.event_cover_img);
        mImagesListLayout = (LinearLayoutCompat) findViewById(R.id.event_images_list);


        // Variables
        mProgress = new ProgressDialog(this);
        mProgress.setTitle("Saving Event");
        mProgress.setMessage("Please wait while we upload your event");
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.setCancelable(false);

        mImageList = new ArrayList<>();

        mCurrentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mEventsDatabase = FirebaseDatabase.getInstance().getReference().child("Events").child(mCurrentUserId);


        // Intent options
        Intent intent = getIntent();

        mCalendar = Calendar.getInstance();

        if (intent.hasExtra("Event")) {
            mEvent = (Event) getIntent().getSerializableExtra("Event");

            if (mEvent == null) {
                showAlert(null, "Can't deserialize data", new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                });
                return;
            }

            mTitle.setText(mEvent.getTitle());
            mBody.setText(mEvent.getBody());
            mLocation.setText(mEvent.getLocation());

            mCalendar.setTimeInMillis(mEvent.getTime());
            displayDateTime();

            Picasso.get().load(mEvent.getThumbImg()).noPlaceholder().into(mCoverImg, new Callback() {
                @Override
                public void onSuccess() {
                    mCoverImg.setVisibility(View.VISIBLE);
                }

                @Override
                public void onError(Exception e) {
                }
            });

            if (mEvent.getImages() != null)
                for (String url : mEvent.getImages()) {
                    addImageView(Uri.parse(url));
                }


        } else {
            showAlert("Unexpected Error", "No event data specified", new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
        }
    }

    private void displayDateTime() {
        int dayNum = mCalendar.get(Calendar.DAY_OF_MONTH);
        String day = mCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.ENGLISH);
        String month = mCalendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH);

        int hour = mCalendar.get(Calendar.HOUR);
        String min = "" + mCalendar.get(Calendar.MINUTE);
        String dayNight = (mCalendar.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM");

        if (dayNight.equals("AM")) // 0
            hour = 12; // 12 instead

        if (min.equals("0")) // :0
            min += "0"; // :00 instead

        mTime.setText(String.format("%s, %s %d %d:%s %s", day, month, dayNum, hour, min, dayNight));
    }

    private void showAlert(String title, String message, DialogInterface.OnCancelListener cancelListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnCancelListener(cancelListener);

        if (mProgress != null && mProgress.isShowing())
            mProgress.hide();

        dialog.show();
    }

    private void addImageView(Uri imageUri) {
        final AppCompatImageView imageView = new AppCompatImageView(this);

        LinearLayoutCompat.LayoutParams layout = new LinearLayoutCompat.LayoutParams(
                LinearLayoutCompat.LayoutParams.MATCH_PARENT,
                LinearLayoutCompat.LayoutParams.WRAP_CONTENT);

        layout.bottomMargin = 10;

        imageView.setLayoutParams(layout);

        //imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setAdjustViewBounds(true);

        int index = mImagesListLayout.getChildCount() - 1;
        mImagesListLayout.addView(imageView, index);

        if (imageUri.getScheme().equals("content")) { // local uri (image to upload) --> cache uri
            // no local images
        } else { // online uri --> download & don't cache
            Picasso.get().load(imageUri).placeholder(R.drawable.image_place_holder).into(imageView);

            // set url to tag as indicator in case of deletion
            imageView.setTag(imageUri.toString());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}
