// SPDX-License-Identifier: GPL-3.0-only
// Copyright 2026 Roy / DA23REAL
package fr.roy.parlanime;
import android.content.*;import android.database.*;import android.net.Uri;import android.os.*;import java.io.*;
public class ShareProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 private File file(Uri u)throws FileNotFoundException{String n=u.getLastPathSegment();if(n==null||!n.matches("ParlAnime-[0-9]+\\.(mp4|mov|mid)"))throw new FileNotFoundException();return new File(getContext().getFilesDir(),n);}
 public String getType(Uri u){return u.toString().endsWith(".mov")?"video/quicktime":u.toString().endsWith(".mid")?"audio/midi":"video/mp4";}
 public ParcelFileDescriptor openFile(Uri u,String m)throws FileNotFoundException{if(!"r".equals(m))throw new FileNotFoundException();return ParcelFileDescriptor.open(file(u),ParcelFileDescriptor.MODE_READ_ONLY);}
 public Cursor query(Uri u,String[] p,String s,String[] a,String o){try{File f=file(u);MatrixCursor c=new MatrixCursor(new String[]{"_display_name","_size"});c.addRow(new Object[]{f.getName(),f.length()});return c;}catch(Exception e){return null;}}
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
