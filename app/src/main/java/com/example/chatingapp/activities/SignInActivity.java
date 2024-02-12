package com.example.chatingapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatingapp.databinding.ActivitySignInBinding;
import com.example.chatingapp.utilities.Constans;
import com.example.chatingapp.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListener();

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        // Log and toast
                        String msg = "Done";
                        Log.d("FCM", msg);
                        Toast.makeText(SignInActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void setListener(){
        binding.textviewCreateAccount.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext() , SignUpActivity.class)));
        binding.buttonSignIn.setOnClickListener(v -> {
            if (isValid()){
                SignIn();
            }
        });

    }
    private void SignIn(){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constans.KEY_COLLECTION_USERS)
                .whereEqualTo(Constans.KEY_EMAIL,binding.inputEmail.getText().toString())
                .whereEqualTo(Constans.KEY_PASSWORD,binding.inputPassword.getText().toString())
                .get()
                .addOnCompleteListener(task ->{
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size()>0){
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constans.KEY_IS_SIGNED_IN,true);
                        preferenceManager.putString(Constans.KEY_USER_ID,documentSnapshot.getId());
                        preferenceManager.putString(Constans.KEY_NAME,documentSnapshot.getString(Constans.KEY_NAME));
                        preferenceManager.putString(Constans.KEY_IMAGE,documentSnapshot.getString(Constans.KEY_IMAGE));
                        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        loading(false);
                        showToast("Unable to Sign In");
                    }
                });
    }
    private void loading(Boolean isloading){
        if (isloading){
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
            binding.progressbarSignIn.setVisibility(View.VISIBLE);
        } else {
            binding.buttonSignIn.setVisibility(View.VISIBLE);
            binding.progressbarSignIn.setVisibility(View.INVISIBLE);
        }
    }
    private void showToast(String value){
        Toast.makeText(getApplicationContext(),value,Toast.LENGTH_SHORT).show();
    }
    private Boolean isValid(){
        if(binding.inputEmail.getText().toString().isEmpty()){
            showToast("Enter Email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()){
            showToast("Enter valid Email");
            return false;
        } else if (binding.inputPassword.getText().toString().isEmpty()){
            showToast("Enter Password");
            return false;
        }else {
            return true;
        }
    }


}