package com.fc.mis.charitable.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.fc.mis.charitable.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    // Fields and button
    private TextInputEditText mFirstName;
    private TextInputEditText mLastName;
    private TextInputEditText mEmailAddress;
    private TextInputEditText mOrgName;
    private TextInputEditText mOrgAddress;
    private TextInputEditText mPass;
    private TextInputEditText mPassAgain;

    private TextInputLayout mFirstNameLayout;
    private TextInputLayout mLastNameLayout;
    private TextInputLayout mEmailAddressLayout;
    private TextInputLayout mOrgNameLayout;
    private TextInputLayout mOrgAddressLayout;
    private TextInputLayout mPassLayout;
    private TextInputLayout mPassAgainLayout;

    private Button mSubmitButton;

    // toolbar
    private Toolbar mToolbar;

    // firebase Auth
    private FirebaseAuth mAuth;

    // progress dialog
    private ProgressDialog mRegProgress;

    // to check if the email is fake or not ...
    Boolean correctEmail = false;

    // firebase database reference ...
    private DatabaseReference mDatabase;

    // firebase user
    private FirebaseUser mCurrentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        // toolbar
        mToolbar = (Toolbar) findViewById(R.id.reg_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Create New Account");
        getSupportActionBar().setDisplayUseLogoEnabled(true);

        // progress dialog
        mRegProgress = new ProgressDialog(this);

        // firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // fields
        mFirstName = (TextInputEditText) findViewById(R.id.reg_first_name_field);
        mLastName = (TextInputEditText) findViewById(R.id.reg_last_name_field);
        mEmailAddress = (TextInputEditText) findViewById(R.id.reg_email_field);
        mOrgName = (TextInputEditText) findViewById(R.id.reg_org_name_field);
        mOrgAddress = (TextInputEditText) findViewById(R.id.reg_org_address_field);
        mPass = (TextInputEditText) findViewById(R.id.reg_pass_field);
        mPassAgain = (TextInputEditText) findViewById(R.id.reg_pass_again_field);

        mFirstNameLayout = (TextInputLayout) findViewById(R.id.reg_first_name_layout);
        mLastNameLayout = (TextInputLayout) findViewById(R.id.reg_last_name_layout);
        mEmailAddressLayout = (TextInputLayout) findViewById(R.id.reg_email_layout);
        mOrgNameLayout = (TextInputLayout) findViewById(R.id.reg_org_name_layout);
        mOrgAddressLayout = (TextInputLayout) findViewById(R.id.reg_org_address_layout);
        mPassLayout = (TextInputLayout) findViewById(R.id.reg_pass_layout);
        mPassAgainLayout = (TextInputLayout) findViewById(R.id.reg_pass_again_layout);

        // button
        mSubmitButton = (Button) findViewById(R.id.submit_btn);
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String first_name = mFirstName.getText().toString();
                final String last_name = mLastName.getText().toString();
                final String email_address = mEmailAddress.getText().toString();
                final String org_name = mOrgName.getText().toString();
                final String org_address = mOrgAddress.getText().toString();
                final String password = mPass.getText().toString();
                final String passwordAgain = mPassAgain.getText().toString();

                // regex ... regular expression to detect valid email from unvalid email ...
                if (isValidEmailId(email_address)) {
                    correctEmail = true;
                } else {
                    correctEmail = false;
                    //Toast.makeText(getApplicationContext(), "Wrong Email Address !", Toast.LENGTH_SHORT).show();
                }

                if (TextUtils.isEmpty(first_name) || TextUtils.isEmpty(last_name) ||
                        TextUtils.isEmpty(email_address) || TextUtils.isEmpty(org_name) ||
                        TextUtils.isEmpty(org_address)) {

                    // please dont crash ... i hate you ... ( update >> it works fuck i love you )
                    mRegProgress.hide();
                    validate_reg(first_name, last_name, email_address, org_name, org_address, password, passwordAgain);
                } else {
                    // show progress dialog registring the user ...
                    mRegProgress.setTitle("Creating Account");
                    mRegProgress.setMessage("Please wait while we create your account");
                    mRegProgress.setCanceledOnTouchOutside(false);
                    mRegProgress.show();

                    if (correctEmail == true) {
                        registerUser(first_name, last_name, email_address, org_name, org_address, password, passwordAgain);
                    } else {
                        mRegProgress.hide();
                        Toast.makeText(RegisterActivity.this, "Wrong Email Address !", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    private void registerUser(final String first_name, final String last_name,
                              final String email_address, final String org_name, final String org_address,
                              final String password, final String passwordAgain) {

        mAuth.createUserWithEmailAndPassword(email_address, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {

                    // this is how we can get the current user id
                    mCurrentUser = mAuth.getCurrentUser();
                    String user_id = mCurrentUser.getUid();
                    mDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);

                    HashMap<String, Object> userMap = new HashMap<>();
                    userMap.put("first_name", first_name);
                    userMap.put("last_name", last_name);
                    userMap.put("org_name", org_name);
                    userMap.put("org_address", org_address);
                    userMap.put("status", "Hi there , i'm using CharitAble App");
                    userMap.put("profile_image", "default");
                    userMap.put("thumb_image", "default");
                    userMap.put("cases_num", 0);
                    userMap.put("events_num", 0);

                    mDatabase.setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            // Sign in success, update UI with the signed-in user's information
                            // if the register is successful before moving to another intent .. dismiss the progress dialog

                            mCurrentUser.sendEmailVerification().addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    mRegProgress.dismiss();

                                    if (task.isSuccessful()) {
                                        Toast.makeText(RegisterActivity.this, "Verification email sent", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(RegisterActivity.this, "Failed to send verification email", Toast.LENGTH_LONG).show();
                                    }

                                    mAuth.signOut();

                                    Intent mainIntent = new Intent(RegisterActivity.this, StartActivity.class);
                                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(mainIntent);
                                    finish();
                                }
                            });
                        }
                    });
                } else {
                    // If sign in fails, display a message to the user.
                    // and if we got some errors we will hide it instead of dismissing it ...
                    mRegProgress.hide();
                    validate_reg(first_name, last_name, email_address, org_name, org_address, password, passwordAgain);
                }
            }
        });
    }

    private void validate_reg(String first_name, String last_name,
                              String email_address, String orgName, String orgAddress,
                              String password, String passwordAgain) {
        // validating the sign up data
        boolean validationError = false;

        if (isEmpty(first_name)) {
            validationError = true;
            mFirstNameLayout.setErrorEnabled(true);
            mFirstNameLayout.setError("Please enter your first name");
        }

        if (isEmpty(last_name)) {
            validationError = true;
            mLastNameLayout.setErrorEnabled(true);
            mLastNameLayout.setError("Please enter your last name");
        }

        if (isEmpty(email_address)) {
            validationError = true;
            mEmailAddressLayout.setErrorEnabled(true);
            mEmailAddressLayout.setError("Please enter your emaill address");
        }

        if (isEmpty(orgName)) {
            validationError = true;
            mOrgNameLayout.setErrorEnabled(true);

            mOrgNameLayout.setError("Please enter your organization name");
        }

        if (isEmpty(orgAddress)) {
            validationError = true;
            mOrgAddressLayout.setErrorEnabled(true);
            mOrgAddressLayout.setError("Please enter your organization address");
        }

        if (!password.equals(passwordAgain)) {
            mPassLayout.setErrorEnabled(true);
            mPassAgainLayout.setErrorEnabled(true);
            Toast.makeText(this, "Please, enter the same password twice", Toast.LENGTH_SHORT).show();
        }

        if (validationError) {
            mRegProgress.hide();
            return;
        }
    }

    private boolean isMatching(String password, String password_again) {
        if (password.equals(password_again)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isEmpty(String txt) {
        if (txt.length() > 0) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isValidEmailId(String email) {

        return Pattern.compile("^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
                + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
                + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$").matcher(email).matches();
    }
}
