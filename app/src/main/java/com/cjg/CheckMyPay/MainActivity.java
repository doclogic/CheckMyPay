package com.cjg.CheckMyPay;
// Written by Cris Graue * All Rights Reserved * 2018
// CJGTime orig project name
// ver 0.1 menu breakout
// ver 0.2 data file for pay rates, tip factoring
// ver 0.3 job code table
// ver 1.01 rename to cjg.pay_check
// ver 1.0x remove "_" and "-" now PayCheck and paycheck
// ver 1.08 paycheck(s) data file
// ver 1.09 layout rework
// ver 1.10 lineItem stamp now Date not String
// ver 1.11 Check details development
// 2022/07/20 ver 1.12 Update to newer androids / compile rebuild newer versions SDK / code cleanup
// 2022/07/23 ver 1.12 no longer cjg.paycheck package now com.cjg.CheckMyPay

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;

import android.os.Bundle;
import android.os.Environment;

import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import static java.lang.Double.valueOf;
import static com.cjg.CheckMyPay.R.id.*;
import static com.cjg.CheckMyPay.R.layout;
import static com.cjg.CheckMyPay.R.string;

class lineItem {
    Date stamp;
    String text;
    public Date getStamp() { return stamp; }
    public String getText() { return text; }
}
class liViewHolder {
    TextView stamp;
    TextView text;
}
class PCItem {
    Date chkDate;
    String chkNum;
    double chkAmt;
    Date perBeg, perEnd;
}
class pcViewHolder {
    TextView cdate;
    TextView num;
    TextView amt;
    TextView bdate;
    TextView edate;
}
class PCDetail {
    String text;
    double rate, hrs, amt;
}
class pdViewHolder {
    TextView text;
    TextView rate;
    TextView hrs;
    TextView amt;
}
class TimeItem {
    Date in, out, bst, bend;
    double hrs, rate, total;

    public Date getIn() {
        return in;
    }
    public Date getOut() {
        return out;
    }
    public Date getBst() {
        return bst;
    }
    public Date getBend() {
        return bend;
    }
    public double getHrs() {
        return hrs;
    }
    public double getRate() {
        return rate;
    }
    public double getTotal() {
        return total;
    }
}
class tViewHolder {
    TextView separator;
    TextView in,bst,bend,out;
    TextView hrs;
    TextView rate;
    TextView total;
}
class CodeItem {
    int cdNum;
    String cdName;
    double cdRate;
}
class cViewHolder {
    TextView cdNum;
    TextView cdName;
    TextView cdRate;
}
class fViewHolder {
    TextView fpath;
    TextView fname;
    TextView fabs;
    TextView fattr;
    TextView fsize;
}

public class MainActivity extends AppCompatActivity {
    private FileOperations fop;
    private static final int DM_FILE = 1, DM_TIME = 2, DM_TOTS = 4, DM_TEXT = 8, DM_SAVE = 16,
            DM_EDIT = 32, DM_JOB = 64, DM_DIV = 128, DM_CHK = 256, DM_CHKDET = 512;
    private static final String F_SEP = System.getProperty("file.separator");
    private int dMode = 0;
    private Integer stix = -1, trix = -1, fiix = -1, cix = -1, pcix = -1, pdix = -1, cdix = -1;
    private StampListAdapter sla;
    private TimeListAdapter tla;
    private CodeListAdapter cla;
    private FileListAdapter fla;
    private PCListAdapter pla;
    private PdListAdapter cdla;
    private ListView slv, tlv, clv, flv, plv, cdlv;

    private Boolean slMod = false;
    private lineItem mit = null;
    private lineItem lastMsg = null;
    private CodeItem jit = null;
    private PCItem pit = null;
    PCDetail cdit = null;
    private String fileContent = "";
    private String curDir = "", curFileDir = "", curObbDir = "", curDataDir = "", curPubDir = "";
    private String curFile = "", curName = "", gloFile = "", tempfile = "", chkFile = "";
    private Integer totline = -1;
    private double curRate = 0.0;
    private double curDiv = 1.0;
    private double curTip = 0.0, curTipCash = 0.0;
    private TextView prompt_v = null;
    private TextView fn_v = null;
    private EditText dt_v = null;
    private EditText m_v = null;
    private EditText div_v = null;
    private EditText EdTxt_v = null;
    private EditText dir_v = null;
    private EditText fname_v = null;
    private EditText jc_v, jn_v, jr_v;
    EditText pcDate, pbDate, peDate, pAmt, pNum;

    private Toast toast;

    // onCreate() onStart() onResume()      onRestart() only before onStart (not in create chain)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_load);
        assert F_SEP != null;  //gotta have something
        TextView Stat_v = findViewById(Status_Line);
        Stat_v.setText("Status_Line");

        if (sla == null) {
            Stat_v.setText("Loading Time Stamp data..."); sla = new StampListAdapter();
        }
        if (cla == null) {
            Stat_v.setText("Loading Code List..."); cla = new CodeListAdapter();
        }
        if (fla == null) {
            Stat_v.setText("Loading file list..."); fla = new FileListAdapter();
        }
        if (fop == null) {
            Stat_v.setText("File Operations init..."); fop = new FileOperations();
        }
        if (pla == null) {
            Stat_v.setText("PC list"); pla = new PCListAdapter();
        }
        if (cdla == null) {
            Stat_v.setText("pd list"); cdla = new PdListAdapter();
        }
        curFileDir = getFilesDir().toString();
        curObbDir = getObbDir().toString();
        curDataDir = "x"; //getDataDir().toString();
        curPubDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString();
/*		Map<String,String> emap;
		emap = System.getenv();
		simpleToast(emap,1);
        getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS); */
        rdGlobal();
        dMode = DM_CHK;         ///start with check list list
        if (savedInstanceState != null) {
//            simpleToast("Restore Instance", 1);
            tempfile = curFileDir + F_SEP + getString(string.f_tempName);
            rdTime(tempfile);
            File d = new File(tempfile);
            d.delete();
            stix = (Integer) savedInstanceState.get("stix");
            trix = (Integer) savedInstanceState.get("trix");
            dMode = (int) savedInstanceState.get("dMode");
            if ((dMode & DM_TEXT) > 0) {          //if we were editing text, reload temp save
                tempfile = curFileDir + F_SEP + getString(string.f_tempTxtName);
                setContentView(layout.text_editor);
                EdTxt_v = findViewById(textBox);
                EdTxt_v.setText(fop.read(tempfile));
                new File(tempfile).delete();
            }
        }
        if (pla.size() < 1)         //If nothing there, go load it up...
            rdChecks();
        if (curFile.length() < 1)       //If no file set, build it...
            curFile = curFileDir + F_SEP + getString(string.f_defName);
        if (sla.size() < 1)         //If nothing there, go load it up...
            rdTime(curFile);              //try load
        if ((dMode & DM_FILE) > 0)                          //still in file select mode
            selectFile("Select", curFile);         //return to file select
        else
            switch (dMode) {            //Set to current display mode
                case DM_TIME: disTime();    break;
                case DM_TOTS: disTot();     break;
                case DM_TEXT: disTxtEd();   break;
                case DM_EDIT: disEd();      break;
                case DM_JOB:  disJob();     break;
                case DM_DIV:  disDiv();     break;
                case DM_CHK:  disChk();     break;
                default:
                    disMain();
            }
//        simpleToast("End onCreate", 0);
    }

