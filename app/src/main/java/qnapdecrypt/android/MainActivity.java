package qnapdecrypt.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import lib.folderpicker.FolderPicker;
import qnapdecrypt.QNAPFileDecrypterEngine;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 1;

    private static final int //SDCARD_PERMISSION = 1,
            FOLDER_PICKER_CODE = 2,
            FILE_PICKER_CODE = 3;

    private String pick_type=""; //Source or Destination

    private TextView txtInfo;
    private TextView tv_source;
    private TextView tv_destination;
    private CheckBox cb_recursive;
    private Button but_decipher;
    private Button but_reset;
    private ProgressBar progressBar;

    private static final String PLAIN_NAME_PREFIX = "plain_";
    private QNAPFileDecrypterEngine cipherEngine = new QNAPFileDecrypterEngine(false, false);
    File srcFile;
    File dstFile;
    private List<File> errorFiles = new ArrayList<>();
    private List<File> successFiles = new ArrayList<>();
    private static final String NAME_FILE_REPORT = "HBSUtility_report.txt";
    private boolean recursiveMode = false;
    String password = "";
    int index = 0;
    int progress=0;

    Handler progressBarHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set the icon into the Action Bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.mipmap.ic_launcher_cloudlock);
        }

        txtInfo = (TextView) findViewById(R.id.TxtInfo);
        tv_source = (TextView) findViewById(R.id.tv_source);
        tv_destination = (TextView) findViewById(R.id.tv_destination);
        cb_recursive = (CheckBox) findViewById(R.id.cb_recursive);
        but_decipher = (Button) findViewById(R.id.button_decypher);
        but_reset = (Button) findViewById(R.id.button_reset);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        // Check whether this app has write external storage permission or not.
        int writeExternalStoragePermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        // If do not grant write external storage permission.
        if(writeExternalStoragePermission != PackageManager.PERMISSION_GRANTED)
        {
            // Request user to grant write external storage permission.
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
        }

    }


    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:

                final Dialog d=new Dialog(this);
                d.setTitle("About");
                d.setContentView(R.layout.about);

                //Homepage
                TextView gitView =(TextView) d.findViewById(R.id.gitpage_link);
                gitView.setMovementMethod(LinkMovementMethod.getInstance());

                //Feedback
                TextView issueView =(TextView) d.findViewById(R.id.issue_link);
                issueView.setMovementMethod(LinkMovementMethod.getInstance());

                //Credit Icon
                Spanned icon = Html.fromHtml(getString(R.string.iconweb));
                TextView about_icon = (TextView)d.findViewById(R.id.about_icon);
                about_icon.setText(icon);
                about_icon.setMovementMethod(LinkMovementMethod.getInstance());

                //Credit Mikiya
                Spanned mikiya = Html.fromHtml(getString(R.string.mikiya_web));
                TextView about_mikiya = (TextView)d.findViewById(R.id.about_mikiya);
                about_mikiya.setText(mikiya);
                about_mikiya.setMovementMethod(LinkMovementMethod.getInstance());

                //Credit kashifo
                Spanned kashifo = Html.fromHtml(getString(R.string.kashifo_web));
                TextView about_kashifo = (TextView)d.findViewById(R.id.about_kashifo);
                about_kashifo.setText(kashifo);
                about_kashifo.setMovementMethod(LinkMovementMethod.getInstance());

                Button okButton = (Button) d.findViewById(R.id.about_button);
                okButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View arg0) {
                        d.dismiss();
                    }
                });

                d.show();

                return true;
        }

        return super.onOptionsItemSelected(item);
        //return false;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK) {

            if((requestCode==FOLDER_PICKER_CODE)||(requestCode==FILE_PICKER_CODE)) {

                String location = intent.getExtras().getString("data");

                if (pick_type.equals("Source")) {
                    tv_source.setText(location);
                    checkFields();
                } else if (pick_type.equals("Destination")) {
                    tv_destination.setText(location);
                    checkFields();
                }

                pick_type = "";
            }

        }
    }

    private void checkFields(){
        if (tv_source.getText().toString().equals("")) {
            txtInfo.setText("Select Source");
        }
        else if (tv_destination.getText().toString().equals("")) {
            txtInfo.setText("Select Destination");
        }
        else {
            txtInfo.setText("Ready to go");
            but_decipher.setEnabled(true);
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
        intent.putExtra("title", "Select File");
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
        intent.putExtra("title", "Select File");
        startActivityForResult(intent, FILE_PICKER_CODE);
    }

    public void recursiveMode (View v) {
        recursiveMode=cb_recursive.isChecked();
    }

    public void resetButton (View v) {
        tv_source.setText("");
        tv_destination.setText("");

        cb_recursive.setChecked(false);

        but_decipher.setEnabled(false);
        but_reset.setEnabled(true);

        txtInfo.setText("Select Source");
        txtInfo.setTextColor(getResources().getColor(R.color.infoDefault));

        progressBar.setVisibility(View.GONE);
        progressBar.setIndeterminate(true);
        progressBar.setProgress(1);

        index=0;
        progress=0;
        password="";

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
                    txtInfo.setTextColor(getResources().getColor(R.color.infoAlert));
                    txtInfo.setText("Cannot decipher a directory in a single file, use a directory as destination when the source is a directory.");
                }
            } else {
                cipherEngine.setDirMode(false);
                decipher(false, false);
            }
        } else {
            txtInfo.setTextColor(getResources().getColor(R.color.infoAlert));
            txtInfo.setText("I/O Error, cannot read source or destination.");
        }

    }


    private void decipher(final boolean dirMode, final boolean recursiveMode) {
        errorFiles.clear();
        successFiles.clear();

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
                                //check for empty password
                                if (password.equals("")) {
                                    Toast.makeText(MainActivity.this, "Password cannot be empty!", Toast.LENGTH_LONG).show();
                                    //Re-call dialog
                                    decipher(dirMode,recursiveMode);
                                }
                                else {
                                    dialog.dismiss();
                                    but_decipher.setEnabled(false);
                                    but_reset.setEnabled(false);
                                    progressBar.setVisibility(View.VISIBLE);
                                    txtInfo.setTextColor(getResources().getColor(R.color.infoWorking));

                                    // Cannot set progress in recursive mode, cannot predict how many files are
                                    // deciphered
                                    if(dirMode && recursiveMode){
                                        txtInfo.setText("Patience, in recursive mode, progress cannot be measured ...");
                                    }
                                    else {
                                        txtInfo.setText("Deciphering . . .   ");
                                    }

                                    new Thread(new Runnable() {
                                        public void run() {

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

                                            //Update GUI objects
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    but_reset.callOnClick();

                                                    String result="...no result...";
                                                    txtInfo.setTextColor(getResources().getColor(R.color.infoAlert));

                                                    if (errorFiles.isEmpty() && !successFiles.isEmpty()) {
                                                        result="All files properly deciphered.";
                                                        txtInfo.setTextColor(getResources().getColor(R.color.infoOK));

                                                    } else if (!errorFiles.isEmpty() && !successFiles.isEmpty()) {
                                                        result="Files deciphered but some files failed or be ignored." + System.getProperty("line.separator") +
                                                                "More informations are available in " + NAME_FILE_REPORT
                                                                + " file in destination folder.";

                                                    } else if (!errorFiles.isEmpty() && successFiles.isEmpty()) {
                                                        result="All files fail to deciphered." + System.getProperty("line.separator") + "More informations are available in "
                                                                + NAME_FILE_REPORT + " file in destination folder.";

                                                    } else if (errorFiles.isEmpty() && successFiles.isEmpty()) {
                                                        result="No files to decipher.";

                                                    }

                                                    txtInfo.setText(result);

                                                }

                                            });

                                            writeReportFile();

                                        }
                                    }).start();

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


    private void decipherMultipleFiles(File cipherFile, File plainFile) {
        File cipherDir = cipherFile;
        File plainDir = plainFile;
        String[] cipheredListFiles = cipherDir.list();


        if(!recursiveMode) {
            runOnUiThread(new Runnable() {
                public void run() {
                    progressBar.setIndeterminate(false);
                }
            });
        }


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

                File newPDir = new File(newPlainDir);
                boolean newPDirRes=false;
                if (!newPDir.isDirectory()) {
                    newPDirRes=newPDir.mkdir();
                }

                if (!newPDirRes || !newPDir.exists() || !newPDir.isDirectory()) {
                    errorFiles.add(eachCipherFile);
                } else {
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



            // Cannot set progress in recursive mode, cannot predict how many files are
            // deciphered
            if (!recursiveMode) {
                progress = (int) (((float) ++index / cipheredListFiles.length) * 100);

                progressBarHandler.post(new Runnable() {
                    public void run() {
                        progressBar.setProgress(progress);
                        txtInfo.setText("Deciphering . . .   "+ progress + "%");
                    }
                });

            }

        }

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
