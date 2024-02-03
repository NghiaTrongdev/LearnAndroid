
package com.example.chatingapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.example.chatingapp.R;
import com.example.chatingapp.databinding.ActivitySignInBinding;
import com.example.chatingapp.databinding.ActivitySignUpBinding;

public class SignUpActivity extends AppCompatActivity {
    private ActivitySignUpBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListener();
    }
    private void setListener(){
        binding.textviewSignIn.setOnClickListener(v -> onBackPressed());
//                startActivity(new Intent(getApplicationContext() , SignInActivity.class)));
    }
}