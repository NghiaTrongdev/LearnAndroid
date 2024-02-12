package com.example.chatingapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

import com.example.chatingapp.R;
import com.example.chatingapp.databinding.ActivityMainBinding;
import com.example.chatingapp.utilities.Constans;
import com.example.chatingapp.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        loadUserDetails();
        getToken();
        setListeners();
    }
    private void setListeners(){
        binding.imageSignOut.setOnClickListener(v-> signOut());
        binding.addNewChat.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), UsersActivity.class));
        });
    }
    private void showToast(String value){
        Toast.makeText(getApplicationContext(),value,Toast.LENGTH_SHORT).show();
    }
    private void loadUserDetails(){
        binding.textName.setText( preferenceManager.getString(Constans.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constans.KEY_IMAGE),Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }
    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }
    private void updateToken(String token){
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constans.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constans.KEY_USER_ID)
                );
        documentReference.update(Constans.KEY_TOKEN_FCM,token)
                .addOnFailureListener(v -> showToast("Failed to update token"));

    }
    private void signOut(){
        showToast("Siging out");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constans.KEY_COLLECTION_USERS)
                .document( preferenceManager.getString(Constans.KEY_USER_ID));

        HashMap<String , Object> updates = new HashMap<>();

        updates.put(Constans.KEY_TOKEN_FCM ,FieldValue.delete() );
        documentReference.update(updates)
                .addOnSuccessListener(v ->{
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(v -> showToast("Failed to Sign out"));
    }
}