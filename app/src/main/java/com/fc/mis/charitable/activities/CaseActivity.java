package com.fc.mis.charitable.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.fc.mis.charitable.R;
import com.fc.mis.charitable.models.Case;
import com.fc.mis.charitable.models.LanguageDetection;
import com.fc.mis.charitable.models.Ngo;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

public class CaseActivity extends AppCompatActivity {
    private static final int PICK_COVER_IMAGE = 100;
    private static final int PICK_IMAGE = 101;

    // toolbar
    private Toolbar mToolbar;

    private AppCompatTextView mTitle;
    private AppCompatTextView mBody;
    private AppCompatTextView mDonation;
    private AppCompatImageView mCoverImg;
    private LinearLayoutCompat mImagesListLayout;

    private Case mCase;
    private List<Uri> mImageList;
    private Chip mNgoChip;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_case);

        // toolbar
        mToolbar = (Toolbar) findViewById(R.id.case_toolbar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Case Details");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.case_collapsing_toolbar_layout);
        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));

        // UI
        mTitle = (AppCompatTextView) findViewById(R.id.case_title_field);
        mBody = (AppCompatTextView) findViewById(R.id.case_body_field);
        mDonation = (AppCompatTextView) findViewById(R.id.case_donation_field);
        mCoverImg = (AppCompatImageView) findViewById(R.id.case_cover_img);
        mImagesListLayout = (LinearLayoutCompat) findViewById(R.id.case_images_list);
        mNgoChip = (Chip) findViewById(R.id.case_ngo_chip);

        mNgoChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNgo();
            }
        });

        mImageList = new ArrayList<>();

        // Intent options
        Intent intent = getIntent();

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

            mNgoChip.setText(mCase.getOrgName());

            final ImageView tempImgView = new ImageView(this);
            Picasso.get().load(mCase.getOrgThumb()).noPlaceholder().into(new Target() {
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

            mTitle.setText(mCase.getTitle());
            mBody.setText(mCase.getBody());
            mDonation.setText(mCase.getDonated() + " L.E / " + mCase.getNeeded() + " L.E");

            // change direction in case of arabic text
            LanguageDetection.checkLanguageLayoutDirectionForAr(mTitle);
            LanguageDetection.checkLanguageLayoutDirectionForAr(mBody);

            // support shared item transition
            supportPostponeEnterTransition();
            mCoverImg.setTransitionName("CoverImage");

            Picasso.get().load(mCase.getThumbImg()).noPlaceholder().into(mCoverImg, new Callback() {
                @Override
                public void onSuccess() {
                    mCoverImg.setVisibility(View.VISIBLE);
                    supportStartPostponedEnterTransition();
                }

                @Override
                public void onError(Exception e) {
                }
            });

            if (mCase.getImages() != null)
                for (String url : mCase.getImages()) {
                    addImageView(Uri.parse(url));
                }

        } else {
            showAlert("Unexpected Error", "No case data specified", new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
        }
    }

    private void showNgo() {
        final String ngoId = mCase.getNgoId();

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

                Intent intent = new Intent(CaseActivity.this, NgoProfileActivity.class);
                intent.putExtra("Ngo", ngo);
                startActivity(intent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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


        if (imageUri.getScheme().equals("content")) { // local uri (image to upload) --> cache uri
            // no local image
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
