package com.fc.mis.charitable.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.Toolbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.fc.mis.charitable.R;
import com.fc.mis.charitable.models.Case;
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
import java.util.HashMap;
import java.util.List;

public class CaseActivity extends AppCompatActivity {
    private static final int PICK_COVER_IMAGE = 100;
    private static final int PICK_IMAGE = 101;

    // toolbar
    private Toolbar mToolbar;

    private FloatingActionButton mFabBtn;
    private MaterialButton mAddCoverBtn;
    private MaterialButton mAddPicBtn;
    private TextInputEditText mTitle;
    private TextInputEditText mBody;
    private TextInputEditText mDonated;
    private TextInputEditText mNeeded;
    private AppCompatImageView mCoverImg;
    private LinearLayoutCompat mImagesListLayout;

    private ProgressDialog mProgress;

    private boolean mEditMode;
    private Case mCase;
    private List<Uri> mImageList;
    private List<String> mImageToRemove;
    private String mCurrentUserId;
    private DatabaseReference mCasesDatabase;
    private boolean mCoverChanged = false;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_case);

        // toolbar
        mToolbar = (Toolbar) findViewById(R.id.case_toolbar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Add Case");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);


        // UI
        mAddPicBtn = (MaterialButton) findViewById(R.id.case_add_picture_btn);
        mAddCoverBtn = (MaterialButton) findViewById(R.id.case_add_cover_btn);
        mFabBtn = (FloatingActionButton) findViewById(R.id.case_fab_btn);
        mTitle = (TextInputEditText) findViewById(R.id.case_title_field);
        mBody = (TextInputEditText) findViewById(R.id.case_body_field);
        mDonated = (TextInputEditText) findViewById(R.id.case_donated_field);
        mNeeded = (TextInputEditText) findViewById(R.id.case_needed_field);
        mCoverImg = (AppCompatImageView) findViewById(R.id.case_cover_img);
        mImagesListLayout = (LinearLayoutCompat) findViewById(R.id.case_images_list);

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
                    saveCase();
                } else if (v.getTag().equals("edit")) {
                    enterEditMode();
                }
            }
        });

        mProgress = new ProgressDialog(this);
        mProgress.setTitle("Saving Case");
        mProgress.setMessage("Please wait while we upload your case");
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.setCancelable(false);

        mImageList = new ArrayList<>();
        mImageToRemove = new ArrayList<>();

        mCurrentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mCasesDatabase = FirebaseDatabase.getInstance().getReference().child("Cases").child(mCurrentUserId);


        // Intent options
        Intent intent = getIntent();
        mEditMode = intent.getBooleanExtra("EditMode", false);

        if (intent.hasExtra("Case")) {
            mCase = (Case) getIntent().getSerializableExtra("Case");

            if (mCase == null) {
                showAlert(null, "Can't deserialize data", new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                });
                return;
            }

            mTitle.setText(mCase.getTitle());
            mBody.setText(mCase.getBody());
            mDonated.setText("" + mCase.getDonated());
            mNeeded.setText("" + mCase.getNeeded());

            Picasso.get().load(mCase.getThumbImg()).noPlaceholder().into(mCoverImg, new Callback() {
                @Override
                public void onSuccess() {
                    mCoverImg.setVisibility(View.VISIBLE);
                }

                @Override
                public void onError(Exception e) {
                }
            });

            if (mCase.getImages() != null)
                for (String url : mCase.getImages()) {
                    addImageView(Uri.parse(url));
                }

            getSupportActionBar().setTitle("Edit Case");

            if (!mEditMode) {
                exitEditMode();
            }

        } else {
            if (mEditMode) {
                exitEditMode();
                showAlert("Unexpected Error", "No case data specified", new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                });
            }
        }
    }

    // region editMode

    private void enterEditMode() {
        getSupportActionBar().setTitle("Edit Case");
        mEditMode = true;
        mTitle.setEnabled(true);
        mBody.setEnabled(true);
        mNeeded.setEnabled(true);
        mDonated.setEnabled(true);
        mCoverImg.setEnabled(true);
        mImagesListLayout.setEnabled(true);
        mAddCoverBtn.setVisibility(View.VISIBLE);
        mAddPicBtn.setVisibility(View.VISIBLE);
        mFabBtn.setTag("done");
        mFabBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_done));
    }

    private void exitEditMode() {
        getSupportActionBar().setTitle("Case Details");
        mEditMode = false;
        mTitle.setEnabled(false);
        mBody.setEnabled(false);
        mNeeded.setEnabled(false);
        mDonated.setEnabled(false);
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

    // region updateCase

    private boolean validate() {
        if (mTitle.getText().length() == 0 || mBody.getText().length() == 0 ||
                mNeeded.getText().length() == 0 || mDonated.getText().length() == 0) {

            Snackbar.make(findViewById(R.id.case_coordinator_layout),
                    "Please, be sure you fill all data", Snackbar.LENGTH_SHORT).show();

            exitEditMode();
            return false; // validation failed
        }
        return true; // validation successed
    }

    private void saveCase() {
        if (mCase == null)
            mCase = new Case();

        if (!validate()) // validation failed
            return;

        String title = mTitle.getText().toString();
        String body = mBody.getText().toString();
        int donated = Integer.valueOf(mDonated.getText().toString());
        int needed = Integer.valueOf(mNeeded.getText().toString());
        boolean imageChanged = mCoverChanged || mImageList.size() > 0;

        if (title.equals(mCase.getTitle()) && body.equals(mCase.getBody()) &&
                donated == mCase.getDonated() && needed == mCase.getNeeded() && !imageChanged) {

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

            Snackbar.make(findViewById(R.id.case_coordinator_layout),
                    "No changes made !", Snackbar.LENGTH_SHORT).show();

            exitEditMode();
            return;
        }

        mProgress.show();

        mCase.setTitle(title);
        mCase.setBody(body);
        mCase.setDonated(Integer.valueOf(donated));
        mCase.setNeeded(Integer.valueOf(needed));

        Case.saveCase(mCase, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    updateCoverImage(new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task) {
                            if (task != null && !task.isSuccessful()) {
                                mProgress.hide();
                                showAlert("Error", task.getException().getMessage(), null);
                                Log.d("CaseActivity", "skip uploading images");
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
                mCasesDatabase
                        .child(mCase.getCaseId())
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

        final StorageReference casesStorage = FirebaseStorage.getInstance().getReference()
                .child("cases_images") // cases folder
                .child(mCurrentUserId) // ngo id
                .child(mCase.getCaseId()); // case id

        final DatabaseReference caseNode = mCasesDatabase.child(mCase.getCaseId()); // case id

        // upload thumb image
        final StorageReference thumbFile = casesStorage.child("thumb_img.jpg");
        thumbFile.putStream(stream).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                listener.onComplete(task);

                if (task.isSuccessful()) {

                    // assign thumb image url to case node
                    thumbFile.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            HashMap<String, Object> map = new HashMap<>();
                            map.put("thumb_img", uri.toString());
                            caseNode.updateChildren(map);
                        }
                    });

                } else {
                    showAlert("Error", task.getException().getMessage(), null);
                }
            }
        });
    }

    private void updateImages() {
        final StorageReference casesStorage = FirebaseStorage.getInstance().getReference()
                .child("cases_images") // cases folder
                .child(mCurrentUserId) // ngo id
                .child(mCase.getCaseId()); // case id

        final DatabaseReference imagesNode = mCasesDatabase
                .child(mCase.getCaseId()) // case id
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

            final StorageReference imageFile = casesStorage.child(node.getKey() + ".jpg");

            try {
                InputStream imgStream = getContentResolver().openInputStream(image);

                imageFile.putStream(imgStream).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        mImageList.remove(image);

                        if (task.isSuccessful()) {
                            Log.d("CaseActivity", "image uploaded, remaining: " + mImageList.size());

                            imageFile.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    node.setValue(uri.toString());
                                }
                            });

                        } else {
                            Log.e("CaseActivity",
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

        Snackbar.make(findViewById(R.id.case_coordinator_layout),
                "Case saved successfully", Snackbar.LENGTH_SHORT).show();

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
                        if (url != null) // assigned only if online picture
                            mImageToRemove.add(url.toString());

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
        final Snackbar snackbar = Snackbar.make(findViewById(R.id.case_coordinator_layout),
                "Cover picture removed", Snackbar.LENGTH_LONG);
        snackbar.setAction("Undo", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCoverImg.setVisibility(View.VISIBLE);
                mAddCoverBtn.setVisibility(View.INVISIBLE);
                Snackbar.make(findViewById(R.id.case_coordinator_layout),
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
                        Toast.makeText(CaseActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (requestCode == PICK_IMAGE) {
                Uri imageUri = data.getData();
                addImageView(imageUri);
            }
        }
    }
}
