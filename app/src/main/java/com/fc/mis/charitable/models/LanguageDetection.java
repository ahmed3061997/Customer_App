package com.fc.mis.charitable.models;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.languageid.FirebaseLanguageIdentification;

public class LanguageDetection {
    public static void detectLanguage(String text, OnCompleteListener<String> listener) {
        FirebaseLanguageIdentification identification = FirebaseNaturalLanguage.getInstance()
                .getLanguageIdentification();
        identification.identifyLanguage(text).addOnCompleteListener(listener);
    }

    public static void checkLanguageLayoutDirectionForAr(final AppCompatTextView textView) {
        detectLanguage(textView.getText().toString(), new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (!task.isSuccessful())
                    return;

                if (task.getResult().equals("ar"))
                    textView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            }
        });
    }
}
