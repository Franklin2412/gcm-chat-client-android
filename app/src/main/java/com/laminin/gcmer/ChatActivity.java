package com.laminin.gcmer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.zip.Inflater;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener{

    private String sender;
    private String receiver;

    private ListView messageListView;
    private Button sendButton;
    private EditText messageEditText;

    RelativeLayout.LayoutParams paramsLeftAlign;
    RelativeLayout.LayoutParams paramsRightAlign;

    private AsyncTask<Void, Void, JSONArray> fetchMessagesTask;
    private AsyncTask<Void, Void, JSONArray> sendMessageTask;

    private BroadcastReceiver mNotificationBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

//        lets find the sender, receiver
        Intent intent = getIntent();
        sender = intent.getStringExtra(Constants.SENDER);
        receiver = intent.getStringExtra(Constants.RECEIVER);

        messageListView = (ListView) findViewById(R.id.message_list_view);
        messageEditText = (EditText) findViewById(R.id.message_edit_text);
        (sendButton = (Button) findViewById(R.id.send_button)).setOnClickListener(this);

        paramsLeftAlign = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        paramsLeftAlign.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

        paramsRightAlign = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        paramsRightAlign.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        fetchMessages();

        mNotificationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                fetchMessages();
            }
        };

    }

    private void fetchMessages() {

        fetchMessagesTask = new AsyncTask<Void, Void, JSONArray>() {

            @Override
            protected JSONArray doInBackground(Void... voids) {
                try {
                    URL url = new URL(Constants.GET_MESSAGES_URL);
                    String postData = "sender=" + sender + "&receiver=" + receiver;
                    byte[] postParamsByte = postData.getBytes("UTF-8");

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setRequestProperty("Content-Length", String.valueOf(postParamsByte.length));
                    conn.setDoOutput(true);
                    conn.getOutputStream().write(postParamsByte);

                    InputStream responseInputStream = conn.getInputStream();
                    StringBuffer responseStringBuffer = new StringBuffer();
                    byte[] byteContainer = new byte[1024];
                    for (int i; (i = responseInputStream.read(byteContainer)) != -1; ) {
                        responseStringBuffer.append(new String(byteContainer, 0, i));
                    }

                    JSONObject response = new JSONObject(responseStringBuffer.toString());
                    if(response.getString("status").contentEquals("0")){
                        return response.getJSONArray("messages");
                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(JSONArray jsonArray) {
                super.onPostExecute(jsonArray);

                // update ui;

                updateUi(jsonArray);

                fetchMessagesTask.cancel(true);
                fetchMessagesTask = null;

            }
        }.execute();
    }

    private void updateUi(JSONArray jsonArray) {
        messageListView.setAdapter(new MessagesAdapter(ChatActivity.this, jsonArray));
        messageEditText.getText().clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.send_button:
                String message = messageEditText.getText().toString();
                if(message.trim().length() > 1) sendMessage(message);
                break;
            default:
                break;
        }
    }

    private void sendMessage(final String message) {
        sendMessageTask = new AsyncTask<Void, Void, JSONArray>() {
            @Override
            protected JSONArray doInBackground(Void... voids) {

                try {
                    URL url = new URL(Constants.SEND_NOTIFICATIONS_URL);
                    String postData = "sender=" + sender + "&receiver=" + receiver + "&content=" + message;
                    byte[] postParamsByte = postData.getBytes("UTF-8");

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setRequestProperty("Content-Length", String.valueOf(postParamsByte.length));
                    conn.setDoOutput(true);
                    conn.getOutputStream().write(postParamsByte);

                    InputStream responseInputStream = conn.getInputStream();
                    StringBuffer responseStringBuffer = new StringBuffer();
                    byte[] byteContainer = new byte[1024];
                    for (int i; (i = responseInputStream.read(byteContainer)) != -1; ) {
                        responseStringBuffer.append(new String(byteContainer, 0, i));
                    }

                    JSONObject response = new JSONObject(responseStringBuffer.toString());
                    if(response.getString("status").contentEquals("0")){
                        return response.getJSONArray("messages");
                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(JSONArray jsonArray) {
                super.onPostExecute(jsonArray);

                // update ui.
                updateUi(jsonArray);

                sendMessageTask.cancel(true);
                sendMessageTask = null;
            }
        }.execute();

    }

    class MessagesAdapter extends BaseAdapter{

        JSONArray mMessagingList;
        LayoutInflater mLayoutInflater;
        MessagesAdapter(Context context, JSONArray messageList){
            mMessagingList = messageList;
            mLayoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            if(mMessagingList != null) return mMessagingList.length();
            else return 0;
        }

        @Override
        public Object getItem(int i) {
            try {
                return mMessagingList.get(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            ViewHolder holder;
            if(convertView == null) {
                view = mLayoutInflater.inflate(R.layout.row_message_list_layout, parent, false);
                holder = new ViewHolder();
                holder.messageTextView = (TextView)view.findViewById(R.id.message_text_view);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (ViewHolder)view.getTag();
            }

            try {
                JSONArray message = mMessagingList.getJSONArray(position);
                holder.messageTextView.setText(message.get(0).toString());
                if(sender.contentEquals(message.get(1).toString())){ // send by us. align it left side
                    holder.messageTextView.setLayoutParams(paramsLeftAlign);
                }else{ // right align
                    holder.messageTextView.setLayoutParams(paramsRightAlign);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return view;
        }

        private class ViewHolder {
            public TextView messageTextView;

        }
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mNotificationBroadcastReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mNotificationBroadcastReceiver, new IntentFilter(Constants.NOTIFICATION_RECEIVED));
    }
}
