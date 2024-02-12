
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
        // khởi tạo đối tượng binding ánh xạ các đối tượng từ file view xml sang java
        // binding sẽ tham chiếu đến các thành phần trong file
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        preferenceManager = new PreferenceManager(getApplicationContext());
        if(preferenceManager.getBoolean(Constans.KEY_IS_SIGNED_IN))
        {
            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            finish();
        }
        //binding.getRoot() trả về View gốc của layout XML, tức là
        // ViewGroup chứa tất cả các thành phần giao diện người dùng trong file XML.
        setContentView(binding.getRoot());
        setListener();
    }
    private void setListener(){
        binding.textviewSignIn.setOnClickListener(v -> onBackPressed());
        binding.buttonSignUp.setOnClickListener(v -> {
            if(isValid()){
                SignUp();
            }
        });
        binding.layoutImage.setOnClickListener(v ->{
            // tao hành động chọn một mục từ tập hợp được chỉ định ,
            //MediaStore.Images.Media, đại diện cho URI của bộ sưu tập hình ảnh ở bên ngoài (external) của thiết bị
            // . Điều này bao gồm các hình ảnh từ thẻ SD hoặc bộ nhớ trong của thiết bị.


            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            //đây là một flag được sử dụng để chỉ định rằng các ứng dụng khác được
            // cấp quyền đọc dữ liệu từ URI được chỉ định trong Intent.
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }
    private void SignUp(){
        loading(true);
        // tạo 1 đối tượng cho phép truy cập vào database
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        // Tạo đối tượng user để lưu trữ tạm
        HashMap<String , Object> user = new HashMap<>();

        // Đặt các giá trị vào lưu tạm
        user.put(Constans.KEY_NAME,binding.inputName.getText().toString());
        user.put(Constans.KEY_EMAIL,binding.inputEmail.getText().toString());
        user.put(Constans.KEY_PASSWORD,binding.inputPassword.getText().toString());
        user.put(Constans.KEY_IMAGE,imageEncoded);


        // thêm đối tượng lưu tạm  vào cơ sở dữ liệu người dùng
        database.collection(Constans.KEY_COLLECTION_USERS)
                .add(user)

                // sử lí sự kiện thêm người dùng thành công
                .addOnSuccessListener(documentReference -> {
                    preferenceManager.putBoolean(Constans.KEY_IS_SIGNED_IN,true);
                    preferenceManager.putString(Constans.KEY_USER_ID,documentReference.getId());
                    preferenceManager.putString(Constans.KEY_NAME,binding.inputName.getText().toString());
                    preferenceManager.putString(Constans.KEY_IMAGE,imageEncoded);
                    Intent intent = new Intent(getApplicationContext(),MainActivity.class);

                    //Cấu hình Intent để xóa các Activity trước đó ra khỏi stack
                    // và tạo một Task mới cho Activity mới được khởi chạy.
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
    // Đăng ký 1 hoạt động , ở đây là chọn hình ảnh
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result ->{
                // nếu chọn thành công sẽ tiếp tục thực thi , ví dụ như chọn được ảnh,
                // còn nếu tràn bộ nhớ hoặc có lỗi thì đoạn if sẽ false
                if (result.getResultCode() == RESULT_OK){

                    // Check xem người dùng có chọn được ảnh không
                    if(result.getData() != null){
                        // lấy URI của ảnh được chọn
                        Uri imageUri = result.getData().getData();
                        try {

                            // Mở một luồng đầu vào từ URI hình ảnh thu được bằng getContentResolver
                            // cho phép truy cập dữ liệu từ các content provider khác
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);

                            // Giải mã luồng đầu vào thành một đối tượng
                            // bitmap đại diện cho hình ảnh được chọn
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageSignUp.setImageBitmap(bitmap);

                            // Ẩn văn bản
                            binding.textAddImage.setVisibility(View.GONE);

                            // Mã hoá hình ảnh
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