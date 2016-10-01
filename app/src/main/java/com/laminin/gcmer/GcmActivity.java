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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

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


public class GcmActivity extends AppCompatActivity {

    private Bundle bundle;
    private String name;
    private String email;
    private String sender;

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "GCMActivity";

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private ProgressBar mRegistrationProgressBar;
    private TextView mInformationTextView;
    private ListView userListView;
    private AsyncTask<Void, Void, JSONObject> getRegisteredUsersTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gcm);

        bundle = getIntent().getExtras();
        if (null != bundle){
            name = bundle.getString(Constants.NAME);
            email = bundle.getString(Constants.EMAIL);
        }
        mRegistrationProgressBar = (ProgressBar) findViewById(R.id.registrationProgressBar);
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(Constants.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    mInformationTextView.setText(getString(R.string.gcm_send_message));
                    fetchRegisteredUsers();
                } else {
                    mInformationTextView.setText(getString(R.string.token_error_message));
                }
            }
        };

        mInformationTextView = (TextView) findViewById(R.id.informationTextView);
        userListView = (ListView) findViewById(R.id.user_list);

        /*if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            // name and phone number we need to register with our server
            Intent intent = new Intent(this, RegistrationIntentService.class);
            intent.putExtra(Constants.NAME, name);
            intent.putExtra(Constants.EMAIL, email);
            startService(intent);
        }else{
            mInformationTextView.setText(R.string.try_later);
            mRegistrationProgressBar.setVisibility(View.GONE);

            // just for testing.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            intent.putExtra(Constants.NAME, name);
            intent.putExtra(Constants.EMAIL, email);
            startService(intent);
        }*/

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if(sharedPreferences.getBoolean(Constants.SENT_TOKEN_TO_SERVER, false)){
            // fetch all the registered users
            sender = sharedPreferences.getString(Constants.SENDER, null);
            fetchRegisteredUsers();
        }else{ // we have not registered, lets register.
            sender = email;
            Intent intent = new Intent(this, RegistrationIntentService.class);
            intent.putExtra(Constants.NAME, name);
            intent.putExtra(Constants.EMAIL, email);
            startService(intent);
        }

        userListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                try{
                    Intent intent = new Intent(GcmActivity.this, ChatActivity.class);
                    intent.putExtra(Constants.SENDER, sender);
                    intent.putExtra(Constants.RECEIVER, ((JSONArray)adapterView.getAdapter().getItem(i)).get(1).toString());
                    startActivity(intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });



    }

    private void fetchRegisteredUsers() {
        getRegisteredUsersTask = new AsyncTask<Void, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(Void... voids) {

                try {
                    URL url = new URL(Constants.FETCH_ALL_USERS_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    InputStream responseInputStream = conn.getInputStream();
                    StringBuffer responseStringBuffer = new StringBuffer();
                    byte[] byteContainer = new byte[1024];
                    for (int i; (i = responseInputStream.read(byteContainer)) != -1; ) {
                        responseStringBuffer.append(new String(byteContainer, 0, i));
                    }

                    JSONObject response = new JSONObject(responseStringBuffer.toString());
                    return response;
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
            protected void onPostExecute(JSONObject response) {
                super.onPostExecute(response);

                // update the UI.

                if(null != response) {
                    try {
                        userListView.setAdapter(new UsersAdapter(GcmActivity.this, response.getJSONArray("users")));
                        userListView.setVisibility(View.VISIBLE);
                        findViewById(R.id.progress_bar_linear_layout).setVisibility(View.GONE);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else{
                    ((TextView)findViewById(R.id.informationTextView)).setText("Could not connect with server, please try later");
                    mRegistrationProgressBar.setVisibility(View.GONE);
                }

                getRegisteredUsersTask.cancel(true);
                getRegisteredUsersTask = null;
            }
        }.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gcm, menu);
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

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver, new IntentFilter(Constants.REGISTRATION_COMPLETE));
    }

    class UsersAdapter extends BaseAdapter{

        JSONArray mUserList;
        LayoutInflater mInflater;
        UsersAdapter(Context context, JSONArray userList){
            mInflater = LayoutInflater.from(context);
            mUserList = userList;
        }

        @Override
        public int getCount() {
            if (null != mUserList) return mUserList.length();
            else return 0;
        }

        @Override
        public Object getItem(int i) {
            try {
                return mUserList.get(i);
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
                view = mInflater.inflate(R.layout.row_user_list_layout, parent, false);
                holder = new ViewHolder();
                holder.name = (TextView)view.findViewById(R.id.name);
                holder.email = (TextView)view.findViewById(R.id.email);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (ViewHolder)view.getTag();
            }

            try {
                JSONArray user = mUserList.getJSONArray(position);
                holder.name.setText(user.get(0).toString());
                holder.email.setText(user.get(1).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return view;
        }

        private class ViewHolder {
            public TextView name, email;

        }
    }
}
