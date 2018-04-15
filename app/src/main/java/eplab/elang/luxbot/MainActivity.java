package eplab.elang.luxbot;

//Android Dependencies

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

//Firebase and Google Dependencies
//API.AI ( Dialog Flow Dependencies )



public class MainActivity extends AppCompatActivity implements AIListener {
    private EditText editText;
    private DatabaseReference ref;
    private AIService aiService;
    private Boolean flagFab = true;
    private static final String User = "user";
    private static final String Bot = "bot";
    private static final String Chat = "chat";
    private GoogleApiClient GAP;
    private static final String ANONYMOUS = "ANONYMOUS";

    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean mPermissionDenied = false;

    // Firebase instance variables
    private FirebaseRecyclerAdapter<ChatMessage, chat_rec> FBAdapter;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        editText = findViewById(R.id.editText);
        RelativeLayout addBtn = findViewById(R.id.addBtn);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);

        recyclerView.setHasFixedSize(true);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        ref = FirebaseDatabase.getInstance().getReference();
        ref.keepSynced(true);


        String aiApi = "5086aebad1e44e64b2023f7b5586dab4";

        final AIConfiguration config = new AIConfiguration(aiApi,
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        aiService = AIService.getService(this, config);
        aiService.setListener(this);

        final AIDataService aiDataService = new AIDataService(config);

        final AIRequest aiRequest = new AIRequest();


        addBtn.setOnClickListener(view -> {

            //Toast Notification
            Context context = getApplicationContext();
            CharSequence text = "Recording...";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();

            String message = editText.getText().toString().trim();


            if (!message.equals("")) {
                ChatMessage chatMessage = new ChatMessage(message, User);
                ref.child(Chat).push().setValue(chatMessage);

                aiRequest.setQuery(message);
                new AsyncTask<AIRequest, Void, AIResponse>() {

                    @Override
                    protected AIResponse doInBackground(AIRequest... aiRequests) {
                        final AIRequest request = aiRequests[0];
                        try {
                            return aiDataService.request(aiRequest);
                        } catch (AIServiceException ignored) {
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(AIResponse response) {
                        if (response != null) {

                            Result result = response.getResult();
                            String reply = result.getFulfillment().getSpeech();
                            ChatMessage chatMessage = new ChatMessage(reply,Bot);
                            ref.child(Chat).push().setValue(chatMessage);
                        }
                    }
                }.execute(aiRequest);
            } else {
                aiService.startListening();
            }
            editText.setText("");

        });
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //Code Here
            }


            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ImageView fab_img = findViewById(R.id.fab_img);
                Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.ic_send_white_24dp);
                Bitmap img1 = BitmapFactory.decodeResource(getResources(), R.drawable.ic_mic_white_24dp);

                if (s.toString().trim().length() != 0 && flagFab) {
                    ImageViewAnimatedChange(MainActivity.this, fab_img, img);
                    flagFab = false;
                }
                else if (s.toString().trim().length() == 0) {
                    ImageViewAnimatedChange(MainActivity.this, fab_img, img1);
                    flagFab = true;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                //Code Here
            }
        }
        );

        //Firebase Recycler Adapter
        ref = FirebaseDatabase.getInstance().getReference();
        SnapshotParser<ChatMessage> parser = snapshot -> {
            ChatMessage chatMessage = snapshot.getValue(ChatMessage.class);
            if (chatMessage != null) {
                chatMessage.setId(snapshot.getKey());
            }
            return chatMessage;
        };

        DatabaseReference mRef = ref.child(Chat);
        FirebaseRecyclerOptions<ChatMessage> options =
                new FirebaseRecyclerOptions.Builder<ChatMessage>()
                        .setQuery(mRef, parser)
                        .build();

        FBAdapter = new FirebaseRecyclerAdapter<ChatMessage, chat_rec>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final chat_rec holder, int position, @NonNull ChatMessage model) {
                if (model.getMsgText().equals(User)) {
                    holder.rightText.setText(model.getMsgText());
                    holder.rightText.setVisibility(View.VISIBLE);
                    holder.leftText.setVisibility(View.GONE);

                } else{
                    holder.leftText.setText(model.getMsgText());
                    holder.rightText.setVisibility(View.GONE);
                    holder.leftText.setVisibility(View.VISIBLE);
                }
            }

            @NonNull
            @Override
            public chat_rec onCreateViewHolder(@NonNull ViewGroup parent, int i) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                return new chat_rec(inflater.inflate(R.layout.msglist, parent, false));
            }
        };

        FBAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int chatMessageCount = FBAdapter.getItemCount();
                int lastVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();

                if (lastVisiblePosition == -1 ||
                        (positionStart >= (chatMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    recyclerView.scrollToPosition(positionStart);
                }
            }
        });

        recyclerView.setAdapter(FBAdapter);

    }

    private void ImageViewAnimatedChange(Context c,final ImageView v,final Bitmap new_image) {
        final Animation anim_out = AnimationUtils.loadAnimation(c, R.anim.zoom_out);
        final Animation anim_in  = AnimationUtils.loadAnimation(c, R.anim.zoom_in);
        anim_out.setAnimationListener(new Animation.AnimationListener()
        {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation)
            {
                v.setImageBitmap(new_image);
                anim_in.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override public void onAnimationRepeat(Animation animation) {}
                    @Override public void onAnimationEnd(Animation animation) {}
                });
                v.startAnimation(anim_in);
            }
        });
            v.startAnimation(anim_out);
    }

    @Override
    public void onResult(ai.api.model.AIResponse response) {

        Result result = response.getResult();

        String message = result.getResolvedQuery();
        ChatMessage chatMessage0 = new ChatMessage(message, User);
        ref.child(Chat).push().setValue(chatMessage0);


        String reply = result.getFulfillment().getSpeech();
        ChatMessage chatMessage = new ChatMessage(reply, Bot);
        ref.child(Chat).push().setValue(chatMessage);


    }


    @Override
    public void onPause() {
        FBAdapter.stopListening();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        FBAdapter.startListening();
    }

    @Override
    public void onError(AIError error) {
        //Code Here
    }

    @Override
    public void onAudioLevel(float level) {
        //Code Here
    }

    @Override
    public void onListeningStarted() {
        //Code Here
    }

    @Override
    public void onListeningCanceled() {
        //Code Here
    }

    @Override
    public void onListeningFinished() {
        //Code Here
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    public boolean onOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.gps_button:
                gps();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void gps() {
        Intent intent = new Intent(this, GPSFunction.class);
        startActivity(intent);
    }
}
