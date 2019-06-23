package com.fc.mis.charitable.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.ImageViewCompat;

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
import android.graphics.drawable.Drawable;
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
import android.widget.ImageView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.fc.mis.charitable.R;
import com.fc.mis.charitable.models.Event;
import com.fc.mis.charitable.models.LanguageDetection;
import com.fc.mis.charitable.models.Ngo;
import com.fc.mis.charitable.models.SwipeDismissTouchListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

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

    private AppCompatTextView mTitle;
    private AppCompatTextView mBody;
    private AppCompatTextView mLocation;
    private AppCompatTextView mTime;
    private AppCompatImageView mCoverImg;
    private Chip mNgoChip;
    private LinearLayoutCompat mImagesListLayout;


    private Event mEvent;
    private List<Uri> mImageList;
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

        CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.event_collapsing_toolbar_layout);
        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));


        // UI
        mTitle = (AppCompatTextView) findViewById(R.id.event_title_field);
        mBody = (AppCompatTextView) findViewById(R.id.event_body_field);
        mLocation = (AppCompatTextView) findViewById(R.id.event_location_field);
        mTime = (AppCompatTextView) findViewById(R.id.event_time_field);
        mCoverImg = (AppCompatImageView) findViewById(R.id.event_cover_img);
        mImagesListLayout = (LinearLayoutCompat) findViewById(R.id.event_images_list);
        mNgoChip = (Chip) findViewById(R.id.event_ngo_chip);

        mNgoChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNgo();
            }
        });

        // Variables
        mImageList = new ArrayList<>();

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

            mNgoChip.setText(mEvent.getOrgName());

            final ImageView tempImgView = new ImageView(this);
            Picasso.get().load(mEvent.getOrgThumb()).noPlaceholder().into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    tempImgView.setImageBitmap(bitmap);
                    mNgoChip.setChipIcon(tempImgView.getDrawable());
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                }
            });

            mTitle.setText(mEvent.getTitle());
            mBody.setText(mEvent.getBody());
            mLocation.setText(mEvent.getLocation());

            // change direction in case of arabic text
            LanguageDetection.checkLanguageLayoutDirectionForAr(mTitle);
            LanguageDetection.checkLanguageLayoutDirectionForAr(mBody);

            // display time
            mCalendar.setTimeInMillis(mEvent.getTime());
            displayDateTime();

            // support shared item transition
            supportPostponeEnterTransition();
            mCoverImg.setTransitionName("CoverImage");

            Picasso.get().load(mEvent.getThumbImg()).noPlaceholder().into(mCoverImg, new Callback() {
                @Override
                public void onSuccess() {
                    mCoverImg.setVisibility(View.VISIBLE);
                    supportStartPostponedEnterTransition();
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

    private void showNgo() {
        final String ngoId = mEvent.getNgoId();

        FirebaseDatabase.getInstance().getReference().child("Users").child("Ngos").child(ngoId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                dataSnapshot.getRef().removeEventListener(this);
                Ngo ngo = new Ngo();
                ngo.setId(ngoId);

                ngo.setOrgName(dataSnapshot.child("org_name").getValue().toString());

                ngo.setAdminName(dataSnapshot.child("first_name").getValue().toString() + " "
                        + dataSnapshot.child("last_name").getValue().toString());

                ngo.setOrgAddress(dataSnapshot.child("org_address").getValue().toString());

                ngo.setThumbImage(dataSnapshot.child("thumb_image").getValue().toString());

                if (dataSnapshot.hasChild("cases_num"))
                    ngo.setCasesCount(dataSnapshot.child("cases_num").getValue(Integer.class));

                if (dataSnapshot.hasChild("events_num"))
                    ngo.setEventsCount(dataSnapshot.child("events_num").getValue(Integer.class));

                Intent intent = new Intent(EventActivity.this, NgoProfileActivity.class);
                intent.putExtra("Ngo", ngo);
                startActivity(intent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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
