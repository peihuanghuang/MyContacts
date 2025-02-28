package com.example.mycontacts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.Manifest;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
    ActivityResultCallback {

  private ActivityResultLauncher requestPermissionLauncher;
  private String TAG = "displayPhoneContacts";
  private final int DISPLAY_CONTACTS_BUTTON_TAG = 0;
  private final int SIGN_IN_ACCOUNT_BUTTON_TAG = 1;
  private final int ADD_CONTACTS = 2;
  private ContentResolver contentResolver;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_main);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
      Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
      return insets;
    });
    Button contacts = findViewById(R.id.button);
    contacts.setOnClickListener(this::onClick);
    Button signInAccount = findViewById(R.id.button2);
    signInAccount.setOnClickListener(this::onClick);
    requestPermissionLauncher = registerForActivityResult(
        new ActivityResultContracts.RequestPermission(), this);
    if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
        != PackageManager.PERMISSION_GRANTED) {
      // Use the traditional way to request permissions and then implement onRequestPermissionsResult() to
      // handle grants.
      // ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 0);
      // Alternative, use Androidx ActivityResultContracts.RequestPermission()
      requestPermissionLauncher = registerForActivityResult(
          new ActivityResultContracts.RequestPermission(), this);
      if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
        showAlertDialog();
      } else {
        requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
      }
    }
    contentResolver = getBaseContext().getContentResolver();
  }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, int deviceId) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId);
//    }

  private void displayPhoneContacts() {
    try (Cursor rcur = contentResolver.query(ContactsContract.RawContacts.CONTENT_URI, null, null,
        null, null)) {
      assert rcur != null;
      while (rcur.moveToNext()) {
        Log.i(TAG, "User Account name: " + rcur.getString(
            rcur.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME)));
        Log.i(TAG, "User Account type: " + rcur.getString(
            rcur.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE)));
      }
    }
    try (Cursor cur = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
        null, null, null, null)) {
      assert cur != null;
      if (cur.getCount() > 0) {
        while (cur.moveToNext()) {
          String id = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
          String name = cur.getString(
              cur.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
          Cursor rawCur = contentResolver.query(RawContacts.CONTENT_URI, null,
              RawContacts.CONTACT_ID + "=" + id, null, null);
          if (rawCur.getCount() > 0) {
            while(rawCur.moveToNext()) {
              Log.i(TAG, "User Account name: " + rawCur.getString(
                  rawCur.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME)));
              Log.i(TAG, "User Account type: " + rawCur.getString(
                  rawCur.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE)));
            }
          }
          if (Integer.parseInt(cur.getString(
              cur.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
            Cursor pCur = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{id}, null);
            while (true) {
              assert pCur != null;
              if (!pCur.moveToNext()) {
                break;
              }
              String phoneNo = pCur.getString(
                  pCur.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
              Log.i("displayPhoneContacts: ", "Name: " + name + ", Phone No: " + phoneNo);
            }
            pCur.close();
            pCur = contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                new String[]{id}, null);
            while (true) {
              assert pCur != null;
              if (!pCur.moveToNext()) {
                break;
              }
              String email = pCur.getString(
                  pCur.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS));
              Log.i("displayPhoneContacts: ", "Name: " + name + ", Email: " + email);
            }

          }
        }
      }
    }
    Toast.makeText(this, "Please check logcat", Toast.LENGTH_SHORT).show();
  }

  private String getSignInAccount() {
    AccountManager manager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
    Account[] list = manager.getAccounts();
    for (Account account :
        list) {
      if (account.type.equals("com.google")) {
        Log.i(TAG, String.format("getSignInAccount: %s", account.name));
        return account.name;
      }
    }
    return "hearables.test@google.com";
  }

  private void addContacts(String name, String phone) {

  }

  @Override
  public void onClick(View v) {
    Integer tag = Integer.parseInt(v.getTag().toString());
    switch (tag) {
      case DISPLAY_CONTACTS_BUTTON_TAG:
        displayPhoneContacts();
        break;
      case SIGN_IN_ACCOUNT_BUTTON_TAG:
        Toast.makeText(this, getSignInAccount(), Toast.LENGTH_SHORT).show();
        break;
      case ADD_CONTACTS:

    }
  }

  @Override
  public void onActivityResult(Object o) {
    boolean granted = (boolean) o;
    if (!granted) {
      finishAndRemoveTask();
    }
  }

  private void showAlertDialog() {
    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
    alertDialogBuilder.setMessage(
        "This app needs you to allow this permission in order to function.Will you allow it");
    alertDialogBuilder.setPositiveButton("Yes",
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface arg0, int arg1) {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
          }
        });
    alertDialogBuilder.show();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }
}