//onPause() onStop() onDestroy()

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
//        simpleToast("save instance", 0);
        super.onSaveInstanceState(outState);
        outState.putInt("stix", stix);
        outState.putInt("trix", trix);
        outState.putInt("dMode", dMode);
        wrtGlobal();
        if ((dMode & DM_TEXT) > 0) {
            tempfile = curFileDir + F_SEP + getString(R.string.f_tempTxtName);
            EdTxt_v = findViewById(textBox);
            fop.write(tempfile, EdTxt_v.getText().toString());
        }
        curFile = curFileDir + F_SEP + getString(R.string.f_tempName);
        if (slMod) wrtTime(curFile);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        getMenuInflater().inflate(R.menu.menu_file, menu);
        getMenuInflater().inflate(R.menu.menu_entry, menu);
        return true;
    }

    @Override

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case file_period: endPeriod();break;
            case file_load: loadOther();break;
            case file_save: saveOther();break;
            case file_checks: disChk();break;
            case file_tx_ed: setTxtEd();break;
            case file_clear: appClear();break;
            case file_reset: appReset();break;
            case entry_tot: totTime();break;
            case entry_insert: insStamp();break;
            case entry_code: disJob();break;
            case entry_del: delStamp();break;
            case entry_clear: clrStamp();break;
            case entry_edit: editStamp();break;
            case entry_shup: shiftUpStamp();break;
            case action_exit: appExit();break;
            case action_quit: appQuit();break;
            case action_break:
                simpleToast("Break", 0);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /***MAIN DISPLAY***********************************************************************************/
    public void disMain() {
        setContentView(layout.activity_main);
        new TextClock(this);

    }
    /***TIME DISPLAY***********************************************************************************/
    private void disTime() {
        setContentView(R.layout.time_main);
        new TextClock(this);
        fn_v = findViewById(fnView);
        fn_v.setText(curName);
        kbSet(false);
        slv = findViewById(stampList);
        if (sla == null) sla = new StampListAdapter();
        slv.setAdapter(sla);
        if ((stix > -1) && (stix < sla.size())) slv.setSelection(stix);
        slv.requestFocus();
        sla.notifyDataSetChanged();
        slv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // entry selected from listview of items
                if (toast != null) toast.cancel();
                stix = position;
                slv.setSelection(stix);
                sla.notifyDataSetChanged();
            }
        });
        dMode = DM_TIME;
    }
    public void tInButton(View view) {
        wrtStamp(getString(string.in));
    }
    public void tOutButton(View view) {
        wrtStamp(getString(string.out));
    }
    public void tBreakStButton(View view) {
        wrtStamp(getString(string.break_str) + " " + getString(string.brst));
    }
    public void tBreakEndButton(View view) {
        wrtStamp(getString(string.break_str) + " " + getString(string.brendt));
    }
    public void tAddButton(View view) {
        wrtStamp("");
        editStamp();
    }
    public void tEditButton(View view) {
        editStamp();
    }
    public void tDoneButton(View view) {
        if ( slMod ) wrtTime(curFile);
        dMode = DM_CHK;
        disChk();
    }
    public void tUpButton(View view) {
        shiftUpStamp();
    }
    public void tDownButton(View view) {
        shiftDownStamp();
    }
    /***TOTALS DISPLAY*********************************************************************************/
    private void disTot() {
        setContentView(R.layout.tot_main);
        tlv = findViewById(totLV);
        kbSet(false);
        if (tla == null) tla = new TimeListAdapter();
        tlv.setAdapter(tla);
        if ((trix > -1) && (trix < tla.size())) tlv.setSelection(trix);
        tlv.requestFocus();
        tla.notifyDataSetChanged();
        tlv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // entry selected from listview of items
                if (toast != null) toast.cancel();
                trix = position;
                tlv.setSelection(trix);
                tla.notifyDataSetChanged();
            }
        });
        dMode = DM_TOTS;
    }
    public void totDoneButton(View view) {
        dMode = 0;
        disTime();
    }
    /***EDIT ENTRY DISPLAY*****************************************************************************/
    private void disEd() {
        setContentView(layout.edit_main);
        dMode = DM_EDIT;
    }
    public void edDoneButton(View view) {       //Single entry edit done
        DateFormat dfmt=new SimpleDateFormat("yyyy/MM/dd hh:mma", Locale.US);
        kbSet(false);
        if ((stix > -1) && (stix < sla.size())) {
            try {
                sla.get(stix).stamp = dfmt.parse(dt_v.getText().toString());
            } catch (java.text.ParseException e) {sla.get(stix).stamp = null;}
            sla.get(stix).text = m_v.getText().toString().trim();
            slMod = true;
        }
        dMode = DM_TIME;
        disTime();
    }
    /***EDIT DIVISOR/MULTIPLIER DISPLAY****************************************************************/
    private void disDiv() {
        setContentView(layout.edit_cdiv);
        dMode = DM_DIV;
    }
    public void eDivButton(View view) {       //Div/Mult entry edit done
        kbSet(false);
        curDiv = valueOf(div_v.getText().toString());
        dMode = DM_TIME;
        disTime();
    }
    /***JOB CODE DISPLAY*******************************************************************************/
    private void disJob() {
        setContentView(layout.edit_job);
        jc_v = findViewById(jcView);
        jn_v = findViewById(jnView);
        jr_v = findViewById(jrView);
        jc_v.setText("");
        jn_v.setText("");
        jr_v.setText("");
        clv = findViewById(codeList);
        if (cla == null) cla = new CodeListAdapter();
        clv.setAdapter(cla);
        if ((cix > -1) && (cix < cla.getCount())) clv.setSelection(cix);
        kbSet(true);
        jn_v.requestFocus();
        cla.notifyDataSetChanged();
        clv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // entry selected from listview of items
                if (toast != null) toast.cancel();
                cix = position;
                clv.setSelection(cix);
                cla.notifyDataSetChanged();
            }
        });
        dMode = DM_JOB;
    }
    public void jAddButton(View view) {       //Single entry add selected
        jit = new CodeItem();
        jc_v = findViewById(jcView);
        jn_v = findViewById(jnView);
        jr_v = findViewById(jrView);
        jit.cdNum = text2int(jc_v.getText().toString());
        jit.cdName = jn_v.getText().toString();
        jit.cdRate = valueOf(jr_v.getText().toString());
        cix = cla.add(jit);
        clv.setSelection(cix);
        cla.notifyDataSetChanged();
        dMode = DM_JOB;
        disJob();
    }
    public void jDoneButton(View view) {       //Single entry edit done
        kbSet(false);
        dMode = DM_TIME;
        disTime();
    }
    /***CHECK LIST DISPLAY ****************************************************************************/
    private void disChk() {
        setContentView(layout.check_main);
        plv = findViewById(checkList);
        if (pla == null) {
            pla = new PCListAdapter();
            pit = new PCItem();
            pit.chkNum="unk";
            pcix = pla.add(pit);
        }
        plv.setAdapter(pla);
        if ((pcix > -1) && (pcix < pla.size())) {
            pit = pla.get(pcix);
            plv.setSelection(pcix);
            disChkEntry(pit);
        }
        plv.requestFocus();
        pla.notifyDataSetChanged();
        plv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // entry selected from listview of items
                if (toast != null) toast.cancel();
                pcix = position;
                plv.setSelection(pcix);
                pla.notifyDataSetChanged();
                pit=pla.get(pcix);
                disChkEntry(pit);
            }
        });
        dMode = DM_CHK;
    }
    public void pcTimeButton(View view) {
        pit = pla.get(pcix);
        String t = curObbDir+F_SEP+"T"+ sortableDate(pit.perEnd)+".txt";
        File f = new File( t );
        if (f.exists())
            curFile = t;
        else
            curFile = curFileDir + F_SEP + getString(string.f_defName);
        sla.clear();
        stix = -1;
        rdTime(curFile);
        slMod = false;
        dMode = DM_TIME;
        disTime();
    }
    public void pcAddButton(View view) {
        pit = new PCItem();
        pcix = pla.add(pit);     //make blank
        disChkEntry(pit);
        pla.notifyDataSetChanged();
    }
    public void pcSaveButton(View view) {
        pit = getChkEntry();
        if((pcix > -1)&&(pcix < pla.size()))
            pla.set(pcix, pit);     //changed - replace
        else
            pcix = pla.add(pit);     //new - add
        pla.notifyDataSetChanged();
    }
    public void pcExitButton(View view) {       //pc view - done button
        wrtChecks();
        appExit();
    }
    public void disChkEntry(PCItem pcit) {
        pcDate = findViewById(cDate);
        pbDate = findViewById(cbDate);
        peDate = findViewById(ceDate);
        pAmt = findViewById(cAmt);
        pNum = findViewById(cNum);
        pcDate.setText(txtFullDate(pcit.chkDate));
        pbDate.setText(txtFullDate(pcit.perBeg));
        peDate.setText(txtFullDate(pcit.perEnd));
        if (pcit.chkAmt>0.00)
            pAmt.setText(String.format(Locale.US,"%9.2f",pcit.chkAmt));
        else
            pAmt.setText("");           //leave blank on zero (shows hint)
        pNum.setText(pcit.chkNum);
    }
    public PCItem getChkEntry() {
        PCItem r = new PCItem();
        pcDate = findViewById(cDate);
        pbDate = findViewById(cbDate);
        peDate = findViewById(ceDate);
        pAmt = findViewById(cAmt);
        pNum = findViewById(cNum);
        r.chkDate = toDate(pcDate.getText().toString());
        r.perBeg = toDate(pbDate.getText().toString());
        r.perEnd = toDate(peDate.getText().toString());
        String amt = pAmt.getText().toString();
        if (!amt.equals("")) r.chkAmt = valueOf(amt); else r.chkAmt = 0.0;
        r.chkNum = pNum.getText().toString().trim();
        return r;
    }
    /**** check details */
    public void disChkDet(View view) {
        setContentView(layout.checkdetail);
        cdlv = findViewById(chDetList);
        if (cdla == null) {
            cdla = new PdListAdapter(); }
        if (cdla.size()<1) {
            cdit = new PCDetail();
            cdit.text="";
            cdit.rate=0.0;cdit.hrs=0.0;cdit.amt=0.0;
            cdix = cdla.add(cdit);
        }
        cdlv.setAdapter(cdla);
        if ((cdix > -1) && (cdix < pla.size())) {
            cdit = cdla.get(cdix);
            cdlv.setSelection(cdix);
            disChkDetail(cdit);
        }
        cdlv.requestFocus();
        cdla.notifyDataSetChanged();
        cdlv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // entry selected from listview of items
                if (toast != null) toast.cancel();
                cdix = position;
                cdlv.setSelection(cdix);
                cdla.notifyDataSetChanged();
                cdit=cdla.get(cdix);
                disChkDetail(cdit);
            }
        });
        dMode = DM_CHKDET;
    }
    public void disChkDetail(PCDetail pd) {
        TextView vpdText, vpdRate, vpdHrs, vpdAmt;
        vpdText = findViewById(pdText);
        vpdRate = findViewById(pdRate);
        vpdHrs = findViewById(pdHrs);
        vpdAmt = findViewById(pdAmt);
        vpdText.setText(pd.text);
        vpdRate.setText(String.format(Locale.US,"%9.2f",pd.rate));
        vpdHrs.setText(String.format(Locale.US,"%9.2f",pd.hrs));
        vpdAmt.setText(String.format(Locale.US,"%9.2f",pd.amt));
    }
    public PCDetail getChkDetail() {
        TextView vpdText, vpdRate, vpdHrs, vpdAmt;
        PCDetail r = new PCDetail();
        vpdText = findViewById(pdText);
        vpdRate = findViewById(pdRate);
        vpdHrs = findViewById(pdHrs);
        vpdAmt = findViewById(pdAmt);
        r.text = vpdText.getText().toString();
        r.rate = Double.parseDouble(vpdRate.getText().toString());
        r.hrs = Double.parseDouble(vpdHrs.getText().toString());
        r.amt = Double.parseDouble(vpdAmt.getText().toString());
        return r;
    }
    public void pdSaveButton(View view) {
        cdit = getChkDetail();
        if((cdix > -1)&&(cdix < cdla.size()))
            cdla.set(cdix, cdit);     //changed - replace
        else
            cdix = cdla.add(cdit);     //new - add
        cdla.notifyDataSetChanged();
    }
    public void pdDoneButton(View view) {       // done button
//        wrtChecks();
        kbSet(false);
        dMode= DM_CHK;
        disChk();
    }
    /***TEXT EDITOR DISPLAY****************************************************************************/
    private void setTxtEd() {
        dMode = DM_TEXT + DM_FILE;
        selectFile("Select file for Text Edit",curFile);
    }
    private void disTxtEd() {
        File tmpFile = new File(curFile);
        if ( tmpFile.isFile() ) fileContent = fop.read(curFile);
        if (fileContent != null) {
            setContentView(layout.text_editor);
            EditText tfv = findViewById(textFnView);
            tfv.setText(curFile);
            EdTxt_v = findViewById(textBox);
            EdTxt_v.setText(fileContent, TextView.BufferType.EDITABLE);
            EdTxt_v.requestFocus();
            dMode = DM_TEXT;
        }
    }
    public void txtSaveButton(View view) {
        EditText tfv = findViewById(textFnView);
        curFile = tfv.getText().toString();
        EdTxt_v = findViewById(textBox);
        fileContent = EdTxt_v.getText().toString();
        if (fop.write(curFile,fileContent)==0)
            simpleToast(curFile + " saved.",1);
        else
            simpleToast("Save failed - " + curFile,1);
        if (curFile.equals( curFileDir + F_SEP + getString(R.string.f_defName))) {
            sla.clear();                //it is the current list, assume data changed
            slMod = false;
            rdTime(curFile);
        }
        dMode= DM_TIME;
        disTime();
    }
    /***FILE SELECTION*********************************************************************************/
    private void selectFile(String prompt, String preload) {//filename.xml Setup view & Adapter
        File tFile;
        setContentView(layout.filename);
//sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        prompt_v = findViewById(filePrompt);
        String pr = curDataDir+System.lineSeparator()+curObbDir+System.lineSeparator()
                +curPubDir+System.lineSeparator()+prompt;
        if (!fop.isExternalStorageWritable()) simpleToast("no external",1);
        prompt_v.setText(pr);
        dir_v = findViewById(dView);
        fname_v = findViewById(fileName);
        flv = findViewById(fileList);
        if (fla == null) fla = new FileListAdapter();
        flv.setAdapter(fla);
        tFile = new File(preload);
        curDir = extractDir(tFile);
        dir_v.setText(curDir);
        if (tFile.exists()) {
            if (tFile.isDirectory())
                curName = "";
            else
                curName = tFile.getName();
        } else {            //file or dir doesn't exist, save new ?
            curName = tFile.getName();
        }
        fname_v.setText(curName);
        fla.clear();
        if (tFile.isDirectory()) {            //show selected directory contents
            fop.find(tFile, fla, false);
        } else {                              //show current directory and filename
            fop.find(tFile.getParentFile(),fla,false);
        }
        if (fla.size()>0) fiix = 0;     //default to first (or only) entry
        if ( (fiix>-1) && (fiix<fla.size()) )
            flv.setSelection(fiix);
        flv.requestFocus();
        flv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick (AdapterView < ? > parent, View view, int position, long id) {
                // entry selected from fileList view of items
                File selFile;
                String seg;
                fiix = position;
                selFile = fla.get(fiix);
                if  ((selFile.exists()&(!selFile.isDirectory()))) {
                    seg = selFile.getName();
                    fname_v.setText(seg);
//                    int i = seg.length();
                    seg = selFile.getParent();
                    dir_v.setText(seg);
                } else {
                    seg = selFile.getPath();
                    dir_v.setText(seg);
                    fname_v.setText("");
                    fla.clear();
                    fop.find(selFile, fla, false);
                }
                if ((fiix>-1)&(fiix<fla.size())) flv.setSelection(fiix);
                fla.notifyDataSetChanged();
            }
        });
    }
    public void fnSelectButton(View view) {                             //filename.xml Select Button
        dir_v = findViewById(dView);
        fname_v = findViewById(fileName);
        curDir = dir_v.getText().toString().trim();
        curName = fname_v.getText().toString().trim();
        if (curDir.substring(curDir.length()- F_SEP.length()).equals(F_SEP))  //is F_SEP already there?
            curFile = curDir + curName;
        else
            curFile =  curDir + F_SEP + curName;
        dMode = dMode & ~DM_FILE;                   //turn off file flag bit
        switch (dMode) {
            case DM_TIME:                                                              //DM_TIME
                if (rdTime(curFile)) slMod = false;
                else simpleToast("Load failed " + curFile, 1);
                disTime();
                break;
            case DM_TEXT:                                                              //DM_TEXT
                if (slMod) simpleToast("unsaved changes!",1);
                disTxtEd();
                break;
            case DM_SAVE:                                                              //DM_SAVE
                wrtTime(curFile);
                disTime();
                break;
            case DM_JOB:                                                               //DM_JOB
                disJob();       //should never get here
                break;
            case DM_TOTS:                                                              //DM_TOTS
                disTot();       //should never get here
                break;
            default:                                                                   //default
                simpleToast("unknown file mode " + dMode,1);
                break;
        }
    }
    public void fnCdButton(View view) {                                     //filename.xml Up Button
        File tmpFile;
        dir_v = findViewById(dView);
        tmpFile = new File(dir_v.getText().toString());
        fla.clear();
        if (fop.find(tmpFile,fla,false)==0) {
//			flv.setEmptyView(layout.empty_view);
            fla.notifyDataSetChanged();  //fop doesn't notify if empty
        }
    }
    public void fnIntButton(View view) {
        File tmpFile;
        dir_v = findViewById(dView);
        dir_v.setText(curFileDir);
        tmpFile = new File(curFileDir);
        fla.clear();
        if (fop.find(tmpFile,fla,false)==0) {
//			flv.setEmptyView(layout.empty_view);
            fla.notifyDataSetChanged();  //fop doesn't notify if empty
        }
    }
    public void fnExtButton(View view) {
        File tmpFile;
        dir_v = findViewById(dView);
        dir_v.setText(curObbDir);
        tmpFile = new File(curObbDir);
        fla.clear();
        if (fop.find(tmpFile,fla,false)==0) {
//			flv.setEmptyView(layout.empty_view);
            fla.notifyDataSetChanged();  //fop doesn't notify if empty
        }
    }
    public void fnUpButton(View view) {                                     //filename.xml Up Button
        File tmpFile;
        dir_v = findViewById(dView);
        tmpFile = new File(dir_v.getText().toString());
        File upf = tmpFile.getParentFile();
        dir_v.setText(tmpFile.getParent());
        fla.clear();
        if (fop.find(upf,fla,false)==0) fla.notifyDataSetChanged();  //if no change, be sure empty display
    }
    public void fnCancel(View view) {                                   //filename.xml Cancel Button
        dMode = 0;
        disTime();
    }
    public void fnDelButton(View view) {
        File tmpFile;
        String tmpFname;
        dir_v = findViewById(dView);
        fname_v = findViewById(fileName);
        curDir = dir_v.getText().toString().trim();
        if (curDir.substring(curDir.length()- F_SEP.length()).equals(F_SEP))  //is F_SEP already there?
            tmpFname = curDir + fname_v.getText().toString();
        else
            tmpFname =  curDir + F_SEP + fname_v.getText().toString();
        tmpFile = new File(tmpFname);
        fla.remove(tmpFile);
        fla.notifyDataSetChanged();
        tmpFile.delete();
    }
    /***MENU INITIATED ACTIONS*************************************************************************/
    public void ExitButton(View view) {
        appExit();
    }
    private void endPeriod() {
        DateFormat dfmt=new SimpleDateFormat("yyyyMMdd",Locale.US);
        Date now = Calendar.getInstance().getTime();
        curFile = getObbDir() + F_SEP + "T";
        curFile += dfmt.format(now);
        dMode = DM_FILE + DM_SAVE;
        selectFile("Select destination", curFile);
    }
    private void totTime() {
        int i = 0;
        Date td=null;
        TimeItem sumentry, tItem;
        String mkey, rTxt;

        if (sla.size() < 1) {
            simpleToast("nothing to total", 1);
            return;
        }
        setContentView(layout.daily);
        if (tla == null) tla = new TimeListAdapter();
        tla.clear();
        sumentry = new TimeItem();
        sumentry.hrs = 0.0;
        sumentry.rate = 0.0;
        sumentry.total = 0.0;
        i=0;
        tItem = new TimeItem();
        while (i < sla.size()) {
            mit = sla.get(i);
            td = mit.stamp;
            if (mit.text.contains(" ")) {
                mkey=mit.text.substring(0,mit.text.indexOf(" "));       //all up to space is main key
                rTxt=mit.text.substring(mit.text.indexOf(" ") + 1);     //remainder after KEY word
            } else {
                mkey = mit.text;                      //no space, so entire text is the key
                rTxt = "";
            }
            if (mkey.equals(getString(string.in))) {
                if (tItem.in != null) {               //there is current shift open
                    trix = tla.add(tItem);            //save current entry (shift)
                }
                tItem = new TimeItem();             //begin a new entry (new shift)
                if (td == null) simpleToast("In time error "+mit.stamp, 0);
                tItem.in = td;
                tItem.out = null;
                tItem.bst = null;
                tItem.bend = null;      //clear end and breaks
                tItem.hrs = 0.0;
                tItem.rate = curRate;
                tItem.total = 0.0;
            }
            if (mkey.equals(getString(string.break_str))) {                   // break...
                if (rTxt.equals(getString(string.brst))) {                //start
                    if (td == null) simpleToast("Brk Strt time error "+mit.stamp, 0);
                    tItem.bst = td;
                }
                if (rTxt.equals(getString(string.brendt))) {              //end
                    if (td == null) simpleToast("Brk End time error "+mit.stamp, 0);
                    tItem.bend = td;
                }
            }
            if (mkey.equals(getString(string.out))) {
                if (td == null) simpleToast("Out time error "+mit.stamp, 0);
                tItem.out = td;
                try {tItem.hrs = tItem.out.getTime() - tItem.in.getTime();}
                catch (NullPointerException e) { tItem.hrs = 0; }
                if (tItem.bst != null) {               //we have break starting time
                    if (tItem.bend != null)           //must also have end time
                        tItem.hrs -= tItem.bend.getTime() - tItem.bst.getTime();  //subtract time on break
                    else
                        simpleToast("break start without end", 1);
                }
                tItem.hrs = tItem.hrs / 3600000.0;        //adjust to hours in decimal (tenths/hr)
                if (tItem.rate == 0.0) simpleToast("Pay Rate NOT set!", 1);
                tItem.total = tItem.hrs * tItem.rate;
                sumentry.hrs += tItem.hrs;
                if (tItem.rate > sumentry.rate)
                    sumentry.rate = tItem.rate;    //keep highest pay rate
                sumentry.total += tItem.total;
            }
            if (mkey.equals(getString(string.tip))) {
                if (!rTxt.equals("")) curTip = valueOf(rTxt);
            }
            if (mkey.equals(getString(string.tipcash))) {
                if (!rTxt.equals("")) curTipCash = valueOf(rTxt);
            }
            if (mkey.equals(getString(string.rate))) {
                if (!rTxt.equals("")) curRate = rLookUp(rTxt);
            }
            if (mkey.equals(getString(string.adj))) {
                if (!rTxt.equals("")) curDiv = valueOf(rTxt);
            }
            i++;
        }
        if (tItem.in!=null)             //expect last record
            trix = tla.add(tItem);             //save last entry
        // instead of "rate total" give "total adjusted" values
        sumentry.rate = sumentry.total;
        sumentry.total = sumentry.rate * curDiv;
        totline=tla.add(sumentry);
        trix=totline;
        disTot();
    }
    private void loadOther() {
        dMode = DM_FILE + DM_TIME;
        curFile = getObbDir().toString();
        selectFile("Select file to Load", curFile );
    }
    private void saveOther() {
        dMode = DM_FILE + DM_SAVE;
        selectFile("Select file to Save", curFile);
    }
    private void appClear() {
        sla.clear();
        disTime();
    }
    private void appReset() {
        if (userOk(findViewById(R.id.action_bar),"Reset all data?","Yes","No")) {
/*            curFile = curFileDir + F_SEP + getString(R.string.f_defName);
            sla.clear();
            slMod = false;
            File f = new File(curFile);
            if (f.delete()) simpleToast("Deleted " + curFile, 1); */
            simpleToast("fail reset",1);
        }
        disTime();
    }
    private void appExit() {
        wrtGlobal();
        wrtChecks();
        wrtTime(curFile);
        slMod = false;
        appQuit();
    }
    private void appQuit() {
        if (slMod) {
            simpleToast("CHANGES LOST - " + curFile, 1);
        }
        setResult(RESULT_OK);
        finish();       //end this activity RETURNS activityResult() *******************************
    }
    /***FUNCTIONS**************************************************************************************/
    private void wrtStamp(String sMsg) {
        mit = new lineItem();
        mit.stamp = Calendar.getInstance().getTime();
        mit.text = sMsg;
        if (mit.equals(lastMsg)) {             //only accept one entry per second from cStamp()
            simpleToast("duplicate",0);
            return; }
        stix = sla.add(mit);
        sla.notifyDataSetChanged();
        slv.setSelection(stix);
        //slv.smoothScrollToPosition(stix);
        lastMsg = mit;
        slMod = true;
//        simpleToast("Logged " + lastMsg, 1);
    }
    private void insStamp() {
        mit = new lineItem();
        mit.stamp = Calendar.getInstance().getTime();
        mit.text = "";
        stix = sla.insert(stix, mit);
        editStamp();
//        simpleToast("Inserted", 0);
    }
    private void shiftUpStamp() {
        if (stix > 0) {
            mit = sla.set(stix - 1, sla.get(stix));
            sla.set(stix, mit);
            stix--;
            slMod = true;
            sla.notifyDataSetChanged();
            slv.setSelectionFromTop(stix, 150);
        }
    }
    private void shiftDownStamp() {
        if (stix + 1 < sla.size()) {
            mit = sla.set(stix + 1, sla.get(stix));
            sla.set(stix, mit);
            stix++;
            slMod = true;
            sla.notifyDataSetChanged();
            slv.setSelectionFromTop(stix, 150);
        }
    }
    private void delStamp() {
        if ((stix>-1)&&(stix<sla.size())) {
            simpleToast(getString(R.string.m_entry_deleted)+sla.get(stix).stamp,1);
            sla.remove(stix);
            slMod=true;
            if (stix>-1) stix--;
            sla.notifyDataSetChanged();
        }
    }
    private void clrStamp() {
        sla.clear();
        slMod = false;
        sla.notifyDataSetChanged();
    }
    private void editStamp() {           //Single entry edit start
        DateFormat dfmt=new SimpleDateFormat("yyyy/MM/dd hh:mma",Locale.US);
        dMode=DM_EDIT;
        setContentView(R.layout.edit_main);
        dt_v = findViewById(dtView);
        m_v = findViewById(mView);
        if ((stix > -1) && (stix < sla.size())) {
            dt_v.setText(dfmt.format(sla.get(stix).stamp));
            m_v.setText(sla.get(stix).text);
        }
        else { dt_v.setText(""); m_v.setText(""); }
    }
    private double rLookUp(String lCode) {
        int lc=0;
        if (lCode==null) return 0.00;
        lc = text2int(lCode);
        return cla.find(lc);
    }
    /***sub-functions**********************************************************************************/
    private void rdGlobal() {
        gloFile = curFileDir + F_SEP +  getString(R.string.f_gloName);
        BufferedReader br;
// Read job codes
        try {
            br = new BufferedReader(new FileReader(gloFile));
            String line;
            while ((line = br.readLine()) != null) {
                int l = line.length();
                if (l>47) {
                    jit = new CodeItem();
                    jit.cdNum = text2int(line.substring(0, 2));
                    jit.cdName = line.substring(3, 43).trim();
                    jit.cdRate = valueOf(line.substring(42));
                    cix = cla.add(jit);
                    cla.notifyDataSetChanged();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void rdChecks() {
        chkFile = curFileDir + F_SEP +  getString(R.string.f_chkName);
        BufferedReader br;
// Read check entries
        if (pla == null) {
            pla = new PCListAdapter();
            pcix = -1;
        }
        try {
            br = new BufferedReader(new FileReader(chkFile));
            String line;
            while ((line = br.readLine()) != null) {
                int l = line.length();
                if (l<43) { simpleToast("entry too short",1); return; }
                pit = new PCItem();
                pit.chkDate = toDate(line.substring(0, 10));
                pit.chkAmt = valueOf(line.substring(11,20));
                pit.perBeg = toDate(line.substring(20, 31));
                pit.perEnd = toDate(line.substring(31, 42));
                pit.chkNum = line.substring(43);
                pcix = pla.add(pit);
            }
            pla.notifyDataSetChanged();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private boolean rdTime(String fname) {
// Read historical timestamps
        BufferedReader br;
        DateFormat ffmt=new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.US);   // yyyy/MM/dd HH:mm
        DateFormat lfmt=new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy",Locale.US);  // Thu Jun 06 00:00:00 MDT 2019 Rate 1
//        DateFormat tfmt=new SimpleDateFormat("yyyy/MM/dd hh:mma",Locale.US);
        String line;
        try {
            br = new BufferedReader(new FileReader(fname));
            while ((line = br.readLine()) != null) {
                mit = new lineItem();
                int l = line.length();
                if (l>=16) {
                    try {
                        mit.stamp = ffmt.parse(line);   // .substring(0, 16)
                        mit.text = line.substring(16);
                    } catch (java.text.ParseException e) {
                        try {
                            mit.stamp = lfmt.parse(line);  //  .substring(0, 25)
                            mit.text = line.substring(25);
                        } catch (java.text.ParseException e2) {mit.stamp = null;}
                    }
                }
                else {
                    mit.stamp = null;
                    mit.text = line;
                }
                stix = sla.add(mit);
            }
            sla.notifyDataSetChanged();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    private void wrtGlobal() {
        StringBuilder outBuf = new StringBuilder();
        StringBuilder outSeg;
        int r = 0;
        cix = 0;
        while (cix < cla.getCount()) {          //loop through all entries
            jit = cla.get(cix);
            outBuf.append(String.format(Locale.US,"%2d",jit.cdNum)).append(" ");
            outSeg = new StringBuilder(jit.cdName);
            while (outSeg.length()<40) outSeg.append(" ");
            outBuf.append(outSeg).append(String.format(Locale.US,"%5.2f",jit.cdRate))
                    .append(System.getProperty("line.separator"));
            cix++;
        }
        gloFile = curFileDir + F_SEP +  getString(R.string.f_gloName);
        r = fop.write(gloFile, outBuf.toString());
//		if (r == 0) simpleToast(gloFile + " saved", 1);
        if (r == 1) simpleToast("Nothing to write " + gloFile, 1);
        if (r == 2) simpleToast("Security exception " + gloFile, 1);
        if (r == 3) simpleToast("IO Exception " + gloFile, 1);
    }
    private void wrtChecks() {
        StringBuilder outBuf = new StringBuilder();
        int r = 0;
        pcix = 0;
        while (pcix < pla.size()) {          //loop through all entries
            pit = pla.get(pcix);
            outBuf.append(txtFullDate(pit.chkDate)).append(" ")
                    .append(String.format(Locale.US,"%9.2f",pit.chkAmt)).append(" ")
                    .append(txtFullDate(pit.perBeg)).append(" ").append(txtFullDate(pit.perEnd)).append(" ")
                    .append(pit.chkNum).append(System.getProperty("line.separator"));
            pcix++;
        }
        chkFile = curFileDir + F_SEP +  getString(R.string.f_chkName);
        r = fop.write(chkFile, outBuf.toString());
//		if (r == 0) simpleToast(gloFile + " saved", 1);
        if (r == 1) simpleToast("Nothing to write " + chkFile, 1);
        if (r == 2) simpleToast("Security exception " + chkFile, 1);
        if (r == 3) simpleToast("IO Exception " + chkFile, 1);
    }
    private void wrtTime(String fname) {
        StringBuilder outBuf = new StringBuilder();
        int i = 0, r = 0;
        while (i < sla.size()) {
            outBuf.append(vStamp(sla.get(i).stamp,1)).append(" ").append(sla.get(i).text)
                    .append(System.getProperty("line.separator"));
            i++;
        }
        r = fop.write(fname, outBuf.toString());
        if (r == 0) {
            simpleToast(fname + " saved " + sla.size(), 1);
            slMod = false;
        }
        if (r == 1) simpleToast("Nothing to write " + fname, 1);
        if (r == 2) simpleToast("Security exception " + fname, 1);
        if (r == 3) simpleToast("IO Exception " + fname, 1);
    }
    String extractDir(File from) {
        String r, p, n;
        p = from.getPath();
        if (from.isDirectory())
            r = p;
        else {
            n = from.getName();
            r = p.substring(0, p.length() - Objects.requireNonNull(F_SEP).length() - n.length());  //all before "/" name
        }
        return r;
    }
    private Date toDate(String str) {
        Date dv;
        DateFormat formatter=new SimpleDateFormat("MM/dd/yyyy",Locale.US);
        try {
            dv = formatter.parse(str);
        } catch (java.text.ParseException e) {dv = null;}
        return dv;
    }
    String txtFullDate(Date d) {
        DateFormat formatter=new SimpleDateFormat("MM/dd/yyyy",Locale.US);
        String s;
        if (d!=null) s=formatter.format(d); else s="";
        return s;
    }
    String sortableDate(Date d) {
        DateFormat formatter=new SimpleDateFormat("yyyyMMdd",Locale.US);
        String s;
        s=formatter.format(d);
        return s;
    }
    private void kbSet( boolean visibility) {
        View view = this.getCurrentFocus();     //see who has the control
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (visibility)        //set visible
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            else            //hide (turn-off)
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    private void simpleToast(String message, int duration) {
        toast = Toast.makeText(this, message, duration);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
    private Integer text2int(String seg) {
        if ((seg==null)||(seg.trim().equals("")))
            return 0;
        else
            return Integer.parseInt(seg.trim());
    }
    String getHrMin(Date d) {
        String r="hh:mm";
        if (d!=null)
            r = d.toString().substring(10,16);
        return r;
    }
    String vStamp(Date dv, int ifmt) {
        String s = "yyyy/mm/dd hh:mm";
        if (ifmt==1) {
            DateFormat tfmt = new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.US);
            s = tfmt.format(dv);
        }
        if (ifmt==2) {
            DateFormat tfmt = new SimpleDateFormat("yyyy/MM/dd hh:mm aa", Locale.US);
            s = tfmt.format(dv);
        }
        return s;
    }
    public boolean userOk(View view, String msg, String ok, String notok) {
        boolean answer=true;
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
        dlgAlert.setTitle("Confirm");
        dlgAlert.setMessage(msg);
        dlgAlert.setNegativeButton(notok,new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
//                answer=false;
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        dlgAlert.setPositiveButton(ok,new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
//                answer=true;
                setResult(RESULT_OK);
                finish();
            }
        });
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
        return answer;
    }
    /***List Adapters**********************************************************************************/
    public class StampListAdapter extends BaseAdapter {

        ArrayList<lineItem> mItems;

        StampListAdapter() {
            super();
            if (mItems == null)
                mItems = new ArrayList<>();
            else
                simpleToast("restored existing mItems",1);
        }

        int add(lineItem entry) {
            mItems.add(entry);            //add it (even duplicates)
            return mItems.indexOf(entry);
        }

        int insert(int pos, lineItem entry) {
            if ((pos>-1)&&(pos<mItems.size()))
                mItems.add(pos, entry);       //if not at end, insert it
            else
                mItems.add(entry);            //if not there, add it
            return mItems.indexOf(entry);
        }

        public int contains(int pos, String str) {
            if ( pos < 0 ) pos = 0;
            while (pos < mItems.size()) {
//                String ustr = str.toUpperCase();
                if ( mItems.get(pos).text.contains(str)    ) return pos;
                else
                    pos++;
            }
            return -1;
        }

        void remove(int i) {
            mItems.remove(i);
        }

        void clear() {
            mItems.clear();
        }

        lineItem get(int i) {
            return mItems.get(i);
        }

        lineItem set(int pos, lineItem entry) {
            return mItems.set(pos, entry);
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        int size() {
            return mItems.size();
        }

        @Override
        public Object getItem(int i) {
            return mItems.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            liViewHolder mvh;
            if (convertView == null) {
                convertView = MainActivity.this.getLayoutInflater().inflate(R.layout.list_row, viewGroup, false);  //Row view
                mvh = new liViewHolder();
                mvh.stamp = convertView.findViewById(lStamp);
                mvh.text = convertView.findViewById(lText);
                convertView.setTag(mvh);
            } else {
                mvh = (liViewHolder) convertView.getTag();
            }
//          till view with the values from the array
            mvh.stamp.setText(vStamp(mItems.get(i).stamp,2));
            mvh.stamp.setSelected(i==stix);
            mvh.stamp.setId(i);
            mvh.text.setText(mItems.get(i).text);
            mvh.text.setSelected(i==stix);
            mvh.text.setId(i);
            return convertView;
        }
    }
    public class CodeListAdapter extends BaseAdapter {

        ArrayList<CodeItem> cItems;

        CodeListAdapter() {
            super();
            if (cItems == null)
                cItems = new ArrayList<>();
            else
                simpleToast("restored existing cItems", 1);
        }
        int add(CodeItem entry) {
            cItems.add(entry);            //add it (even duplicates)
            return cItems.indexOf(entry);
        }
        double find(double item) {
            int ix = 0;
            while (ix<cItems.size()) {
                if (cItems.get(ix).cdNum==item) return cItems.get(ix).cdRate;
                ix++;
            }
            return 0.0;
        }

        public int getCount() { return cItems.size(); }
        CodeItem get(int i) { return cItems.get(i); }
        public Object getItem(int i) { return cItems.get(i); }
        public long getItemId(int i) { return i; }
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            cViewHolder cvh;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_crow, viewGroup, false);  //Row view
                cvh = new cViewHolder();
                cvh.cdNum = convertView.findViewById(lCode);
                cvh.cdName = convertView.findViewById(lcName);
                cvh.cdRate = convertView.findViewById(lcRate);
                convertView.setTag(cvh);
            } else {
                cvh = (cViewHolder) convertView.getTag();
            }
//          Fill view with the values from the array
            cvh.cdNum.setText(String.format(Locale.US,"%2d",cItems.get(i).cdNum));
            cvh.cdNum.setSelected(i==stix);
            cvh.cdNum.setId(i);
            cvh.cdName.setText(cItems.get(i).cdName);
            cvh.cdName.setSelected(i==stix);
            cvh.cdName.setId(i);
            cvh.cdRate.setText(String.format(Locale.US,"%8.2f",cItems.get(i).cdRate));
            cvh.cdRate.setSelected(i==stix);
            cvh.cdRate.setId(i);
            return convertView;
        }
    }
    public class TimeListAdapter extends BaseAdapter {

        ArrayList<TimeItem> tItems;

        TimeListAdapter() {
            super();
            if (tItems == null)
                tItems = new ArrayList<>();
            else
                simpleToast("restored existing tItems", 1);
        }

        public void clear() { tItems.clear(); }
        int add(TimeItem entry) {
            tItems.add(entry);            //add it (even duplicates)
            return tItems.indexOf(entry);
        }
        public Object getItem(int i) {
            return tItems.get(i);
        }

        public long getItemId(int i) { return i; }
        @Override
        public int getCount() { return tItems.size(); }
        int size() {     return tItems.size(); }

        public View getView(int i, View convertView, ViewGroup viewGroup) {
            tViewHolder tvh;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_trow, viewGroup, false);  //Row view
                tvh = new tViewHolder();
                tvh.separator = convertView.findViewById(separator);
                tvh.in = convertView.findViewById(lIn);
                tvh.bst = convertView.findViewById(lBst);
                tvh.bend = convertView.findViewById(lBend);
                tvh.out = convertView.findViewById(lOut);
                tvh.hrs = convertView.findViewById(lHours);
                tvh.rate = convertView.findViewById(lRate);
                tvh.total = convertView.findViewById(lTotal);
                convertView.setTag(tvh);
            } else {
                tvh = (tViewHolder) convertView.getTag();
            }
//          Fill view with the values from the array
            if (i==totline) {
                tvh.separator.setText(getString(string.tot_separator));
                tvh.separator.setVisibility(View.VISIBLE);
                tvh.in.setText("");
                tvh.in.setId(i);
                tvh.bst.setText("");
                tvh.bst.setId(i);
                tvh.bend.setText("");
                tvh.bend.setId(i);
                tvh.out.setText("");
                tvh.out.setId(i);
            } else {
                tvh.separator.setVisibility(View.GONE);
                tvh.in.setText(getHrMin(tItems.get(i).in));
                tvh.in.setSelected(i==stix);
                tvh.in.setId(i);
                tvh.bst.setText(getHrMin(tItems.get(i).bst));
                tvh.bst.setSelected(i==stix);
                tvh.bst.setId(i);
                tvh.bend.setText(getHrMin(tItems.get(i).bend));
                tvh.bend.setSelected(i==stix);
                tvh.bend.setId(i);
                tvh.out.setText(getHrMin(tItems.get(i).out));
                tvh.out.setSelected(i==stix);
                tvh.out.setId(i);
            }
            tvh.hrs.setText(String.format(Locale.US,"%6.2f",tItems.get(i).hrs));
            tvh.hrs.setSelected(i==stix);
            tvh.hrs.setId(i);
            tvh.rate.setText(String.format(Locale.US,"%4.2f",tItems.get(i).rate));
            tvh.rate.setSelected(i==stix);
            tvh.rate.setId(i);
            tvh.total.setText(String.format(Locale.US,"%8.2f",tItems.get(i).total));
            tvh.total.setSelected(i==stix);
            tvh.total.setId(i);
            return convertView;
        }
    }
    public class PCListAdapter extends BaseAdapter {

        ArrayList<PCItem> pItems;

        PCListAdapter() {
            super();
            if (pItems == null)
                pItems = new ArrayList<>();
            else
                simpleToast("restored existing pItems", 1);
        }

        public void clear() { pItems.clear(); }
        int add(PCItem entry) {
            pItems.add(entry);            //add it (even duplicates)
            return pItems.indexOf(entry);
        }
        public Object getItem(int i) {
            return pItems.get(i);
        }
        public PCItem get(int i) {
            return pItems.get(i);
        }
        public void set(int i, PCItem p) { pItems.set(i,p);}

        public long getItemId(int i) { return i; }
        @Override
        public int getCount() { return pItems.size(); }
        int size() {     return pItems.size(); }

        public View getView(int i, View convertView, ViewGroup viewGroup) {
            pcViewHolder pvh;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_pcrow, viewGroup, false);  //Row view
                pvh = new pcViewHolder();
                pvh.cdate = convertView.findViewById(lcDate);
                pvh.num = convertView.findViewById(lcNum);
                pvh.amt = convertView.findViewById(lcAmt);
                pvh.bdate = convertView.findViewById(lcbDate);
                pvh.edate = convertView.findViewById(lceDate);
                convertView.setTag(pvh);
            } else {
                pvh = (pcViewHolder) convertView.getTag();
            }
//          Fill view with the values from the array
            pvh.cdate.setText(txtFullDate(pItems.get(i).chkDate));
            pvh.cdate.setSelected(i==pcix);
            pvh.cdate.setId(i);
            pvh.num.setText(pItems.get(i).chkNum);
            pvh.num.setSelected(i==pcix);
            pvh.num.setId(i);
            pvh.amt.setText(String.format(Locale.US,"%8.2f",pItems.get(i).chkAmt));
            pvh.amt.setSelected(i==pcix);
            pvh.amt.setId(i);
            pvh.bdate.setText(txtFullDate(pItems.get(i).perBeg));
            pvh.bdate.setSelected(i==pcix);
            pvh.bdate.setId(i);
            pvh.edate.setText(txtFullDate(pItems.get(i).perEnd));
            pvh.edate.setSelected(i==pcix);
            pvh.edate.setId(i);
            return convertView;
        }
    }
    public class PdListAdapter extends BaseAdapter {

        ArrayList<PCDetail> pdItems;

        PdListAdapter() {
            super();
            if (pdItems == null)
                pdItems = new ArrayList<>();
            else
                simpleToast("restored existing pdItems", 1);
        }

        public void clear() { pdItems.clear(); }
        int add(PCDetail entry) {
            pdItems.add(entry);            //add it (even duplicates)
            return pdItems.indexOf(entry);
        }
        public void set(int i, PCDetail p) { pdItems.set(i,p);}

        public PCDetail get(int i) {
            return pdItems.get(i);
        }
        public Object getItem(int i) {
            return pdItems.get(i);
        }

        public long getItemId(int i) { return i; }
        @Override
        public int getCount() { return pdItems.size(); }
        int size() {     return pdItems.size(); }

        public View getView(int i, View convertView, ViewGroup viewGroup) {
            pdViewHolder pvh;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_pdrow, viewGroup, false);  //Row view
                pvh = new pdViewHolder();
                pvh.text = convertView.findViewById(lText);
                pvh.hrs = convertView.findViewById(lHours);
                pvh.rate = convertView.findViewById(lRate);
                pvh.amt = convertView.findViewById(lAmt);
                convertView.setTag(pvh);
            } else {
                pvh = (pdViewHolder) convertView.getTag();
            }
//          Fill view with the values from the array
            pvh.text.setText(pdItems.get(i).text);
            pvh.text.setSelected(i==pdix);
            pvh.text.setId(i);
            pvh.hrs.setText(String.format(Locale.US,"%4.2f",pdItems.get(i).hrs));
            pvh.hrs.setSelected(i==pdix);
            pvh.hrs.setId(i);
            pvh.rate.setText(String.format(Locale.US,"%4.2f",pdItems.get(i).rate));
            pvh.rate.setSelected(i==pdix);
            pvh.rate.setId(i);
            pvh.amt.setText(String.format(Locale.US,"%4.2f",pdItems.get(i).amt));
            pvh.amt.setSelected(i==pdix);
            pvh.amt.setId(i);
            return convertView;
        }
    }
    public class FileListAdapter extends BaseAdapter {

        ArrayList<File> fItems;

        FileListAdapter() {
            super();
            if (fItems == null)
                fItems = new ArrayList<>();
//		else
//			simpleToast("restored existing fItems", 1);
        }

        int add(File entry) {
            fItems.add(entry);            //add it (even duplicates)
            return fItems.indexOf(entry);
        }
        boolean remove(File entry) {
            return fItems.remove(entry);
        }
        File get(int i) {
            return fItems.get(i);
        }
        @Override
        public Object getItem(int i) {
            return fItems.get(i);
        }
        public String getName(int i) {
            return fItems.get(i).getName();
        }

        @Override
        public long getItemId(int i) {
            return i;
        }
        @Override
        public int getCount() { return fItems.size(); }
        int size() {     return fItems.size(); }
        void clear() {          fItems.clear();}

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            fViewHolder fvh;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_frow, viewGroup, false);  //Row view
                fvh = new fViewHolder();
                fvh.fpath = convertView.findViewById(lFpath);
                fvh.fsize = convertView.findViewById(lFsize);
                convertView.setTag(fvh);
            } else {
                fvh = (fViewHolder) convertView.getTag();
            }
//          Fill view with the values from the array
            fvh.fpath.setText(fItems.get(i).getName());
            fvh.fpath.setSelected(fItems.get(i).isDirectory());     //highlight directories
            fvh.fpath.setId(i);
//                            String.format  %[argument_index$][flags][width][.precision]conversion
            fvh.fsize.setText(String.format(Locale.US,"%d",fItems.get(i).length()));
            if (fItems.get(i).isFile())
                fvh.fsize.setVisibility(View.VISIBLE);
            else
                fvh.fsize.setVisibility(View.GONE);
            fvh.fsize.setId(i);
            return convertView;
        }
    }
}