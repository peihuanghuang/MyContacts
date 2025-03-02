package com.example.mycontacts;

import android.Manifest.permission;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.Manifest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
    ActivityResultCallback {

  private ActivityResultLauncher requestPermissionLauncher;
  private String TAG = "displayContacts";
  private final int DISPLAY_CONTACTS_BUTTON_TAG = 0;
  private final int SIGN_IN_ACCOUNT_BUTTON_TAG = 1;
  private final int ADD_CONTACTS_BUTTON_TAG = 2;
  private final int DELETE_CONTACTS_BUTTON_TAG = 3;
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
    Button listContacts = findViewById(R.id.button);
    listContacts.setOnClickListener(this::onClick);
    Button signInAccount = findViewById(R.id.button2);
    signInAccount.setOnClickListener(this::onClick);
    Button addContacts = findViewById(R.id.button3);
    addContacts.setOnClickListener(this::onClick);
    Button deleteContacts = findViewById(R.id.button4);
    deleteContacts.setOnClickListener(this::onClick);
    if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
        != PackageManager.PERMISSION_GRANTED ||
        checkSelfPermission(permission.WRITE_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
      /*
      Use the traditional way to request permissions and then implement onRequestPermissionsResult() to
      handle grants.
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 0);
      */
      /*
      Alternative, use Androidx ActivityResultContracts.RequestPermission()
      AppCompactActivity class implements ActivityResultCaller interface,
      https://developer.android.com/reference/androidx/activity/result/ActivityResultCaller
      Call ActivityResultCaller#registerForActivityResult() with one of two usages below.
      The first parameter is an instance of ActivityResultContracts.RequestPermission()
      1. request one permission.
      The second parameter is the callback which will be called by system to handle
      the result granted by users by an UI grant. The system will pass a boolean value to your
      callback.
      onActivityResult(Object o){
      boolean isGranted = (boolean)o;
      ...
      ...
      }
      2. request multiple permissions.
      The second parameter is the callback which will be called by system to handle
      the result granted by users by an UI grant. The system will pass an instance of
      the LinkedHashMap, the keys are Manifest.permission, like android.permission.WRITE_CONTACTS,
      the value is boolean, to your callback.
      onActivityResult(Object o){
      LinkedHashMap isGranted = (LinkedHashMap)o;
      if (isGranted.get(key)) {
        ...
        ...
        }
      }
      */
      requestPermissionLauncher = registerForActivityResult(
          new ActivityResultContracts.RequestMultiplePermissions(), this);
      if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CONTACTS)) {
        showAlertDialog();
      } else {
        requestPermissionLauncher.launch(new String[]{Manifest.permission.WRITE_CONTACTS,
            permission.READ_CONTACTS});
      }
    }
    contentResolver = getBaseContext().getContentResolver();
    List<Contact> contacts = queryContactsByName("Jason");
  }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, int deviceId) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId);
