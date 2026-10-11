// SPDX-License-Identifier: GPL-3.0-only
// Copyright 2026 Roy / DA23REAL
package fr.roy.parlanime;
import android.graphics.*;import android.media.*;import java.io.*;import java.nio.*;import java.util.*;
public final class Export {
 public static final int SIZE=512,FPS=25;
 public static void mov(File out,Packs.Pack pack,Sound.Data sound,ArrayList<Voice.Frame> frames,Voice.Settings s,Sound.Progress p)throws Exception{
  final int total=(int)Math.ceil(sound.seconds()*FPS);
  android.util.Log.i("ParlAnimeTest","MOV total="+total+" seconds="+sound.seconds());
  HashMap<Bitmap,byte[]> compressed=new HashMap<>();
  try(MovWriter w=new MovWriter(out,SIZE,SIZE,FPS)){
   for(int index=0;index<total;index++){
    if(p.cancelled())throw new InterruptedIOException("Annulé");
    Voice.Frame frame=frames.get(Math.min(frames.size()-1,index*2));
    Bitmap pose=Packs.pose(pack,frame,s);byte[] png=compressed.get(pose);
    if(png==null){Bitmap b=Packs.render(pack,frame,s,SIZE,true);ByteArrayOutputStream buffer=new ByteArrayOutputStream();if(!b.compress(Bitmap.CompressFormat.PNG,100,buffer))throw new IOException("Compression PNG impossible.");b.recycle();png=buffer.toByteArray();compressed.put(pose,png);}
    w.frame(png);if(index%25==0)android.util.Log.i("ParlAnimeTest","MOV frame="+index+"/"+total);
    p.update((int)(index*95L/total));
   }
   android.util.Log.i("ParlAnimeTest","MOV audio");w.audio(sound);w.finish();p.update(100);
  }
 } static void encodeAudio(File out,Sound.Data sound,Sound.Progress p)throws Exception{
  MediaCodec codec=MediaCodec.createEncoderByType("audio/mp4a-latm");MediaMuxer mux=null;
  try(RandomAccessFile in=new RandomAccessFile(sound.file,"r")){
   MediaFormat format=MediaFormat.createAudioFormat("audio/mp4a-latm",sound.rate,sound.channels);format.setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectLC);format.setInteger(MediaFormat.KEY_BIT_RATE,128000);codec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);codec.start();mux=new MediaMuxer(out.getPath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);int track=-1;long pos=0;in.seek(44);boolean input=false,done=false;MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
   while(!done){if(p.cancelled())throw new InterruptedIOException("Annulé");if(!input){int idx=codec.dequeueInputBuffer(10000);if(idx>=0){ByteBuffer b=codec.getInputBuffer(idx);int size=b.capacity()/(sound.channels*2)*(sound.channels*2);byte[] a=new byte[size];int n=in.read(a);long pts=pos*1000000L/(sound.rate*sound.channels*2);if(n<0){input=true;codec.queueInputBuffer(idx,0,0,pts,MediaCodec.BUFFER_FLAG_END_OF_STREAM);}else{b.clear();b.put(a,0,n);codec.queueInputBuffer(idx,0,n,pts,0);pos+=n;}}}
    int idx=codec.dequeueOutputBuffer(info,10000);if(idx==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){track=mux.addTrack(codec.getOutputFormat());mux.start();}else if(idx>=0){if(info.size>0&&(info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0){ByteBuffer b=codec.getOutputBuffer(idx);b.position(info.offset);b.limit(info.offset+info.size);mux.writeSampleData(track,b,info);}done=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;codec.releaseOutputBuffer(idx,false);}
   }
  }finally{try{codec.stop();}catch(Exception e){}codec.release();if(mux!=null){try{mux.stop();}catch(Exception e){}mux.release();}}
 }
 public static void mp4(File out,Packs.Pack pack,Sound.Data sound,ArrayList<Voice.Frame> frames,Voice.Settings s,Sound.Progress p)throws Exception{
  mp4(out,pack,sound,frames,s,new Backdrop(),p);}
 public static void mp4(File out,Packs.Pack pack,Sound.Data sound,ArrayList<Voice.Frame> frames,Voice.Settings s,Backdrop background,Sound.Progress p)throws Exception{
  File audio=new File(out.getParentFile(),"encoding-audio.m4a");encodeAudio(audio,sound,p);MediaExtractor ax=new MediaExtractor();MediaCodec codec=MediaCodec.createEncoderByType("video/avc");MediaMuxer mux=null;
  try{
   MediaFormat f=MediaFormat.createVideoFormat("video/avc",SIZE,SIZE);f.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);f.setInteger(MediaFormat.KEY_BIT_RATE,2000000);f.setInteger(MediaFormat.KEY_FRAME_RATE,FPS);f.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1);codec.configure(f,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);codec.start();
   mux=new MediaMuxer(out.getPath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);ax.setDataSource(audio.getPath());ax.selectTrack(0);int at=mux.addTrack(ax.getTrackFormat(0)),vt=-1;boolean started=false,input=false,done=false;int frame=0,total=(int)Math.ceil(sound.seconds()*FPS);MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
   while(!done){if(p.cancelled())throw new InterruptedIOException("Annulé");if(!input){int idx=codec.dequeueInputBuffer(10000);if(idx>=0){long pts=frame*1000000L/FPS;if(frame>=total){android.util.Log.i("ParlAnimeTest","MP4 input EOS");codec.queueInputBuffer(idx,0,0,pts,MediaCodec.BUFFER_FLAG_END_OF_STREAM);input=true;}else{
      Bitmap bitmap=Packs.render(pack,frames.get(Math.min(frames.size()-1,frame*2)),s,SIZE,false,background);Image im=codec.getInputImage(idx);if(im==null){bitmap.recycle();throw new IOException("L’encodeur vidéo de ce téléphone ne fournit pas de surface YUV compatible.");}fill(im,bitmap);im.close();bitmap.recycle();codec.queueInputBuffer(idx,0,SIZE*SIZE*3/2,pts,0);frame++;if(frame%25==0)android.util.Log.i("ParlAnimeTest","MP4 frame="+frame+"/"+total);p.update((int)(frame*90L/total));
    }}}
    int idx=codec.dequeueOutputBuffer(info,10000);if(idx==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){vt=mux.addTrack(codec.getOutputFormat());mux.start();started=true;}else if(idx>=0){if(info.size>0&&(info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0){ByteBuffer b=codec.getOutputBuffer(idx);b.position(info.offset);b.limit(info.offset+info.size);mux.writeSampleData(vt,b,info);}done=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;codec.releaseOutputBuffer(idx,false);}
   }
   android.util.Log.i("ParlAnimeTest","MP4 audio mux");ByteBuffer b=ByteBuffer.allocate(262144);while(true){if(p.cancelled())throw new InterruptedIOException("Annulé");b.clear();int n=ax.readSampleData(b,0);if(n<0)break;info.set(0,n,ax.getSampleTime(),ax.getSampleFlags());mux.writeSampleData(at,b,info);ax.advance();}if(!started)throw new IOException("Vidéo vide.");p.update(100);
  }finally{ax.release();try{codec.stop();}catch(Exception e){}codec.release();if(mux!=null){try{mux.stop();}catch(Exception e){}mux.release();}audio.delete();}
 }
 static void fill(Image image,Bitmap bitmap){int[] rgb=new int[SIZE*SIZE];bitmap.getPixels(rgb,0,SIZE,0,0,SIZE,SIZE);Image.Plane[] planes=image.getPlanes();for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++){int color=rgb[y*SIZE+x],r=(color>>16)&255,g=(color>>8)&255,b=color&255;put(planes[0],x,y,((66*r+129*g+25*b+128)>>8)+16);if((x&1)==0&&(y&1)==0){int rr=0,gg=0,bb=0;for(int yy=0;yy<2;yy++)for(int xx=0;xx<2;xx++){int c=rgb[(y+yy)*SIZE+x+xx];rr+=(c>>16)&255;gg+=(c>>8)&255;bb+=c&255;}rr/=4;gg/=4;bb/=4;put(planes[1],x/2,y/2,((-38*rr-74*gg+112*bb+128)>>8)+128);put(planes[2],x/2,y/2,((112*rr-94*gg-18*bb+128)>>8)+128);}}}
 static void put(Image.Plane p,int x,int y,int v){p.getBuffer().put(y*p.getRowStride()+x*p.getPixelStride(),(byte)Math.max(0,Math.min(255,v)));}
 public static void midi(File out,ArrayList<Voice.Frame> frames)throws IOException{
  ByteArrayOutputStream body=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(body);d.write(new byte[]{0,(byte)255,81,3,7,(byte)161,32});int lastTick=0,lastVel=-1,lastShape=-1,lastNote=-1;int[] values={0,16,32,48,64,80,96,112,127};
  for(int i=0;i<frames.size();i++){Voice.Frame f=frames.get(i);int t=i*20;if(f.shape!=lastShape){variable(d,t-lastTick);d.write(new byte[]{(byte)176,20,(byte)values[f.shape]});lastTick=t;lastShape=f.shape;}if(f.velocity!=lastVel||f.note!=lastNote){if(lastNote>=0){variable(d,t-lastTick);d.write(new byte[]{(byte)128,(byte)lastNote,0});lastTick=t;}if(f.velocity>0){variable(d,t-lastTick);d.write(new byte[]{(byte)144,(byte)f.note,(byte)f.velocity});lastTick=t;}lastNote=f.velocity>0?f.note:-1;lastVel=f.velocity;}}
  int end=frames.size()*20;if(lastNote>=0){variable(d,end-lastTick);d.write(new byte[]{(byte)128,(byte)lastNote,0});lastTick=end;}variable(d,end-lastTick);d.write(new byte[]{(byte)176,20,0});d.write(new byte[]{0,(byte)255,47,0});try(DataOutputStream file=new DataOutputStream(new FileOutputStream(out))){file.writeBytes("MThd");file.writeInt(6);file.writeShort(0);file.writeShort(1);file.writeShort(500);file.writeBytes("MTrk");file.writeInt(body.size());body.writeTo(file);}
 }
 public static void snkoz(File out,Sound.Data sound,ArrayList<Voice.Frame> frames,Sound.Progress progress)throws Exception{
  File mid=File.createTempFile("snkoz-",".mid",out.getParentFile());try{midi(mid,frames);try(java.util.zip.ZipOutputStream zip=new java.util.zip.ZipOutputStream(new FileOutputStream(out))){File[] files={mid,sound.file};String[] names={"ParlAnime.mid","ParlAnime.wav"};for(int i=0;i<2;i++){zip.putNextEntry(new java.util.zip.ZipEntry(names[i]));try(InputStream in=new FileInputStream(files[i])){byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1){if(progress.cancelled())throw new InterruptedIOException("Annulé");zip.write(buffer,0,n);}}zip.closeEntry();}zip.putNextEntry(new java.util.zip.ZipEntry("LISEZ-MOI.txt"));zip.write("Snkoz : décompresse puis importe ParlAnime.mid et ParlAnime.wav au même point de départ. MIDI canal 1 : notes et vélocité, CC20 pour la bouche. Audio WAV avec gate et musique éventuelle. Durées et départ communs. Seuils de banque usuels : 50 / 90.\n".getBytes("UTF-8"));zip.closeEntry();}}finally{mid.delete();}progress.update(100);
 }
 static void variable(DataOutputStream out,int v)throws IOException{int b=v&127;while((v>>=7)>0)b=(b<<8)|((v&127)|128);while(true){out.write(b);if((b&128)==0)break;b>>=8;}}
}


