package com.example.chatingapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;

import com.example.chatingapp.Adapters.ChatAdapter;
import com.example.chatingapp.Models.ChatMessage;
import com.example.chatingapp.Models.User;
import com.example.chatingapp.R;
import com.example.chatingapp.databinding.ActivityChatBinding;
import com.example.chatingapp.utilities.Constans;
import com.example.chatingapp.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {
    private ActivityChatBinding binding;
    private User receivedUser;
    private List<ChatMessage> chatMessages;
    private PreferenceManager preferenceManager;
    private ChatAdapter chatAdapter;
    private FirebaseFirestore database;
    private String conversationId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListener();
        loadReceiverDetails();
        init();
        listenerMessage();
    }
    private void init(){
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(getImagefromString(receivedUser.image),
                preferenceManager.getString(Constans.KEY_USER_ID),
                chatMessages);
        binding.chatRecycleView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();

    }
    private void sendMessage(){
        HashMap<String,Object> message = new HashMap<>();
        message.put(Constans.KEY_SENDER_ID,preferenceManager.getString(Constans.KEY_USER_ID));
        message.put(Constans.KEY_RECEIVED_ID,receivedUser.id);
        message.put(Constans.KEY_MESSAGE,binding.inputMessage.getText().toString());
        message.put(Constans.KEY_TIMESTAMP,new Date());
        database.collection(Constans.KEY_COLLECTION_CHAT).add(message);

        if(conversationId != null){
            updateConversion(binding.inputMessage.getText().toString());
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constans.KEY_SENDER_ID,preferenceManager.getString(Constans.KEY_USER_ID));
            conversion.put(Constans.KEY_SENDER_IMAGE,preferenceManager.getString(Constans.KEY_IMAGE));
            conversion.put(Constans.KEY_SENDER_NAME,preferenceManager.getString(Constans.KEY_NAME));
            conversion.put(Constans.KEY_RECEIVED_ID,receivedUser.id);
            conversion.put(Constans.KEY_RECEIVER_NAME,receivedUser.name);
            conversion.put(Constans.KEY_RECEIVER_IMAGE,receivedUser.image);
            conversion.put(Constans.KEY_LAST_MESSAGE,binding.inputMessage.getText().toString());
            conversion.put(Constans.KEY_TIMESTAMP,new Date());
            addConversion(conversion);

        }
        binding.inputMessage.setText(null);
    }
    private void listenerMessage(){
        database.collection(Constans.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constans.KEY_SENDER_ID,preferenceManager.getString(Constans.KEY_USER_ID))
                .whereEqualTo(Constans.KEY_RECEIVED_ID,receivedUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Constans.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constans.KEY_SENDER_ID,receivedUser.id)
                .whereEqualTo(Constans.KEY_RECEIVED_ID,preferenceManager.getString(Constans.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if(error != null){
            return;
        }
        if(value !=null){
            int count = chatMessages.size();
            for(DocumentChange documentChange : value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constans.KEY_SENDER_ID);
                    chatMessage.receivedId = documentChange.getDocument().getString(Constans.KEY_RECEIVED_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constans.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDatetime(documentChange.getDocument().getDate(Constans.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constans.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }

            }
            Collections.sort(chatMessages,(obj1,obj2) ->obj1.dateObject.compareTo(obj2.dateObject));
            if(count ==0){
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(),chatMessages.size());
                binding.chatRecycleView.smoothScrollToPosition(chatMessages.size()-1);
            }
            binding.chatRecycleView.setVisibility(View.VISIBLE);
        }
        binding.chatProgressBar.setVisibility(View.GONE);
        if (conversationId == null){
            checkConversion();
        }

    };
    private Bitmap getImagefromString(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }
    private void loadReceiverDetails(){
        receivedUser = (User) getIntent().getSerializableExtra(Constans.KEY_USER);
        binding.textName.setText(receivedUser.name);
    }
    private void setListener(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.imageSend.setOnClickListener(v -> sendMessage());
    }
    private String getReadableDatetime(Date date){
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }
    private void checkConversion(){
        if (chatMessages.size()!=0){
            checkConversionRemotely(
                    preferenceManager.getString(Constans.KEY_USER_ID),
                    receivedUser.id
            );
            checkConversionRemotely(
                    receivedUser.id,
                    preferenceManager.getString(Constans.KEY_USER_ID)

            );
        }
    }
    private void checkConversionRemotely(String senderId,String receivedId){
        database.collection(Constans.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constans.KEY_SENDER_ID,senderId)
                .whereEqualTo(Constans.KEY_RECEIVED_ID,receivedId)
                .get()
                .addOnCompleteListener(conversationComplete);
    }
    private final OnCompleteListener<QuerySnapshot> conversationComplete = task -> {
        if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() >0){
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversationId = documentSnapshot.getId();
        }
    };
    private void addConversion(HashMap<String , Object> conversion){
        database.collection(Constans.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversationId = documentReference.getId());
    }
    private void updateConversion(String message){
        DocumentReference documentReference = database.collection(Constans.KEY_COLLECTION_CONVERSATIONS).document(conversationId);
        documentReference.update(
                Constans.KEY_LAST_MESSAGE, message,
                Constans.KEY_TIMESTAMP,new Date()
        );
    }
}