package com.fc.mis.charitable.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.fc.mis.charitable.R;
import com.fc.mis.charitable.models.Ngo;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class NgoProfileActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private AppCompatTextView mOrgName;
    private AppCompatTextView mAdminName;
    private AppCompatTextView mEmail;
    private AppCompatTextView mAddress;
    private CircleImageView mThumbImg;
    private FloatingActionButton mMailFab;
    private Button mViewCases;
    private Button mViewEvents;
    private Ngo mNgo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ngo_profile);

        // toolbar
        mToolbar = (Toolbar) findViewById(R.id.ngo_profile_toolbar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Ngo Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);


        // UI
        mOrgName = findViewById(R.id.ngo_profile_org_name);
        mAddress = findViewById(R.id.ngo_profile_address);
        mAdminName = findViewById(R.id.ngo_profile_admin_name);
        mThumbImg = findViewById(R.id.ngo_profile_thumb_img);
        mViewCases = findViewById(R.id.ngo_profile_view_cases_btn);
        mViewEvents = findViewById(R.id.ngo_profile_view_events_btn);
        mMailFab = findViewById(R.id.ngo_profile_mail_fab);

        mViewCases.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDetails("Cases");
            }
        });

        mViewEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDetails("Events");
            }
        });

        mMailFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        // Intent
        Intent intent = getIntent();

        if (!intent.hasExtra("Ngo")) {
            Log.e("NgoProfileActiviy", "No ngo data");
            finish();
        }

        mNgo = (Ngo) intent.getSerializableExtra("Ngo");

        mOrgName.setText(mNgo.getOrgName());
        mAddress.setText(mNgo.getOrgAddress());
        mAdminName.setText(mNgo.getAdminName());

        Picasso.get().load(mNgo.getThumbImage()).placeholder(R.drawable.default_avatar).into(mThumbImg);
    }

    private void sendMessage() {
    }


    private void showDetails(String catagory) {
        Intent intent = new Intent(this, NgoProfileExtentedActivity.class);
        intent.putExtra("ViewType", catagory);
        intent.putExtra("NgoId", mNgo.getId());
        intent.putExtra("NgoName", mNgo.getOrgName());
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}
