package com.cjg.CheckMyPay;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

import android.os.Environment;
import android.util.Log;
import android.widget.*;

public class FileOperations {
    private class fViewHolder {
        TextView text;
    }
    public int fIX=-1;

    public FileOperations() {

    }
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public int write(String fname, String fcontent){
        FileWriter fw=null;
        if ((fcontent == null)||(Objects.equals(fcontent, ""))) return 1;
        File file = new File(fname);
        // If file does not exists, then create it
        if (!file.exists())
            try {file.createNewFile();}
            catch (IOException e) {
                Log.d("fail","create");return -1;}
        try {fw = new FileWriter(file.getAbsoluteFile());}
        catch (IOException e) {
            Log.d("fail","write open");return -1;}
        BufferedWriter bw = new BufferedWriter(fw);
        try {
            bw.write(fcontent);
            bw.close();
        } catch (IOException e) {
            Log.d("fail","write");return -1;}
        Log.d("Success","Success");
        return 0;
    }
    public String read(String fname){

        BufferedReader br;
        String response;

        try {

            StringBuilder output = new StringBuilder();
            //String fpath = "/sdcard/"+fname+".txt";

            br = new BufferedReader(new FileReader(fname));
            String line;
            while ((line = br.readLine()) != null) {
                output.append(line).append(System.getProperty("line.separator"));
            }
            response = output.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return null;

        }
        return response;
    }
    /*
        public String readLine(String fname){

            BufferedReader br = null;
            String response = null;

            try {

                StringBuilder output = new StringBuilder();

                br = new BufferedReader(new FileReader(fname));
                response = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return null;

            }
            return response;
        }

    */
    public int find(File dir, MainActivity.FileListAdapter fla, boolean recursive) {

        File[] listFile = dir.listFiles();
        int cnt=0;

        if (listFile != null) {
            for (File aListFile : listFile) {
                fla.add(aListFile);
                cnt++;
                if (aListFile.isDirectory() & recursive) {
                    find(aListFile, fla, recursive);   //recursive call to sub-dir
                }
            }
        }
        if (cnt>0) fla.notifyDataSetChanged();
        return cnt;
    }
/*    public void txtEd(String fname) {

        super.setContentView(layout.text_editor);
        pathView = findViewById(id.fileBox);
        pathView.setText(path);
        TextView temp = findViewById(id.pathText);
        temp.setText(getObbDir().toString());

    }
*/
}