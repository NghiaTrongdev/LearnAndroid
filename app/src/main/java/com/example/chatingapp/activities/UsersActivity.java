package com.example.chatingapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.chatingapp.Adapters.UsersAdapter;
import com.example.chatingapp.Listeners.UserListener;
import com.example.chatingapp.Models.User;
import com.example.chatingapp.R;
import com.example.chatingapp.databinding.ActivityUsersBinding;
import com.example.chatingapp.utilities.Constans;
import com.example.chatingapp.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends BaseActivity implements UserListener {


    private ActivityUsersBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getUsers();
        setListener();
    }

    private void setListener() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
    }

    private void getUsers() {
        loading(true);
        // Tạo đối tượng để trỏ đến cơ sở dữ liệu
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        // query dữ liệu từ database
        database.collection(Constans.KEY_COLLECTION_USERS)

                .get()
                // xử lý kết quả của truy vấn
                .addOnCompleteListener(v -> {
                    loading(false);
                    // lấy userID hiện tại sau khi login đã lưu
                    String currentUserId = preferenceManager.getString(Constans.KEY_USER_ID);
                    if (v.isSuccessful() && v.getResult() != null) {
                        List<User> users = new ArrayList<>();
                        // lấy dữ liệu từ kết quả trả về
                        for (QueryDocumentSnapshot x : v.getResult()) {
                            // kiểm tra xem id hiện tại có bằng cái
                            // id cũ không nếu bằng thì bỏ qua
                            if (currentUserId.equals(x.getId())) {
                                continue;
                            }

                            User user = new User();
                            user.name = x.getString(Constans.KEY_NAME);
                            user.email = x.getString(Constans.KEY_EMAIL);
                            user.image = x.getString(Constans.KEY_IMAGE);
                            user.token = x.getString(Constans.KEY_TOKEN_FCM);
                            user.id = x.getId();
                            users.add(user);
                        }
                        if (users.size() > 0) {
                            UsersAdapter usersAdapter = new UsersAdapter(users, this);
                            binding.recycleUser.setAdapter(usersAdapter);
                            binding.recycleUser.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessange();
                        }
                    }
                });


    }

    private void showErrorMessange() {
        binding.textviewError.setText(String.format("%s", "No user Available"));
        binding.processbar.setVisibility(View.VISIBLE);
    }

    private void loading(Boolean isload) {
        if (isload) {
            binding.processbar.setVisibility(View.VISIBLE);
        } else {
            binding.processbar.setVisibility(View.INVISIBLE);

        }
    }

    @Override
    public void onClickListener(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constans.KEY_USER,user);
        startActivity(intent);
        finish();
    }
}