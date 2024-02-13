package com.example.chatingapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.example.chatingapp.Adapters.RecentConversationAdapter;
import com.example.chatingapp.Listeners.ConversionListener;
import com.example.chatingapp.Models.ChatMessage;
import com.example.chatingapp.Models.User;
import com.example.chatingapp.R;
import com.example.chatingapp.databinding.ActivityMainBinding;
import com.example.chatingapp.utilities.Constans;
import com.example.chatingapp.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ConversionListener {
    private ActivityMainBinding binding;
    PreferenceManager preferenceManager;
    private RecentConversationAdapter recentConversationAdapter;
    private List<ChatMessage> conservations;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        init();
        loadUserDetails();
        getToken();
        setListeners();
        listenConservation();
    }
    private void init(){
        conservations = new ArrayList<>();
        recentConversationAdapter = new RecentConversationAdapter(conservations,this);
        database = FirebaseFirestore.getInstance();
        binding.recentConversation.setAdapter(recentConversationAdapter);
    }
    private void setListeners(){
        binding.imageSignOut.setOnClickListener(v-> signOut());
        binding.addNewChat.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), UsersActivity.class));
        });
    }
    private void listenConservation(){
        database.collection(Constans.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constans.KEY_SENDER_ID,preferenceManager.getString(Constans.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constans.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constans.KEY_RECEIVED_ID,preferenceManager.getString(Constans.KEY_USER_ID))
                .addSnapshotListener(eventListener);
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
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if(error != null){
            return;
        }
        if(value != null){
            for(DocumentChange documentChange : value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    String senderId = documentChange.getDocument().getString(Constans.KEY_SENDER_ID);
                    String receivedId = documentChange.getDocument().getString(Constans.KEY_RECEIVED_ID);
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = senderId;
                    chatMessage.receivedId = receivedId;
                    if(preferenceManager.getString(Constans.KEY_USER_ID).equals(senderId)){
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constans.KEY_RECEIVER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constans.KEY_RECEIVER_NAME);
                        chatMessage.conversionId = documentChange.getDocument().getString(Constans.KEY_RECEIVED_ID);

                    } else {
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constans.KEY_SENDER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constans.KEY_SENDER_NAME);
                        chatMessage.conversionId = documentChange.getDocument().getString(Constans.KEY_SENDER_ID);

                    }
                    chatMessage.message = documentChange.getDocument().getString(Constans.KEY_LAST_MESSAGE);
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constans.KEY_TIMESTAMP);
                    conservations.add(chatMessage);
                } else if(documentChange.getType() == DocumentChange.Type.MODIFIED){
                    for (int i = 0 ; i < conservations.size(); i++){
                        String senderId = documentChange.getDocument().getString(Constans.KEY_SENDER_ID);
                        String receivedId = documentChange.getDocument().getString(Constans.KEY_RECEIVED_ID);
                        if(conservations.get(i).senderId.equals(senderId)&& conservations.get(i).receivedId.equals(receivedId)){
                            conservations.get(i).message = documentChange.getDocument().getString(Constans.KEY_LAST_MESSAGE);
                            conservations.get(i).dateObject = documentChange.getDocument().getDate(Constans.KEY_TIMESTAMP);
                            break;
                        }
                    }
                }

            }
            Collections.sort(conservations,(obj1,obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
            recentConversationAdapter.notifyDataSetChanged();
            binding.recentConversation.smoothScrollToPosition(0);
            binding.recentConversation.setVisibility(View.VISIBLE);
            binding.progressbar.setVisibility(View.GONE);
        }
    };

    @Override
    public void onConversionClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constans.KEY_USER,user);
        startActivity(intent);
    }
}