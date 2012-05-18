package edu.berkeley.cs.amplab.carat.android.storage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.List;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import edu.berkeley.cs.amplab.carat.thrift.HogBugReport;
import edu.berkeley.cs.amplab.carat.thrift.HogsBugs;
import edu.berkeley.cs.amplab.carat.thrift.Reports;

public class CaratDataStorage {

    public static final String FILENAME = "carat-reports.dat";
    public static final String BUGFILE = "carat-bugs.dat";
    public static final String HOGFILE = "carat-hogs.dat";

    public static final String FRESHNESS = "carat-freshness.dat";
    private Application a = null;

    private long freshness = 0;
    private WeakReference<Reports> caratData = null;
    private WeakReference<SimpleHogBug[]> bugData = null;
    private WeakReference<SimpleHogBug[]> hogData = null;

    public CaratDataStorage(Application a) {
        this.a = a;
        freshness = readFreshness();
        caratData = new WeakReference<Reports>(readReports());
        readBugReport();
        readHogReport();
    }

    public void writeReports(Reports reports) {
        if (reports == null)
            return;
        caratData = new WeakReference<Reports>(reports);
        writeObject(reports, FILENAME);
    }

    public void writeFreshness() {
        freshness = System.currentTimeMillis();
        writeText(freshness + "", FRESHNESS);
    }

    public void writeObject(Object o, String fname) {
        FileOutputStream fos = getFos(fname);
        if (fos == null)
            return;
        try {
            ObjectOutputStream dos = new ObjectOutputStream(fos);
            dos.writeObject(o);
            dos.close();
        } catch (IOException e) {
            Log.e(this.getClass().getName(), "Could not write object:" + o
                    + "!");
            e.printStackTrace();
        }
    }

    public Object readObject(String fname) {
        FileInputStream fin = getFin(fname);
        if (fin == null)
            return null;
        try {
            ObjectInputStream din = new ObjectInputStream(fin);
            Object o = din.readObject();
            din.close();
            return o;
        } catch (IOException e) {
            Log.e(this.getClass().getName(), "Could not read object from "
                    + fname + "!");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            Log.e(this.getClass().getName(),
                    "Could not find class: " + e.getMessage()
                            + " reading from " + fname + "!");
            e.printStackTrace();
        }
        return null;
    }

    public void writeText(String thing, String fname) {
        FileOutputStream fos = getFos(fname);
        if (fos == null)
            return;
        try {
            DataOutputStream dos = new DataOutputStream(fos);
            dos.writeUTF(thing);
            dos.close();
        } catch (IOException e) {
            Log.e(this.getClass().getName(), "Could not write text:" + thing
                    + "!");
            e.printStackTrace();
        }
    }

    public String readText(String fname) {
        FileInputStream fin = getFin(fname);
        if (fin == null)
            return null;
        try {
            DataInputStream din = new DataInputStream(fin);
            String s = din.readUTF();
            din.close();
            return s;
        } catch (IOException e) {
            Log.e(this.getClass().getName(), "Could not read text from "
                    + fname + "!");
            e.printStackTrace();
        }
        return null;
    }

    private FileInputStream getFin(String fname) {
        try {
            return a.openFileInput(fname);
        } catch (FileNotFoundException e) {
            Log.e(this.getClass().getName(), "Could not open carat data file "
                    + fname + " for reading!");
            // e.printStackTrace();
            return null;
        }
    }

    private FileOutputStream getFos(String fname) {
        try {
            return a.openFileOutput(fname, Context.MODE_PRIVATE);
        } catch (FileNotFoundException e) {
            Log.e(this.getClass().getName(), "Could not open carat data file "
                    + fname + " for writing!");
            // e.printStackTrace();
            return null;
        }
    }

    public long readFreshness() {
        String s = readText(FRESHNESS);
        Log.d("CaratDataStorage", "Read freshness: " + s);
        if (s != null)
            return Long.parseLong(s);
        else
            return -1;
    }

