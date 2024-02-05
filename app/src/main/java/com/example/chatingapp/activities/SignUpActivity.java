
package com.example.chatingapp.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.chatingapp.R;
import com.example.chatingapp.databinding.ActivitySignInBinding;
import com.example.chatingapp.databinding.ActivitySignUpBinding;
import com.example.chatingapp.utilities.Constans;
import com.example.chatingapp.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {
    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;
    private String imageEncoded;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setContentView(binding.getRoot());
        setListener();
    }
    private void setListener(){
        binding.textviewSignIn.setOnClickListener(v -> onBackPressed());
//                startActivity(new Intent(getApplicationContext() , SignInActivity.class)));
        binding.buttonSignUp.setOnClickListener(v -> {
            if(isValid()){
                SignUp();
            }
        });
        binding.layoutImage.setOnClickListener(v ->{
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }
    private void SignUp(){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String , Object> user = new HashMap<>();
        user.put(Constans.KEY_NAME,binding.inputName.getText().toString());
        user.put(Constans.KEY_EMAIL,binding.inputEmail.getText().toString());
        user.put(Constans.KEY_PASSWORD,binding.inputPassword.getText().toString());
        user.put(Constans.KEY_IMAGE,imageEncoded);
        database.collection(Constans.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    preferenceManager.putBoolean(Constans.KEY_IS_SIGNED_IN,true);
                    preferenceManager.putString(Constans.KEY_USER_ID,documentReference.getId());
                    preferenceManager.putString(Constans.KEY_NAME,binding.inputName.getText().toString());
                    preferenceManager.putString(Constans.KEY_IMAGE,imageEncoded);
                    Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    loading(false);
                    showToast(e.getMessage());
                });
    }
    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }
    private String encodeImage(Bitmap bitmap){
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth/bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap,previewHeight,previewHeight,false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG ,50,byteArrayOutputStream );
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes,Base64.DEFAULT);
    }
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result ->{
                if (result.getResultCode() == RESULT_OK){
                    if(result.getData() != null){
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageSignUp.setImageBitmap(bitmap);
                            binding.textAddImage.setVisibility(View.GONE);
                            imageEncoded = encodeImage(bitmap);

                        } catch (FileNotFoundException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
    );
    private Boolean isValid(){
        if (imageEncoded == null){
            showToast("Select profile Image");
            return false;
        } else if(binding.inputName.getText().toString().trim().isEmpty()){
            showToast("Enter your name!!! ");
            return  false;
        } else if(binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter your EmailAddress");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()){
            showToast("Enter valid Email");
            return  false;
        } else if(binding.inputPassword.getText().toString().trim().isEmpty()){
            showToast("Enter your Password");
            return  false;
        }else if(binding.inputConfirmPassword.getText().toString().trim().equals(binding.inputPassword.getText().toString().trim().isEmpty())) {
            showToast("Password & confirm Password must be same");
            return false;
        } else {
            return true;

        }
    }
    private void loading(Boolean isLoading){
        if(isLoading){
            binding.buttonSignUp.setVisibility(View.VISIBLE);
            binding.progressbar.setVisibility(View.INVISIBLE);
        } else {
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.progressbar.setVisibility(View.VISIBLE);
        }
    }
}