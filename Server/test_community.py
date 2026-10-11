import base64,io,json,tempfile,threading,unittest,urllib.request,urllib.error
from pathlib import Path
from http.server import HTTPServer
from PIL import Image
from community import Store,handler

class CommunityTests(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory();self.store=Store(self.temp.name)
  b=io.BytesIO();Image.new('RGBA',(288,128),(255,0,0,100)).save(b,format='PNG')
  self.body=dict(name='Test',author='Roy',license='Test fixture only',rights=True,png=base64.b64encode(b.getvalue()).decode())
 def tearDown(self):self.temp.cleanup()
 def test_moderation(self):
  response=self.store.submit(self.body,'fixture');ident=response['id'];self.assertEqual(response['message'],'Soumis à modération');self.assertEqual(self.store.catalog()['packs'],[]);self.assertIsNone(self.store.download(ident))
  self.store.moderate(ident,'approved');self.assertEqual(len(self.store.catalog()['packs']),1);self.assertTrue(self.store.download(ident).startswith(b'\x89PNG'))
  target=Path(self.temp.name)/"Parl'Anim";self.store.export(target);self.assertTrue((target/'packs'/f'{ident}.png').exists())
  self.store.moderate(ident,'rejected');self.store.export(target);self.assertFalse((target/'packs'/f'{ident}.png').exists());self.assertEqual(json.loads((target/'catalog.json').read_text())['packs'],[])
 def test_invalid(self):
  for field,value in [('rights',False),('name',''),('name','x'*121),('png','invalid'),('author','x\nBad')]:
   with self.subTest(field=field),self.assertRaises(ValueError):self.store.submit({**self.body,field:value},'fixture')
 def test_quota(self):
  for i in range(5):self.store.submit(self.body,'fixture')
  with self.assertRaises(ValueError):self.store.submit(self.body,'fixture')
 def test_http(self):
  server=HTTPServer(('127.0.0.1',0),handler(self.store));thread=threading.Thread(target=server.serve_forever);thread.start();url=f'http://127.0.0.1:{server.server_port}'
  try:
   request=urllib.request.Request(url+'/submissions',data=json.dumps(self.body).encode(),headers={'Content-Type':'application/json'})
   with urllib.request.urlopen(request) as response:self.assertEqual(response.status,201);ident=json.load(response)['id']
   with self.assertRaises(urllib.error.HTTPError):urllib.request.urlopen(url+'/packs/'+ident+'.png')
   self.store.moderate(ident,'approved')
   with urllib.request.urlopen(url+'/catalog.json') as response:self.assertEqual(len(json.load(response)['packs']),1)
   with urllib.request.urlopen(url+'/packs/'+ident+'.png') as response:self.assertEqual(response.headers['Content-Type'],'image/png')
   with self.assertRaises(urllib.error.HTTPError):urllib.request.urlopen(url+'/../moderation.sqlite')
  finally:server.shutdown();server.server_close();thread.join()
if __name__=='__main__':unittest.main(verbosity=2)
