// SPDX-License-Identifier: GPL-3.0-only
// Copyright 2026 Roy / DA23REAL
package fr.roy.parlanime;

/** Port of Snkoz VoiceEngineR8 acoustic heuristics. No trained speech model. */
public final class Voice {
 public static final int RATE=16000, HOP=320;
 public static class Settings {
  public double gate=-32, low=-40, high=-6; public int smooth=220, whisper=50, loud=90;
  public double normalDb=-20,loudDb=-14;
  public double normal(){return Double.isNaN(normalDb)?low+(whisper-1.5)/126.0*(high-low):normalDb;}
  public double cries(){return Double.isNaN(loudDb)?low+(loud-1.5)/126.0*(high-low):loudDb;}
  public void threshold(int index,double db){
   normalDb=normal();loudDb=cries();db=Math.max(-80,Math.min(-1,db));
   if(index==0){gate=Math.min(-3,db);normalDb=Math.max(normalDb,gate+1);loudDb=Math.max(loudDb,normalDb+1);}
   else if(index==1){normalDb=Math.max(-79,Math.min(-2,db));gate=Math.min(gate,normalDb-1);loudDb=Math.max(loudDb,normalDb+1);}
   else{loudDb=Math.max(-78,db);normalDb=Math.min(normalDb,loudDb-1);gate=Math.min(gate,normalDb-1);}
  }
  public int velocity(double db){
   if(db<normal())return interpolate(db,gate,normal(),1,whisper-1);
   if(db<cries())return interpolate(db,normal(),cries(),whisper,loud-1);
   return interpolate(db,cries(),0,loud,127);
  }
  static int interpolate(double db,double a,double b,int first,int last){return Math.max(first,Math.min(last,(int)Math.round(first+(last-first)*(db-a)/(b-a))));}
  public void validate(){if(!(gate<normal()&&normal()<cries()&&cries()<0)||whisper<2||loud<=whisper||loud>127||smooth<0||smooth>1000)throw new IllegalArgumentException("Seuils attendus : attente < normal < cris < 0 dB.");}
 }
 public static class Frame {public double db=-100; public int tick,shape,velocity,note=-1; public int bank(Settings s){return velocity<s.whisper?0:velocity<s.loud?1:2;}}
 private final Settings s; private double previous=-100, envelope=-100; private int tick=0; private int lastShape=0,candidate=0,count=0;
 public Voice(Settings settings){s=settings;}
 public Frame process(double[] x) {
  Frame f=new Frame();f.tick=tick++; double mean=0;for(double a:x)mean+=a;mean/=1024;double energy=0;
  for(int i=0;i<1024;i++){x[i]-=mean;energy+=x[i]*x[i];}
  f.db=10*Math.log10(energy/1024+1e-12);
  double alpha=s.smooth==0?1:1-Math.exp(-20.0/s.smooth);
  envelope=envelope<-99?f.db:envelope+alpha*(f.db-envelope);
  if(f.db>s.gate){
   double hz=pitch(x);f.note=hz>0?Math.max(0,Math.min(127,(int)Math.round(69+12*Math.log(hz/440)/Math.log(2)))):60;
   double[] re=new double[1024],im=new double[1024],p=new double[512];
   for(int j=0;j<1024;j++)re[j]=x[j]*(.5-.5*Math.cos(2*Math.PI*j/1023));fft(re,im);
   for(int j=1;j<512;j++)p[j]=re[j]*re[j]+im[j]*im[j];
   double total=band(p,80,7900)+1e-12,low=band(p,80,450)/total,high=band(p,4000,7900)/total,mid=band(p,1800,4000)/total;
   if(f.db-previous>9&&(hz==0||high>.12))f.shape=1;
   else if(high>.42)f.shape=3;else if(mid>.42&&high>.09)f.shape=4;else if(hz==0)f.shape=5;
   else if(low>.90&&band(p,700,1800)/total<.035)f.shape=2;
   else {
    double[] lr=new double[1024],li=new double[1024];
    for(int j=1;j<512;j++)lr[j]=lr[1024-j]=Math.log(p[j]*(1+.95*.95-1.9*Math.cos(2*Math.PI*j/1024))+1e-12);
    fft(lr,li);for(int j=0;j<1024;j++){if(j>24&&j<1000){lr[j]=0;li[j]=0;}else{lr[j]/=1024;li[j]=-li[j]/1024;}}fft(lr,li);
    int f1=16;for(int j=13;j<=64;j++)if(lr[j]>lr[f1])f1=j;
    int f2=Math.max(f1+16,58);for(int j=Math.max(f1+16,58);j<205;j++)if(lr[j]>lr[f2])f2=j;
    f.shape=f1*15.625>620?6:f2*15.625>1650?7:8;
   }
   f.velocity=s.velocity(envelope);
  }
  // A causal two-frame debounce preserves live/offline equivalence and sharp attacks.
  if(f.shape==candidate)count++;else{candidate=f.shape;count=1;}
  if(s.smooth==0||f.shape==0||f.shape==1||count>=2)lastShape=f.shape;
  f.shape=lastShape;if(f.shape==0){f.velocity=0;f.note=-1;}previous=f.db;return f;
 }
 static double band(double[] p,double lo,double hi){double v=0;for(int i=Math.max(1,(int)(lo*1024/16000));i<Math.min(p.length,(int)(hi*1024/16000));i++)v+=p[i];return v;}
 static double pitch(double[] x){double[] d=new double[321];double sum=0;for(int lag=1;lag<=320;lag++){double v=0;for(int i=0;i<640;i++){double a=x[i]-x[i+lag];v+=a*a;}sum+=v;d[lag]=sum>1e-15?v*lag/sum:1;}for(int t=26;t<319;t++)if(d[t]<.18){while(t+1<320&&d[t+1]<d[t])t++;double den=d[t-1]-2*d[t]+d[t+1];double sub=Math.abs(den)>1e-9?.5*(d[t-1]-d[t+1])/den:0;return 16000/(t+Math.max(-.5,Math.min(.5,sub)));}return 0;}
 static void fft(double[] re,double[] im){int n=re.length;for(int i=1,j=0;i<n;i++){int bit=n>>1;for(;(j&bit)!=0;bit>>=1)j^=bit;j^=bit;if(i<j){double a=re[i];re[i]=re[j];re[j]=a;}}for(int len=2;len<=n;len<<=1){double ar=Math.cos(-2*Math.PI/len),ai=Math.sin(-2*Math.PI/len);for(int i=0;i<n;i+=len){double wr=1,wi=0;for(int j=0;j<len/2;j++){int u=i+j,v=u+len/2;double tr=re[v]*wr-im[v]*wi,ti=re[v]*wi+im[v]*wr;re[v]=re[u]-tr;im[v]=im[u]-ti;re[u]+=tr;im[u]+=ti;double w=wr*ar-wi*ai;wi=wr*ai+wi*ar;wr=w;}}}}
}
