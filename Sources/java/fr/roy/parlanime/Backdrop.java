// SPDX-License-Identifier: GPL-3.0-only
// Copyright 2026 Roy / DA23REAL
package fr.roy.parlanime;
import android.content.*;import android.graphics.*;import android.net.Uri;import java.io.*;
/** Shared background composition for preview and MP4. MOV never calls it. */
public final class Backdrop {
 public static final String[] IDS={"studio-radio","plein-air","studio-youtubeur","scene-spectacle","salon-cosy"};
 public Bitmap image;public int color=0xfff2effa;public String id="";
 public void load(Context c,String value)throws IOException{Bitmap next=null;if(value.equals("custom")){File file=new File(c.getFilesDir(),"background.png");if(file.exists())next=BitmapFactory.decodeFile(file.getPath());}else if(!value.isEmpty()){try(InputStream in=c.getAssets().open("backgrounds/"+value+".png")){next=BitmapFactory.decodeStream(in);}if(next==null)throw new IOException("Image de fond illisible.");}image=next;id=next==null?"":value;}
 public void importImage(Context c,Uri uri)throws IOException{byte[] data;try(InputStream in=c.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Image inaccessible.");data=Packs.readLimited(in,32*1024*1024);}BitmapFactory.Options opt=new BitmapFactory.Options();opt.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,opt);if(opt.outWidth<=0||opt.outHeight<=0)throw new IOException("Image de fond illisible.");int sample=1;while(Math.max(opt.outWidth,opt.outHeight)/sample>1536)sample*=2;opt.inJustDecodeBounds=false;opt.inSampleSize=sample;Bitmap next=BitmapFactory.decodeByteArray(data,0,data.length,opt);if(next==null)throw new IOException("Image de fond illisible.");File temp=new File(c.getFilesDir(),"background-new.png");try(FileOutputStream out=new FileOutputStream(temp)){if(!next.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("Écriture de l’image impossible.");}if(!temp.renameTo(new File(c.getFilesDir(),"background.png")))throw new IOException("Impossible de conserver l’image de fond.");image=next;id="custom";}
 public void draw(Canvas c,int width,int height){c.drawColor(color);if(image!=null){float scale=Math.max(width/(float)image.getWidth(),height/(float)image.getHeight());float w=image.getWidth()*scale,h=image.getHeight()*scale;c.drawBitmap(image,null,new RectF((width-w)/2,(height-h)/2,(width+w)/2,(height+h)/2),new Paint(3));}}
}
