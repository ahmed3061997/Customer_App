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

    private FloatingActionButton mFabBtn;
    private MaterialButton mAddCoverBtn;
    private MaterialButton mAddPicBtn;
    private TextInputEditText mTitle;
    private TextInputEditText mBody;
    private TextInputEditText mLocation;
    private TextInputEditText mTime;
    private AppCompatImageView mCoverImg;
    private LinearLayoutCompat mImagesListLayout;

    private ProgressDialog mProgress;

    private boolean mEditMode;
    private Event mEvent;
    private List<Uri> mImageList;
    private List<String> mImageToRemove;
    private String mCurrentUserId;
    private DatabaseReference mEventsDatabase;
    private boolean mCoverChanged = false;
    private Calendar mCalendar;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        // toolbar
        mToolbar = (Toolbar) findViewById(R.id.event_toolbar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Add Event");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);


        // UI
        mAddPicBtn = (MaterialButton) findViewById(R.id.event_add_picture_btn);
        mAddCoverBtn = (MaterialButton) findViewById(R.id.event_add_cover_btn);
        mFabBtn = (FloatingActionButton) findViewById(R.id.event_fab_btn);
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
        mImageToRemove = new ArrayList<>();

        mCurrentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mEventsDatabase = FirebaseDatabase.getInstance().getReference().child("Events").child(mCurrentUserId);


        // Event Handling
        mAddPicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addImage();
            }
        });

        mAddCoverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCoverImage();
            }
        });
        mCoverImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCoverImage();
            }
        });

        mCoverImg.setOnTouchListener(new SwipeDismissTouchListener(mCoverImg, null,
                new SwipeDismissTouchListener.DismissCallbacks() {
                    @Override
                    public boolean canDismiss(Object token) {
                        return true;
                    }

                    @Override
                    public void onDismiss(View view, Object token) {
                        removeCoverImage();
                    }
                }));

        if (mEditMode)
            mFabBtn.setTag("edit");

        mFabBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getTag().equals("done")) {
                    saveEvent();
                } else if (v.getTag().equals("edit")) {
                    enterEditMode();
                }
            }
        });

        mTime.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    pickDateTime();
            }
        });

        mTime.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {
                pickDateTime();
            }
        });


        // Intent options
        Intent intent = getIntent();
        mEditMode = intent.getBooleanExtra("EditMode", false);

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

            getSupportActionBar().setTitle("Edit Event");

            if (!mEditMode) {
                exitEditMode();
            }

        } else {
            if (mEditMode) {
                exitEditMode();
                showAlert("Unexpected Error", "No event data specified", new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                });
            }
        }
    }

    // region date & time picker

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void pickDateTime() {
        final DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                mCalendar.set(Calendar.YEAR, year);
                mCalendar.set(Calendar.MONTH, month);
                mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                pickTime();
            }
        }, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.setCancelable(false);
        datePickerDialog.setCanceledOnTouchOutside(false);
        datePickerDialog.show();
    }

    private void pickTime() {
        final TimePickerDialog timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                mCalendar.set(Calendar.MINUTE, minute);

                displayDateTime();
            }
        }, mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE), false);
        timePickerDialog.setCancelable(false);
        timePickerDialog.setCanceledOnTouchOutside(false);
        timePickerDialog.show();
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

    // endregion

    // region editMode

    private void enterEditMode() {
        getSupportActionBar().setTitle("Edit Event");
        mEditMode = true;
        mTitle.setEnabled(true);
        mBody.setEnabled(true);
        mTime.setEnabled(true);
        mLocation.setEnabled(true);
        mCoverImg.setEnabled(true);
        mImagesListLayout.setEnabled(true);
        mAddCoverBtn.setVisibility(View.VISIBLE);
        mAddPicBtn.setVisibility(View.VISIBLE);
        mFabBtn.setTag("done");
        mFabBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_done));
    }

    private void exitEditMode() {
        getSupportActionBar().setTitle("Event Details");
        mEditMode = false;
        mTitle.setEnabled(false);
        mBody.setEnabled(false);
        mTime.setEnabled(false);
        mLocation.setEnabled(false);
        mCoverImg.setEnabled(false);
        mImagesListLayout.setEnabled(false);
        mAddCoverBtn.setVisibility(View.GONE);
        mAddPicBtn.setVisibility(View.GONE);
        mFabBtn.setTag("edit");
        mFabBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_edit));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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

    // endregion

    // region updateEvent

    private boolean validate() {
        if (mTitle.getText().length() == 0 || mBody.getText().length() == 0 ||
                mLocation.getText().length() == 0 || mTime.getText().length() == 0) {

            Snackbar.make(findViewById(R.id.case_coordinator_layout),
                    "Please, be sure you fill all data", Snackbar.LENGTH_SHORT).show();

            exitEditMode();
            return false; // validation failed
        }
        return true; // validation successed
    }


    private void saveEvent() {
        if (mEvent == null)
            mEvent = new Event();

        if (!validate()) // validation failed
            return;

        String title = mTitle.getText().toString();
        String body = mBody.getText().toString();
        String location = mLocation.getText().toString();
        long time = mCalendar.getTimeInMillis();
        boolean imageChanged = mCoverChanged || mImageList.size() > 0;


        if (title.equals(mEvent.getTitle()) && body.equals(mEvent.getBody()) &&
                location.equals(mEvent.getLocation()) && time == mEvent.getTime() && !imageChanged) {

            // no changes --> check if  user wanna delete cover image or other images
            if (mCoverImg.getVisibility() == View.GONE || mImageToRemove.size() > 0) {
                mProgress.show(); // show dialog

                // skip to delete cover or other images
                updateCoverImage(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        updateImages();
                    }
                });

                return; // skip snackbar ...
            }

            Snackbar.make(findViewById(R.id.event_coordinator_layout),
                    "No changes made !", Snackbar.LENGTH_SHORT).show();

            exitEditMode();
            return;
        }

        mProgress.show();

        mEvent.setTitle(title);
        mEvent.setBody(body);
        mEvent.setLocation(location);
        mEvent.setTime(time);

        Event.saveEvent(mEvent, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    updateCoverImage(new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task) {
                            if (task != null && !task.isSuccessful()) {
                                mProgress.hide();
                                showAlert("Error", task.getException().getMessage(), null);
                                Log.d("EventActivity", "skip uploading images");
                                return; // skip uploading images
                            }
                            updateImages();
                        }
                    });
                } else {
                    showAlert("Error", task.getException().getMessage(), null);
                }
            }
        });
    }

    private void updateCoverImage(final OnCompleteListener listener) {
        if (!mCoverChanged) {
            // Check if deleted
            if (mCoverImg.getVisibility() == View.GONE) { // If GONE --> deleted
                mEventsDatabase
                        .child(mEvent.getEventId())
                        .child("thumb_img")
                        .setValue("default"); // set to default
            }

            // image not changed or deleted --> skip to next
            listener.onComplete(null);
            return;

        } // cover image changed --> continue

        // open thumb image
        Object uri = mCoverImg.getTag();
        if (uri == null) {
            listener.onComplete(null);
            return;
        }

        Uri thumbImg = (Uri) uri;
        InputStream stream;
        try {
            stream = getContentResolver().openInputStream(thumbImg);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            showAlert("Error", e.getMessage(), null);
            return;
        }

        final StorageReference eventsStorage = FirebaseStorage.getInstance().getReference()
                .child("events_images") // events folder
                .child(mCurrentUserId) // ngo id
                .child(mEvent.getEventId()); // event id

        final DatabaseReference eventNode = mEventsDatabase.child(mEvent.getEventId()); // event id

        // upload thumb image
        final StorageReference thumbFile = eventsStorage.child("thumb_img.jpg");
        thumbFile.putStream(stream).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                listener.onComplete(task);

                if (task.isSuccessful()) {

                    // assign thumb image url to event node
                    thumbFile.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            HashMap<String, Object> map = new HashMap<>();
                            map.put("thumb_img", uri.toString());
                            eventNode.updateChildren(map);
                        }
                    });

                } else {
                    showAlert("Error", task.getException().getMessage(), null);
                }
            }
        });
    }

    private void updateImages() {
        final StorageReference eventsStorage = FirebaseStorage.getInstance().getReference()
                .child("events_images") // events folder
                .child(mCurrentUserId) // ngo id
                .child(mEvent.getEventId()); // event id

        final DatabaseReference imagesNode = mEventsDatabase
                .child(mEvent.getEventId()) // event id
                .child("images");

        // remove images
        if (mImageToRemove.size() > 0) {
            for (String url : mImageToRemove) {
                DatabaseReference ref = imagesNode.equalTo(url).getRef();
                if (ref != null)
                    ref.removeValue();
            }
        }

        // no new images
        if (mImageList.size() == 0) {
            finishUpdating();
            return;
        }

        // upload images
        for (final Uri image : mImageList) {
            final DatabaseReference node = imagesNode.push();

            final StorageReference imageFile = eventsStorage.child(node.getKey() + ".jpg");

            try {
                InputStream imgStream = getContentResolver().openInputStream(image);

                imageFile.putStream(imgStream).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        mImageList.remove(image);

                        if (task.isSuccessful()) {
                            Log.d("EventActivity", "image uploaded, remaining: " + mImageList.size());

                            imageFile.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    node.setValue(uri.toString());
                                }
                            });

                        } else {
                            Log.e("EventActivity",
                                    "Error while uploading image, "
                                            + task.getException().getMessage());
                        }

                        // completed
                        if (mImageList.size() == 0)
                            finishUpdating();

                    }
                });
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void finishUpdating() {
        mProgress.hide();

        Snackbar.make(findViewById(R.id.event_coordinator_layout),
                "Event saved successfully", Snackbar.LENGTH_SHORT).show();

        exitEditMode();
    }

    // endregion

    // region images

    private void addImage() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK);
        pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");

        Intent chooserIntent = Intent.createChooser(pickIntent, "Select Picture");
        pickIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivityForResult(chooserIntent, PICK_IMAGE);
    }

    @SuppressLint("ClickableViewAccessibility")
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

        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                if (!mEditMode)
                    return false;

                final ViewGroup.LayoutParams lp = v.getLayoutParams();
                final int originalHeight = v.getHeight();

                ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(
                        v.getContext().getResources().getInteger(
                                android.R.integer.config_shortAnimTime));

                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mImageToRemove.add(v.getTag().toString());
                        mImagesListLayout.removeView(v);
                        // Reset view presentation
                        v.setAlpha(1f);
                        v.setTranslationX(0);
                        lp.height = originalHeight;
                        v.setLayoutParams(lp);
                    }
                });

                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        lp.height = (Integer) valueAnimator.getAnimatedValue();
                        v.setLayoutParams(lp);
                    }
                });

                animator.start();
                return false;
            }
        });

        imageView.setOnTouchListener(new SwipeDismissTouchListener(imageView, null,
                new SwipeDismissTouchListener.DismissCallbacks() {
                    @Override
                    public boolean canDismiss(Object token) {
                        return mEditMode;
                    }

                    @Override
                    public void onDismiss(View view, Object token) {
                        Object url = view.getTag();
                        if (url != null)
                            mImageToRemove.add(view.getTag().toString());

                        mImagesListLayout.removeView(view);
                    }
                }));


        if (imageUri.getScheme().equals("content")) { // local uri (image to upload) --> cache uri
            mImageList.add(imageUri); // cache

            try {
                InputStream imgStream = getContentResolver().openInputStream(imageUri);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inScaled = false;
                options.inSampleSize = 2;

                Bitmap img = BitmapFactory.decodeStream(imgStream, null, options);

                imageView.setImageBitmap(img);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else { // online uri --> download & don't cache
            Picasso.get().load(imageUri).placeholder(R.drawable.image_place_holder).into(imageView);

            // set url to tag as indicator in case of deletion
            imageView.setTag(imageUri.toString());
        }
    }

    // endregion

    // region coverImage

    private void addCoverImage() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK);
        pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");

        Intent chooserIntent = Intent.createChooser(pickIntent, "Select Cover Picture");

        startActivityForResult(chooserIntent, PICK_COVER_IMAGE);
    }

    private void removeCoverImage() {
        mCoverImg.setVisibility(View.GONE);
        mAddCoverBtn.setVisibility(View.VISIBLE);
        final Snackbar snackbar = Snackbar.make(findViewById(R.id.event_coordinator_layout),
                "Cover picture removed", Snackbar.LENGTH_LONG);
        snackbar.setAction("Undo", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCoverImg.setVisibility(View.VISIBLE);
                mAddCoverBtn.setVisibility(View.INVISIBLE);
                Snackbar.make(findViewById(R.id.event_coordinator_layout),
                        "Cover picture restored", Snackbar.LENGTH_SHORT).show();
            }
        });
        snackbar.show();
    }

    // endregion

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_COVER_IMAGE) {
                final Uri imageUri = data.getData();

                Picasso.get().load(imageUri).noPlaceholder().into(mCoverImg, new Callback() {
                    @Override
                    public void onSuccess() {
                        mCoverChanged = true;
                        mCoverImg.setTag(imageUri);
                        mCoverImg.setVisibility(View.VISIBLE);
                        mAddCoverBtn.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(EventActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (requestCode == PICK_IMAGE) {
                Uri imageUri = data.getData();
                addImageView(imageUri);
            }
        }
    }
}
