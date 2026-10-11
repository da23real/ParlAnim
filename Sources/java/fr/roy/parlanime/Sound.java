// SPDX-License-Identifier: GPL-3.0-only
// Copyright 2026 Roy / DA23REAL
package fr.roy.parlanime;
import android.content.*;import android.media.*;import android.net.Uri;import java.io.*;import java.nio.*;import java.util.*;
public final class Sound {
 public static class Prepared {public Data voice,mix;}
 static class Reader implements Closeable {
  final Data d;final RandomAccessFile f;byte[] page=new byte[0];long first=-1;
  Reader(Data data)throws IOException{d=data;f=new RandomAccessFile(d.file,"r");}
  double sample(long n,int ch,boolean loop)throws IOException{
   if(d.samples==0)return 0;if(loop)n=Math.floorMod(n,d.samples);else if(n<0||n>=d.samples)return 0;
   if(first<0||n<first||n>=first+page.length/(2*d.channels)){first=n;page=new byte[(int)Math.min(4096,d.samples-n)*2*d.channels];f.seek(44+n*2*d.channels);f.readFully(page);}
   int i=(int)(n-first)*d.channels*2+Math.min(ch,d.channels-1)*2;return (short)((page[i]&255)|(page[i+1]<<8))/32768.0;
  }
  float at(long output,int ch,boolean loop)throws IOException{double pos=output*d.rate/48000.0;long n=(long)pos;return(float)(sample(n,ch,loop)*(1-(pos-n))+sample(n+1,ch,loop)*(pos-n));}
  public void close()throws IOException{f.close();}
 }
 public static Prepared prepare(Data source,Data music,Voice.Settings voiceSettings,AudioChain.Settings audio,File directory,Progress progress)throws Exception{
  Prepared result=new Prepared();result.voice=new Data();result.mix=new Data();
  long total=(long)Math.ceil(source.seconds()*48000);String stamp=Long.toString(System.nanoTime());
  for(Data d:new Data[]{result.voice,result.mix}){d.rate=48000;d.channels=2;d.samples=total;}
  result.voice.file=new File(directory,"voice-"+stamp+".wav");result.mix.file=new File(directory,"mix-"+stamp+".wav");
  boolean success=false;
  try(Reader input=new Reader(source);Reader background=music==null?null:new Reader(music);RandomAccessFile vocal=new RandomAccessFile(result.voice.file,"rw");RandomAccessFile mixed=new RandomAccessFile(result.mix.file,"rw")){
   vocal.write(new byte[44]);mixed.write(new byte[44]);AudioChain chain=new AudioChain();float[] v=new float[960],m=new float[960];byte[] vb=new byte[1920],mb=new byte[1920];
   for(long offset=0;offset<total;offset+=480){if(progress.cancelled())throw new InterruptedIOException("Annulé");int count=(int)Math.min(480,total-offset);
    for(int i=0;i<count;i++)for(int ch=0;ch<2;ch++){v[i*2+ch]=input.at(offset+i,ch,false);m[i*2+ch]=background==null?0:background.at(offset+i,ch,true);}
    chain.process(v,m,count,2,48000,voiceSettings.gate,audio);
    for(int i=0;i<count*2;i++){int a=Math.round(v[i]*32767);int b=(int)Math.round(Math.max(-1,Math.min(1,v[i]+m[i]))*32767);vb[2*i]=(byte)a;vb[2*i+1]=(byte)(a>>8);mb[2*i]=(byte)b;mb[2*i+1]=(byte)(b>>8);}
    vocal.write(vb,0,count*4);mixed.write(mb,0,count*4);if(offset%48000==0)progress.update((int)(offset*100/Math.max(1,total)));
   }
   wavHeader(vocal,48000,2,total*4);wavHeader(mixed,48000,2,total*4);success=true;return result;
  }finally{if(!success){result.voice.file.delete();result.mix.file.delete();}}
 }
 public interface Progress {void update(int percent); boolean cancelled();}
 public static class Data {public File file;public int rate,channels;public long samples;public double seconds(){return samples/(double)rate;} }
 public static void wavHeader(RandomAccessFile f,int rate,int channels,long bytes)throws IOException{f.seek(0);f.writeBytes("RIFF");le(f,36+bytes,4);f.writeBytes("WAVEfmt ");le(f,16,4);le(f,1,2);le(f,channels,2);le(f,rate,4);le(f,rate*channels*2,4);le(f,channels*2,2);le(f,16,2);f.writeBytes("data");le(f,bytes,4);}
 static void le(RandomAccessFile f,long n,int bytes)throws IOException{for(int i=0;i<bytes;i++)f.write((int)(n>>(i*8)));}
 public static Data decode(Context c,Uri uri,File out,Progress progress)throws Exception{
  MediaExtractor ex=new MediaExtractor();MediaCodec codec=null;
  try(RandomAccessFile f=new RandomAccessFile(out,"rw")){
   f.setLength(0);f.write(new byte[44]);ex.setDataSource(c,uri,null);int track=-1;for(int i=0;i<ex.getTrackCount();i++)if(ex.getTrackFormat(i).getString(MediaFormat.KEY_MIME).startsWith("audio/")){track=i;break;}if(track<0)throw new IOException("Ce fichier ne contient pas d’audio.");
   ex.selectTrack(track);MediaFormat fmt=ex.getTrackFormat(track);long duration=fmt.containsKey(MediaFormat.KEY_DURATION)?fmt.getLong(MediaFormat.KEY_DURATION):0;
   codec=MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME));codec.configure(fmt,null,null,0);codec.start();Data d=new Data();d.file=out;d.rate=fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE);d.channels=fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT);int encoding=AudioFormat.ENCODING_PCM_16BIT;
   boolean input=false,done=false;MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();long last=System.currentTimeMillis();
   while(!done){if(progress.cancelled())throw new InterruptedIOException("Annulé");boolean moved=false;
    if(!input){int idx=codec.dequeueInputBuffer(10000);if(idx>=0){ByteBuffer b=codec.getInputBuffer(idx);int n=ex.readSampleData(b,0);if(n<0){codec.queueInputBuffer(idx,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);input=true;}else{codec.queueInputBuffer(idx,0,n,ex.getSampleTime(),0);ex.advance();}moved=true;}}
    int idx=codec.dequeueOutputBuffer(info,10000);if(idx==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){MediaFormat o=codec.getOutputFormat();d.rate=o.getInteger(MediaFormat.KEY_SAMPLE_RATE);d.channels=o.getInteger(MediaFormat.KEY_CHANNEL_COUNT);if(o.containsKey(MediaFormat.KEY_PCM_ENCODING))encoding=o.getInteger(MediaFormat.KEY_PCM_ENCODING);if(encoding!=AudioFormat.ENCODING_PCM_16BIT&&encoding!=AudioFormat.ENCODING_PCM_FLOAT)throw new IOException("Format audio PCM non pris en charge : "+encoding);moved=true;}
    else if(idx>=0){ByteBuffer b=codec.getOutputBuffer(idx);b.position(info.offset);b.limit(info.offset+info.size);b.order(ByteOrder.LITTLE_ENDIAN);if(encoding==AudioFormat.ENCODING_PCM_FLOAT){while(b.remaining()>=4){int v=(int)(Math.max(-1,Math.min(1,b.getFloat()))*32767);f.write(v&255);f.write((v>>8)&255);}}else{byte[] a=new byte[b.remaining()];b.get(a);f.write(a);}done=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;codec.releaseOutputBuffer(idx,false);if(duration>0)progress.update((int)Math.min(99,info.presentationTimeUs*100/duration));moved=true;}
    if(f.length()>0x70000000L)throw new IOException("Audio trop volumineux pour ce téléphone.");if(moved)last=System.currentTimeMillis();else if(System.currentTimeMillis()-last>15000)throw new IOException("Le décodeur audio ne répond plus.");
   }
   long bytes=f.length()-44;d.samples=bytes/(d.channels*2);if(d.samples==0)throw new IOException("Audio vide.");wavHeader(f,d.rate,d.channels,bytes);return d;
  }finally{if(codec!=null){try{codec.stop();}catch(Exception ignored){}codec.release();}ex.release();}
 }
 public static ArrayList<Voice.Frame> analyze(Data d,Voice.Settings settings,Progress p)throws Exception{
  ArrayList<Voice.Frame> frames=new ArrayList<>();Voice engine=new Voice(settings);double[] window=new double[1024];
  try(RandomAccessFile f=new RandomAccessFile(d.file,"r")){
   long total=(long)Math.ceil(d.seconds()*50);for(int k=0;k<total;k++){if(p.cancelled())throw new InterruptedIOException("Annulé");System.arraycopy(window,320,window,0,704);
    long start=(long)k*d.rate/50,end=Math.min(d.samples,(long)(k+1)*d.rate/50);int count=(int)(end-start);byte[] b=new byte[count*d.channels*2];f.seek(44+start*d.channels*2);f.readFully(b);
    for(int j=0;j<320;j++){int n=(int)((long)j*d.rate/16000);double v=0;if(n<count){for(int ch=0;ch<d.channels;ch++){int at=(n*d.channels+ch)*2;v+=(short)((b[at]&255)|(b[at+1]<<8))/32768.0;}v/=d.channels;}window[704+j]=v;}
    frames.add(engine.process(window.clone()));if(k%25==0)p.update((int)(k*100/total));
   }
  }return frames;
 }
}
