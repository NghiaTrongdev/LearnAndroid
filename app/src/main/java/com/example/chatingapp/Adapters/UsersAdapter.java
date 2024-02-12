package com.example.chatingapp.Adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatingapp.Listeners.UserListener;
import com.example.chatingapp.Models.User;
import com.example.chatingapp.databinding.ItemContainerUserBinding;

import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {
    private final List<User> users;
    private final UserListener userListener;

    @NonNull
    @Override
    // Được gọi khi các view có list cần hiển thị item lên màn hình
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // sử dụng view binding để kết nối với xml
        ItemContainerUserBinding itemContainerUserBinding = ItemContainerUserBinding.inflate(
                // tạo ra một đối tượng LayoutInflater từ Context của parent
                // (trong trường hợp này là Context của RecyclerView).
                LayoutInflater.from(parent.getContext()),

                // view group mà viewholder gắn vào , ở đây là parent
                parent,
                // Tham số này chỉ định liệu view được inflate có được gắn vào parent ngay sau khi
                // inflate hay không. Bởi vì RecyclerView sẽ thêm ViewHolder vào nó khi cần,
                // nên bạn đặt tham số này là false.
                false
        );

        return new UserViewHolder(itemContainerUserBinding);
    }

    @Override
    // Được gọi khi viewgroup cần hiển thị dữ liệu mới
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.setUserData(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public UsersAdapter(List<User> users, UserListener _userlistener) {
        this.users = users;
        this.userListener = _userlistener;
    }

    class UserViewHolder extends RecyclerView.ViewHolder{
        ItemContainerUserBinding binding;
       UserViewHolder(ItemContainerUserBinding itemContainerUserBinding){
           super(itemContainerUserBinding.getRoot());
           binding = itemContainerUserBinding;
       }
        public void setUserData(User user){
           binding.textName.setText(user.name);
           binding.textEmail.setText(user.email);
           binding.imageProfile.setImageBitmap(getUserImage(user.image));
           binding.getRoot().setOnClickListener(v->userListener.onClickListener(user));
       }
    }
    private Bitmap getUserImage(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }
}
