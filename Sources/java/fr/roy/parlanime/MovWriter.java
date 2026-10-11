// SPDX-License-Identifier: GPL-3.0-only
// Copyright 2026 Roy / DA23REAL
package fr.roy.parlanime;
import java.io.*;import java.util.*;
/** QuickTime PNG RGBA + little endian PCM. No external encoder dependency. */
public final class MovWriter implements Closeable {
 final RandomAccessFile file;final ArrayList<Integer> sizes=new ArrayList<>(),offsets=new ArrayList<>();final int width,height,fps;long audioOffset,audioBytes;int rate,channels;boolean finished;
 public MovWriter(File path,int w,int h,int fps)throws IOException{file=new RandomAccessFile(path,"rw");file.setLength(0);width=w;height=h;this.fps=fps;file.write(atom("ftyp",bytes(d->{d.writeBytes("qt  ");d.writeInt(0);d.writeBytes("qt  ");})));file.writeInt(0);file.writeBytes("mdat");}
 public void frame(byte[] png)throws IOException{if(file.getFilePointer()+png.length>0x70000000L)throw new IOException("MOV trop volumineux ; utilise une durée plus courte ou le MP4.");offsets.add((int)file.getFilePointer());sizes.add(png.length);file.write(png);}
 public void audio(Sound.Data data)throws IOException{audioOffset=file.getFilePointer();rate=data.rate;channels=data.channels;try(FileInputStream in=new FileInputStream(data.file)){long skip=44;while(skip>0){long n=in.skip(skip);if(n<=0)throw new EOFException();skip-=n;}byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){if(file.getFilePointer()+n>0x70000000L)throw new IOException("MOV trop volumineux.");file.write(b,0,n);audioBytes+=n;}}}
 interface Write {void run(DataOutputStream d)throws IOException;}
 static byte[] bytes(Write w)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);w.run(d);d.flush();return b.toByteArray();}
 static byte[] atom(String type,byte[]... children)throws IOException{return bytes(d->{int size=8;for(byte[] c:children)size+=c.length;d.writeInt(size);d.writeBytes(type);for(byte[] c:children)d.write(c);});}
 static void zeros(DataOutputStream d,int n)throws IOException{d.write(new byte[n]);}
 static void matrix(DataOutputStream d)throws IOException{d.writeInt(65536);d.writeInt(0);d.writeInt(0);d.writeInt(0);d.writeInt(65536);d.writeInt(0);d.writeInt(0);d.writeInt(0);d.writeInt(0x40000000);}
 byte[] track(boolean video)throws IOException{
  int timescale=video?fps:rate;int count=video?sizes.size():(int)(audioBytes/(channels*2));int duration=(int)Math.round(count*1000.0/timescale);
  byte[] tkhd=atom("tkhd",bytes(d->{d.writeInt(7);zeros(d,8);d.writeInt(video?1:2);d.writeInt(0);d.writeInt(duration);zeros(d,8);d.writeShort(0);d.writeShort(0);d.writeShort(video?0:256);d.writeShort(0);matrix(d);d.writeInt(video?width<<16:0);d.writeInt(video?height<<16:0);}));
  byte[] mdhd=atom("mdhd",bytes(d->{d.writeInt(0);zeros(d,8);d.writeInt(timescale);d.writeInt(count);d.writeShort(0);d.writeShort(0);}));
  byte[] hdlr=atom("hdlr",bytes(d->{d.writeInt(0);d.writeInt(0);d.writeBytes(video?"vide":"soun");zeros(d,12);d.writeBytes(video?"ParlAnime video\0":"ParlAnime audio\0");}));
  byte[] entry=video?atom("png ",bytes(d->{zeros(d,6);d.writeShort(1);zeros(d,16);d.writeShort(width);d.writeShort(height);d.writeInt(72<<16);d.writeInt(72<<16);d.writeInt(0);d.writeShort(1);zeros(d,32);d.writeShort(32);d.writeShort(-1);})):
   atom("sowt",bytes(d->{zeros(d,6);d.writeShort(1);zeros(d,8);d.writeShort(channels);d.writeShort(16);d.writeShort(0);d.writeShort(0);d.writeInt(rate<<16);}));
  byte[] stsd=atom("stsd",bytes(d->{d.writeInt(0);d.writeInt(1);d.write(entry);}));
  byte[] stts=atom("stts",bytes(d->{d.writeInt(0);d.writeInt(1);d.writeInt(count);d.writeInt(1);}));
  byte[] stsc=atom("stsc",bytes(d->{d.writeInt(0);d.writeInt(1);d.writeInt(1);d.writeInt(video?1:count);d.writeInt(1);}));
  byte[] stsz=atom("stsz",bytes(d->{d.writeInt(0);d.writeInt(video?0:channels*2);d.writeInt(count);if(video)for(int n:sizes)d.writeInt(n);}));
  byte[] stco=atom("stco",bytes(d->{d.writeInt(0);d.writeInt(video?offsets.size():1);if(video)for(int n:offsets)d.writeInt(n);else d.writeInt((int)audioOffset);}));
  byte[] url=atom("url ",bytes(d->d.writeInt(1)));byte[] dinf=atom("dinf",atom("dref",bytes(d->{d.writeInt(0);d.writeInt(1);d.write(url);})));
  byte[] header=video?atom("vmhd",bytes(d->{d.writeInt(1);zeros(d,8);})):atom("smhd",bytes(d->zeros(d,8)));
  return atom("trak",tkhd,atom("mdia",mdhd,hdlr,atom("minf",header,dinf,atom("stbl",stsd,stts,stsc,stsz,stco))));
 }
 public void finish()throws IOException{long end=file.getFilePointer();file.seek(20);file.writeInt((int)(end-20));file.seek(end);int duration=(int)Math.ceil(Math.max(sizes.size()*1000.0/fps,audioBytes*1000.0/(rate*channels*2)));
  byte[] mvhd=atom("mvhd",bytes(d->{d.writeInt(0);zeros(d,8);d.writeInt(1000);d.writeInt(duration);d.writeInt(65536);d.writeShort(256);zeros(d,10);matrix(d);zeros(d,24);d.writeInt(3);}));file.write(atom("moov",mvhd,track(true),track(false)));finished=true;
 }
 public void close()throws IOException{file.close();}
}
