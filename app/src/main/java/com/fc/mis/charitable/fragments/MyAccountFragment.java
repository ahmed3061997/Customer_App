package com.fc.mis.charitable.fragments;


import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.fc.mis.charitable.R;
import com.fc.mis.charitable.activities.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 */
public class MyAccountFragment extends Fragment {
    // firebase Database
    private DatabaseReference mDatabase;
    private FirebaseUser mCurrentUser;
    // firebase storage ...
    private StorageReference mImageStorage;
    // progress dialog
    private ProgressDialog mProgressDialog;

    private CircleImageView mDisplayImage;
    private TextView mFullName;
    private TextView mStatus;
    private Button mImageBtn;
    private Button mStatusBtn;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_account, container, false);

        // firebase Database
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

        String currentUserId = mCurrentUser.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(currentUserId);
        mImageStorage = FirebaseStorage.getInstance().getReference();

        // retrieve data from database
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String first_name = dataSnapshot.child("first_name").getValue().toString();
                String last_name = dataSnapshot.child("last_name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                final String profile_image = dataSnapshot.child("profile_image").getValue().toString();
                final String thumb_image = dataSnapshot.child("thumb_image").getValue().toString();

                mFullName.setText(first_name + " " + last_name);
                mStatus.setText(status);
                Picasso.get().load(profile_image).networkPolicy(NetworkPolicy.OFFLINE)
                        .placeholder(R.drawable.default_avatar).into(mDisplayImage, new Callback() {
                    @Override
                    public void onSuccess() {
                        // IF IT RETRIEVE THE IMAGE OFFLINE SUCCESSFULLY ....
                    }

                    @Override
                    public void onError(Exception e) {
                        // IF IT CAN'T RETRIEVE THE IMAGE OFFLINE ... RETRIEVE IT ONLINE ...
                        Picasso.get().load(profile_image).placeholder(R.drawable.default_avatar).into(mDisplayImage);
                    }
                });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        mDisplayImage = (CircleImageView) view.findViewById(R.id.account_profile_img);
        mFullName = view.findViewById(R.id.account_fullname_txt);
        mStatus = view.findViewById(R.id.account_status_txt);

        mImageBtn = (Button) view.findViewById(R.id.account_image_btn);
        mImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start picker to get image for cropping and then use the image in cropping activity
                CropImage.activity()
                        .setActivityTitle("Profile Image")
                        .setCropShape(CropImageView.CropShape.OVAL)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setBorderCornerColor(Color.GREEN)
                        .setActivityMenuIconColor(Color.GREEN)
                        .setAspectRatio(1, 1)
                        .start(getActivity());
            }
        });

        mStatusBtn = (Button) view.findViewById(R.id.account_status_btn);
        mStatusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get teh value from status txtview to send it to status activity input
                //String status_value = mStatus.getText().toString();
                //Intent statusIntent = new Intent( getActivity(), StatusActivity.class);
                //statusIntent.putExtra( "status_value", status_value );
                //startActivity( statusIntent );
            }
        });
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        // Set title bar
        ((MainActivity) getActivity()).setActionBarTitle("My Account");
        ((MainActivity) getActivity()).setActionBarShadow(false);
    }
}