    public Reports readReports() {
        Object o = readObject(FILENAME);
        Log.d("CaratDataStorage", "Read Reports: " + o);
        if (o != null) {
            caratData = new WeakReference<Reports>((Reports) o);
            return (Reports) o;
        } else
            return null;
    }

    /**
     * @return the freshness
     */
    public long getFreshness() {
        return freshness;
    }

    /**
     * @return the caratData
     */
    public Reports getReports() {
        if (caratData != null && caratData.get() != null)
            return caratData.get();
        else
            return readReports();
    }

    /**
     * @return the bug reports
     */
    public SimpleHogBug[] getBugReport() {
        if (bugData == null || bugData.get() == null) {
            readBugReport();
        }
        if (bugData == null || bugData.get() == null)
            return null;
        return bugData.get();
    }

    /**
     * @return the hog reports
     */
    public SimpleHogBug[] getHogReport() {
        if (hogData == null || hogData.get() == null) {
            readHogReport();
        }
        if (hogData == null || hogData.get() == null)
            return null;
        return hogData.get();
    }

    public void writeBugReport(HogBugReport r) {
        if (r != null) {
            SimpleHogBug[] list = convert(r.getHbList(), true);
            if (list != null){
                bugData = new WeakReference<SimpleHogBug[]>(list);
                writeObject(list, BUGFILE);
            }
        }
    }

    public void writeHogReport(HogBugReport r) {
        if (r != null) {
            SimpleHogBug[] list = convert(r.getHbList(), false);
            if (list != null){
                hogData = new WeakReference<SimpleHogBug[]>(list);
                writeObject(list, HOGFILE);
            }
        }
    }

    private SimpleHogBug[] convert(List<HogsBugs> list, boolean isBug) {
        if (list == null)
            return null;
        SimpleHogBug[] result = new SimpleHogBug[list.size()];
        int size = list.size();
        for (int i = 0; i < size; ++i) {
            HogsBugs item = list.get(i);
            result[i] = new SimpleHogBug(fixName(item.getAppName()), isBug);
            result[i].setAppLabel(item.getAppLabel());
            result[i].setAppPriority(item.getAppPriority());
            result[i].setExpectedValue(item.getExpectedValue());
            result[i].setExpectedValueWithout(item.getExpectedValueWithout());
            result[i].setwDistance(item.getWDistance());
            result[i].setxVals(convert(item.getXVals()));
            result[i].setyVals(convert(item.getYVals()));
            result[i].setxValsWithout(convert(item.getXValsWithout()));
            result[i].setyValsWithout(convert(item.getYValsWithout()));
        }
        return result;
    }

    public static double[] convert(List<Double> dbls) {
        if (dbls == null)
            return new double[0];
        for (int j = 0; j < dbls.size(); ++j) {
            if (dbls.get(j) == 0.0) {
                dbls.remove(j);
                j--;
            }
        }
        double[] arr = new double[dbls.size()];
        for (int j = 0; j < dbls.size(); ++j) {
            arr[j] = dbls.get(j);
        }
        return arr;
    }

    private String fixName(String name) {
        if (name == null)
            return null;
        int idx = name.lastIndexOf(':');
        if (idx <= 0)
            idx = name.length();
        String n = name.substring(0, idx);
        return n;
    }

    public SimpleHogBug[] readBugReport() {
        Object o = readObject(BUGFILE);
        Log.d("CaratDataStorage", "Read Bugs: " + o);
        if (o == null || !(o instanceof SimpleHogBug[]))
            return null;
        SimpleHogBug[] r = (SimpleHogBug[]) o;
        bugData = new WeakReference<SimpleHogBug[]>(r);
        return r;
    }

    public SimpleHogBug[] readHogReport() {
        Object o = readObject(HOGFILE);
        Log.d("CaratDataStorage", "Read Hogs: " + o);
        if (o == null || !(o instanceof SimpleHogBug[]))
            return null;
        SimpleHogBug[] r = (SimpleHogBug[]) o;
        hogData = new WeakReference<SimpleHogBug[]>(r);
        return r;
    }
}