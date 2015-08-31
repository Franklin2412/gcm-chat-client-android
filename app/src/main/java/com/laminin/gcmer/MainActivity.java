package com.laminin.gcmer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    EditText nameEditText;
    EditText emailEditText;
    Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nameEditText = (EditText) findViewById(R.id.edit_text_name);
        emailEditText = (EditText) findViewById(R.id.edit_text_email);
        registerButton = (Button) findViewById(R.id.button_register);
        registerButton.setOnClickListener(this);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // already registered!
        if(sharedPreferences.getBoolean(Constants.SENT_TOKEN_TO_SERVER, false)){
            Intent intent = new Intent(this, GcmActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            case R.id.button_register:
                String name = nameEditText.getText().toString();
                String email = emailEditText.getText().toString();

                if (name.trim().length() > 1 && email.trim().length() > 1){
                    Intent intent = new Intent(this, GcmActivity.class);
                    intent.putExtra(Constants.NAME, name);
                    intent.putExtra(Constants.EMAIL, email);
                    startActivity(intent);
                }else{
                    Toast.makeText(this, "Error: while validating credentials", Toast.LENGTH_LONG).show();
                }

                break;
            default:
                break;
        }
    }
}