//    }

  private void displayContacts() {
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
            while (rawCur.moveToNext()) {
              Log.i(TAG, "User Account name: " + rawCur.getString(
                  rawCur.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME)));
              Log.i(TAG, "User Account type: " + rawCur.getString(
                  rawCur.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE)));
            }
          }
          rawCur.close();
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
              Log.i("displayContacts: ", "Name: " + name + ", Phone No: " + phoneNo);
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
              Log.i("displayContacts: ", "Name: " + name + ", Email: " + email);
            }
            pCur.close();

          }
        }
      }
      cur.close();
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
    return "tl4a@gmail.com";
  }

  private void insertContact(String contactName, String phoneNumber) {
    Cursor cursor = contentResolver.query(Contacts.CONTENT_URI, null, null, null, null);
    assert cursor != null;
    while (cursor.moveToNext()) {
      String contactDisplayName = cursor.getString(
          cursor.getColumnIndexOrThrow(Contacts.DISPLAY_NAME));
      if (contactDisplayName.equals(contactName)) {
        Toast.makeText(this, String.format("The contact, %s, already exists", contactName),
            Toast.LENGTH_SHORT).show();
        return;
      }
    }
    ArrayList<ContentProviderOperation> insertOperations = new ArrayList<>();
    int rawContactInsertIndex = insertOperations.size();
    insertOperations.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
        .withValue(RawContacts.ACCOUNT_NAME, getSignInAccount())
        .withValue(RawContacts.ACCOUNT_TYPE, "com.google").build());
    insertOperations.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
        .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
        .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
        .withValue(StructuredName.DISPLAY_NAME, contactName).build());
    insertOperations.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
        .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
        .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
        .withValue(Phone.NUMBER, phoneNumber)
        .withValue(Phone.TYPE, Phone.TYPE_MOBILE).build());
    try {
      contentResolver.applyBatch(ContactsContract.AUTHORITY, insertOperations);
      Toast.makeText(this, String.format("The contact, %s, is successfully added!", contactName),
          Toast.LENGTH_LONG).show();
    } catch (OperationApplicationException e) {
      throw new RuntimeException(e);
    } catch (RemoteException e) {
      throw new RuntimeException(e);
    }

  }

  public void deleteContact(String contactName) {
    String where = String.format("%s = ? ", Data.DISPLAY_NAME);
    String[] parameters = new String[]{contactName};

    ArrayList<ContentProviderOperation> deleteOperations = new ArrayList<>();
    deleteOperations.add(ContentProviderOperation.newDelete(RawContacts.CONTENT_URI)
        .withSelection(where, parameters).build());

    ContentProviderResult[] result;
    try {
      result = getContentResolver().applyBatch(ContactsContract.AUTHORITY, deleteOperations);
    } catch (OperationApplicationException e) {
      throw new RuntimeException(e);
    } catch (RemoteException e) {
      throw new RuntimeException(e);
    }

    if (result[0].count != 0) {
      Toast.makeText(this, String.format("Deleted the contact, %s, successfully!", contactName),
          Toast.LENGTH_LONG).show();
    } else {
      Toast.makeText(this, String.format("The contact, %s, doesn't exist!", contactName),
          Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public void onClick(View v) {
    Integer tag = Integer.parseInt(v.getTag().toString());
    switch (tag) {
      case DISPLAY_CONTACTS_BUTTON_TAG:
        displayContacts();
        break;
      case SIGN_IN_ACCOUNT_BUTTON_TAG:
        Toast.makeText(this, getSignInAccount(), Toast.LENGTH_SHORT).show();
        break;
      case ADD_CONTACTS_BUTTON_TAG:
        insertContact("Mother", "0916590968");
        break;
      case DELETE_CONTACTS_BUTTON_TAG:
        deleteContact("Mother");
    }
  }

  @Override
  public void onActivityResult(Object o) {
    LinkedHashMap<String, Boolean> granted = (LinkedHashMap<String, Boolean>) o;
    if (!granted.get("android.permission.WRITE_CONTACTS") || !granted.get(
        "android.permission.READ_CONTACTS")) {
      finishAndRemoveTask();
    }
  }

  private void showAlertDialog() {
    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
    alertDialogBuilder.setMessage(
        "This app needs you to allow this permission in order to function.Will you allow it");
    alertDialogBuilder.setPositiveButton("Yes",
        (arg0, arg1) -> {
          requestPermissionLauncher.launch(permission.READ_CONTACTS);
        });
    alertDialogBuilder.show();
  }

  private List<Contact> queryContactsByName(String name) {
    String where = String.format("%s = ?", Contacts.DISPLAY_NAME);
    List<Contact> contacts = new ArrayList<>();
    List<String> contactPhoneNumbers = new ArrayList<>();
    Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, where,
        new String[]{name}, null);
    while (cursor.moveToNext()) {
      String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
      if (Integer.parseInt(cursor.getString(
          cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
        contactPhoneNumbers = getContactPhoneNumbers(id);
      }
      contacts.add(Contact.builder()
          .setDisplayName(cursor.getString(cursor.getColumnIndexOrThrow(Contacts.DISPLAY_NAME)))
          .setAccount(getContactAccount(id).get())
          .setPhoneNumber(contactPhoneNumbers).build());
    }
    cursor.close();
    return contacts;
  }

  private Optional<String> getContactAccount(String contactId) {
    Cursor rawCursor = contentResolver.query(RawContacts.CONTENT_URI, null,
        String.format("%s = %s", RawContacts.CONTACT_ID, contactId), null, null);
    while (rawCursor.moveToNext()) {
      return Optional.of(
          rawCursor.getString(rawCursor.getColumnIndexOrThrow(RawContacts.ACCOUNT_NAME)));
    }
    return Optional.empty();
  }

  private List<String> getContactPhoneNumbers(String contactId) {
    List<String> phoneNumbers = new ArrayList<>();
    Cursor phoneCursor = contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        null,
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
        new String[]{contactId}, null);
    while (phoneCursor.moveToNext()) {
      String phoneNo = phoneCursor.getString(
          phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
      phoneNumbers.add(phoneNo);
      Log.i("getContactPhoneNumber: ", String.format("Phone No: %s", phoneNo));
    }
    phoneCursor.close();
    return phoneNumbers;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }
}