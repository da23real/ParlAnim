// SPDX-License-Identifier: GPL-3.0-only
// Copyright 2026 Roy / DA23REAL
package fr.roy.parlanime;

/** Stateful, sample-rate independent gate followed by music ducking. */
public final class AudioChain {
 public static class Settings {
  public boolean gate=true,radio=false;
  public double music=.80, reduction=19, gateDb=-40;
  public int attack=35,release=450,gateAttack=10,gateHold=300,gateRelease=1000;
 }
 private double gateGain=0,duckGain=1; private long holdSamples;
 public double musicGain(){return duckGain;}
 public void process(float[] voice,float[] music,int count,int channels,int rate,double thresholdDb,Settings s){
  double power=0;for(int i=0;i<count*channels;i++)power+=voice[i]*voice[i];
  double threshold=Math.pow(10,s.gateDb/20);
  boolean open=Math.sqrt(power/Math.max(1,count*channels))>=threshold;
  if(open)holdSamples=(long)rate*s.gateHold/1000;
  if(!s.gate)gateGain=1;
  double gatedPower=0;
  for(int i=0;i<count;i++){
   double gateTarget=(!s.gate||open||holdSamples>0)?1:0;
   if(!open&&holdSamples>0)holdSamples--;
   double step=1.0/(rate*Math.max(1,gateTarget>gateGain?s.gateAttack:s.gateRelease)/1000.0);
   gateGain+=Math.max(-step,Math.min(step,gateTarget-gateGain));
   for(int ch=0;ch<channels;ch++){int at=i*channels+ch;voice[at]*=gateGain;gatedPower+=voice[at]*voice[at];}
  }
  // Detector sees only the gated voice, never the background track.
  boolean speaking=Math.sqrt(gatedPower/Math.max(1,count*channels))>=threshold;
  double target=s.radio&&speaking?Math.pow(10,-s.reduction/20):1;
  double coefficient=Math.exp(-1.0/(rate*Math.max(1,target<duckGain?s.attack:s.release)/1000.0));
  if(music!=null)for(int i=0;i<count;i++){
   duckGain=target+(duckGain-target)*coefficient;
   for(int ch=0;ch<channels;ch++)music[i*channels+ch]*=s.music*duckGain;
  }
 }
}
