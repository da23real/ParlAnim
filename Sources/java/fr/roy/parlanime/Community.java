// SPDX-License-Identifier: GPL-3.0-only
// Copyright 2026 Roy / DA23REAL
package fr.roy.parlanime;
import android.content.Context;import android.graphics.*;import android.util.Base64;import java.io.*;import java.net.*;import java.security.*;import org.json.*;

public final class Community {
 static JSONObject config(Context c)throws Exception{try(InputStream in=c.getAssets().open("community.json")){return new JSONObject(new String(Packs.readLimited(in,65536),"UTF-8"));}}
 static String endpoint(Context c,String key)throws Exception{String url=config(c).optString(key,"");if(url.isEmpty())throw new IOException("La communauté n’est pas encore en ligne. Destination prévue : DA23REAL / Parl'Anim. La mise en ligne attend la validation du prototype.");URL parsed=new URL(url);if(!parsed.getProtocol().equals("https")||parsed.getUserInfo()!=null)throw new IOException("Le service doit utiliser HTTPS.");return url.replaceAll("/+$","");}
 static byte[] request(String url,byte[] body,int maximum,Sound.Progress p)throws Exception{
  HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();connection.setConnectTimeout(15000);connection.setReadTimeout(20000);connection.setInstanceFollowRedirects(false);
  try{if(body!=null){connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json; charset=utf-8");connection.setFixedLengthStreamingMode(body.length);try(OutputStream out=connection.getOutputStream()){for(int pos=0;pos<body.length;pos+=32768){if(p.cancelled())throw new InterruptedIOException("Annulé");int n=Math.min(32768,body.length-pos);out.write(body,pos,n);p.update(pos*100/body.length);}}}
   int status=connection.getResponseCode();if(status!=(body==null?200:201))throw new IOException("Service Communauté : réponse "+status+". Aucun dépôt confirmé.");
   try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] chunk=new byte[16384];int n;while((n=in.read(chunk))!=-1){if(p.cancelled())throw new InterruptedIOException("Annulé");if(out.size()+n>maximum)throw new IOException("Réponse trop volumineuse.");out.write(chunk,0,n);}return out.toByteArray();}
  }finally{connection.disconnect();}
 }
 static JSONArray catalog(Context c,Sound.Progress p)throws Exception{return new JSONObject(new String(request(endpoint(c,"catalog")+"/catalog.json",null,1024*1024,p),"UTF-8")).getJSONArray("packs");}
 static Packs.Pack download(Context c,JSONObject item,Sound.Progress p)throws Exception{
  String id=item.getString("id");if(!id.matches("[0-9a-f]{32}"))throw new IOException("Identifiant de personnage invalide.");byte[] png=request(endpoint(c,"catalog")+"/packs/"+id+".png",null,16*1024*1024,p);StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(png))hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));if(!hash.toString().equals(item.getString("sha256")))throw new IOException("Téléchargement incomplet : empreinte incorrecte.");return Packs.importSheet(c,new ByteArrayInputStream(png),item.getString("name"));
 }
 static String submit(Context c,Packs.Pack pack,String author,String license,Sound.Progress p)throws Exception{
  String destination=endpoint(c,"submission");Bitmap sheet=Bitmap.createBitmap(2304,1024,Bitmap.Config.ARGB_8888);byte[] png;
  try{Canvas canvas=new Canvas(sheet);Paint paint=new Paint(3);for(int row=0;row<4;row++)for(int col=0;col<9;col++){Bitmap im=pack.custom?pack.states[row][col]:(row==0?pack.idle[col]:pack.images[row-1][col]);canvas.drawBitmap(im,null,new Rect(col*256,row*256,(col+1)*256,(row+1)*256),paint);}ByteArrayOutputStream out=new ByteArrayOutputStream();sheet.compress(Bitmap.CompressFormat.PNG,100,out);png=out.toByteArray();}finally{sheet.recycle();}
  JSONObject body=new JSONObject().put("name",pack.name).put("author",author).put("license",license).put("rights",true).put("png",Base64.encodeToString(png,Base64.NO_WRAP));JSONObject answer=new JSONObject(new String(request(destination+"/submissions",body.toString().getBytes("UTF-8"),65536,p),"UTF-8"));String id=answer.optString("id");if(!"pending".equals(answer.optString("status"))||!id.matches("[0-9a-f]{32}"))throw new IOException("Le serveur n’a pas confirmé la mise en modération.");return id;
 }
}
