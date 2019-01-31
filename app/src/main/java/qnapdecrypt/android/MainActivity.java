package qnapdecrypt.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import lib.folderpicker.FolderPicker;
import qnapdecrypt.QNAPFileDecrypterEngine;


public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG_EXTERNAL_STORAGE = "EXTERNAL_STORAGE";
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 1;
    //private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE_PERMISSION = 2;

    private static final int SDCARD_PERMISSION = 1,
            FOLDER_PICKER_CODE = 2,
            FILE_PICKER_CODE = 3;

    private String pick_type=""; //Source or Destination

    private TextView txtInfo;
    private TextView tv_source;
    private TextView tv_destination;
    private CheckBox cb_recursive;
    private Button but_decipher;
    private Button but_reset;

    private static final String PLAIN_NAME_PREFIX = "plain_";
    private QNAPFileDecrypterEngine cipherEngine = new QNAPFileDecrypterEngine(false, false);
    File srcFile;
    File dstFile;
    private List<File> errorFiles = new ArrayList<>();
    private List<File> successFiles = new ArrayList<>();
    private static final String NAME_FILE_REPORT = "HBSUtility_report.txt";
    private boolean recursiveMode = false;
    String password = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtInfo = (TextView) findViewById(R.id.TxtInfo);
        tv_source = (TextView) findViewById(R.id.tv_source);
        tv_destination = (TextView) findViewById(R.id.tv_destination);
        cb_recursive = (CheckBox) findViewById(R.id.cb_recursive);
        but_decipher = (Button) findViewById(R.id.button_decypher);
        but_reset = (Button) findViewById(R.id.button_reset);


        // Check whether this app has write external storage permission or not.
        int writeExternalStoragePermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        // If do not grant write external storage permission.
        if(writeExternalStoragePermission != PackageManager.PERMISSION_GRANTED)
        {
            // Request user to grant write external storage permission.
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
        }


    }


    /*
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            int grantResultsLength = grantResults.length;
            if (grantResultsLength > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "You grant write external storage permission. Please click original button again to continue.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "You denied write external storage permission.", Toast.LENGTH_LONG).show();
            }
        }

    }
    */

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            String location = intent.getExtras().getString("data");

            if(pick_type=="Source") {
                tv_source.setText(location);
                if(tv_destination.getText().toString().equals("")) {
                    txtInfo.setText("Select Destination");
                }
                else if(password=="") {
                    txtInfo.setText("Insert Password");
                }
                else {
                    txtInfo.setText("Ready to go");
                    but_decipher.setEnabled(true);
                }
            }
            else if(pick_type=="Destination") {
                tv_destination.setText(location);

                if(tv_source.getText().toString().equals("")) {
                    txtInfo.setText("Select Source");
                }
                else if(password=="") {
                    txtInfo.setText("Insert Password");
                }
                else {
                    txtInfo.setText("Ready to go");
                    but_decipher.setEnabled(true);
                }

            }

            pick_type="";
        }
    }

    public void selectSourceFolder(View v) {
        pick_type="Source";
        Intent intent = new Intent(this, FolderPicker.class);
        startActivityForResult(intent, FOLDER_PICKER_CODE);

    }

    public void selectSourceFile(View v) {
        pick_type="Source";
        Intent intent = new Intent(this, FolderPicker.class);
        intent.putExtra("pickFiles", true);
        startActivityForResult(intent, FILE_PICKER_CODE);
    }

    public void selectDestinationFolder(View v) {
        pick_type="Destination";
        Intent intent = new Intent(this, FolderPicker.class);
        startActivityForResult(intent, FOLDER_PICKER_CODE);

    }

    public void selectDestinationFile(View v) {
        pick_type="Destination";
        Intent intent = new Intent(this, FolderPicker.class);
        intent.putExtra("pickFiles", true);
        startActivityForResult(intent, FILE_PICKER_CODE);
    }

    public void recursiveMode (View v) {
        recursiveMode=cb_recursive.isChecked();
    }

    public void resetButton (View v) {
        tv_source.setText("");
        tv_destination.setText("");
        cb_recursive.setChecked(false);
        txtInfo.setText("Select Source");
        password="";
        but_decipher.setEnabled(false);
    }

    public void decipherButton(View v) {

        srcFile = new File (tv_source.getText().toString());
        dstFile = new File (tv_destination.getText().toString());

        if (srcFile != null && dstFile != null && srcFile.canRead() && dstFile.canRead()) {
            // Check if directory mode selected
            if (srcFile.isDirectory()) {
                if (dstFile.isDirectory()) {
                    cipherEngine.setDirMode(true);
                    decipher(true, recursiveMode);
                } else {
                    txtInfo.setText("Cannot decipher a directory in a single file, use a directory as destination when the source is a directory.");
                }
            } else {
                cipherEngine.setDirMode(false);
                decipher(false, false);
            }
        } else {
            txtInfo.setText("I/O Error, cannot read source or destination.");
        }

    }


    private void decipher(boolean dirMode, boolean recursiveMode) {
        errorFiles.clear();
        successFiles.clear();

        if (dirMode) {
            decipherMultipleFiles(srcFile, dstFile);
        } else {
            if (srcFile.canRead() && !srcFile.isDirectory()) {
                File outputFile = dstFile;
                if (dstFile.isDirectory()) {
                    if (srcFile.getParentFile().equals(dstFile)) {
                        outputFile = new File(dstFile + File.separator + PLAIN_NAME_PREFIX + srcFile.getName());
                    } else {
                        outputFile = new File(dstFile + File.separator + srcFile.getName());
                    }
                } else if (srcFile.equals(dstFile)) {
                    outputFile = new File(
                            dstFile.getParent() + File.separator + PLAIN_NAME_PREFIX + srcFile.getName());
                }
                if (cipherEngine.doDecipherFile(srcFile, outputFile, password)) {
                    successFiles.add(srcFile);
                } else {
                    errorFiles.add(srcFile);
                }
            }
        }

        but_reset.callOnClick();

        if (errorFiles.isEmpty() && !successFiles.isEmpty()) {
            txtInfo.setText("All files properly deciphered.");
        } else if (!errorFiles.isEmpty() && !successFiles.isEmpty()) {
            txtInfo.setText("Files deciphered but some files failed or be ignored." + System.getProperty("line.separator") +
                    "More informations are available in " + NAME_FILE_REPORT
                    + " file in destination folder.");
        } else if (!errorFiles.isEmpty() && successFiles.isEmpty()) {
            txtInfo.setText("All files fail to deciphered." + System.getProperty("line.separator") + "More informations are available in "
                    + NAME_FILE_REPORT + " file in destination folder.");
        } else if (errorFiles.isEmpty() && successFiles.isEmpty()) {
            txtInfo.setText("No files to decipher.");
        }

        writeReportFile();

    }

    private void decipherMultipleFiles(File cipherFile, File plainFile) {
        File cipherDir = cipherFile;
        File plainDir = plainFile;
        String[] cipheredListFiles = cipherDir.list();

        for (String eachCipheredFileName : cipheredListFiles) {
            String eachPlainFileName = eachCipheredFileName;
            if (cipherDir.equals(plainDir)) {
                eachPlainFileName = PLAIN_NAME_PREFIX + eachCipheredFileName;
            }
            File eachCipherFile = new File(cipherDir + File.separator + eachCipheredFileName);
            File eachPlainFile = new File(plainDir + File.separator + eachPlainFileName);

            // Check recursive mode
            if (recursiveMode && eachCipherFile.isDirectory() && eachCipherFile.canRead()) {
                String newPlainDir = plainDir + File.separator + eachPlainFileName;
                String newCipherDir = cipherDir + File.separator + eachCipheredFileName;

                    File newPDir = new File (newPlainDir);
                    if(!newPDir.isDirectory()){
                        newPDir.mkdir();
                    }

                    if (!newPDir.exists()||!newPDir.isDirectory()) {
                        errorFiles.add(eachCipherFile);
                    }
                    else {
                        decipherMultipleFiles(new File(newCipherDir), new File(newPlainDir));
                    }

            } else {
                if (!eachCipherFile.isDirectory() && eachCipherFile.canRead()) {
                    if (cipherEngine.doDecipherFile(eachCipherFile, eachPlainFile, password)) {
                        successFiles.add(eachCipherFile);
                    } else {
                        errorFiles.add(eachCipherFile);
                    }
                }
            }

            /*
            // Cannot set progress in recursive mode, cannot predict how many files are
            // deciphered
            if (!recursiveMode) {
                int progress = (int) (((float) ++index / cipheredListFiles.length) * 100);
                setProgress(progress);
            }
            */
        }
    }


    public void passwordButton(View v){

        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.password_prompt, null);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.userPassword);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                password = userInput.getText().toString();
                                if (!password.equals("")) {
                                    if(tv_source.getText().toString().equals("")) {
                                        txtInfo.setText("Select Source");
                                    }
                                    else if(tv_destination.getText().toString().equals("")) {
                                        txtInfo.setText("Select Destination");
                                    }
                                    else {
                                        txtInfo.setText("Ready to go");
                                        but_decipher.setEnabled(true);
                                    }
                                }
                                else{
                                    txtInfo.setText("Password invalid");
                                }
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

    private void writeReportFile() {
        // Write errors in file
        File resFile;
        if (dstFile.isDirectory()) {
            resFile = new File(dstFile.getAbsolutePath() + File.separator + NAME_FILE_REPORT);
        } else {
            resFile = new File(dstFile.getParentFile().getAbsolutePath() + File.separator + NAME_FILE_REPORT);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Files in success for decipher operations in Hybrid Backup Sync utility :"
                + System.lineSeparator() + System.lineSeparator());
        for (File eachSuccessPath : successFiles) {
            builder.append(eachSuccessPath.getAbsolutePath() + System.lineSeparator());
        }
        builder.append(System.lineSeparator() + "----------------" + System.lineSeparator() + System.lineSeparator());
        builder.append("Files in error for decipher operations in Hybrid Backup Sync utility :" + System.lineSeparator()
                + System.lineSeparator());
        for (File eachErrorPath : errorFiles) {
            builder.append(eachErrorPath.getAbsolutePath() + System.lineSeparator());
        }

        byte[] buf = builder.toString().getBytes();
        try {
            FileOutputStream outstream = new FileOutputStream(resFile);
            outstream.write(buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
