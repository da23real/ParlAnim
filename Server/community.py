"""Parl'Anim moderation service. Run behind HTTPS; no GitHub credentials needed.
Pending content is never exposed by HTTP. Moderation is a local CLI operation.
"""
import argparse, base64, hashlib, io, json, re, sqlite3, time, uuid
from contextlib import contextmanager
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from PIL import Image

MAX_BODY=24*1024*1024

class Store:
    def __init__(self, root):
        self.root=Path(root); self.root.mkdir(parents=True,exist_ok=True)
        with self.db() as db:
            db.execute('CREATE TABLE IF NOT EXISTS packs(id TEXT PRIMARY KEY,name TEXT,author TEXT,license TEXT,status TEXT,png BLOB,sha256 TEXT,created REAL,client TEXT)')
    @contextmanager
    def db(self):
        connection=sqlite3.connect(self.root/'moderation.sqlite')
        try:
            with connection: yield connection
        finally: connection.close()
    def submit(self, body, client):
        if body.get('rights') is not True: raise ValueError('Confirme tes droits de partage.')
        fields=[]
        for name in ('name','author','license'):
            value=body.get(name)
            if not isinstance(value,str) or not 1<=len(value.strip())<=120 or any(ord(c)<32 for c in value): raise ValueError('Nom, auteur et licence requis (120 caractères maximum).')
            fields.append(value.strip())
        try:
            raw=base64.b64decode(body['png'],validate=True)
            if len(raw)>16*1024*1024: raise ValueError()
            im=Image.open(io.BytesIO(raw))
            w,h=im.size
            if im.format!='PNG' or w%9 or h%4 or w//9!=h//4 or not 16<=w//9<=512: raise ValueError()
            im.load(); im=im.convert('RGBA')
            if im.getchannel('A').getextrema()[0]==255: raise ValueError()
            clean=io.BytesIO();im.save(clean,format='PNG');raw=clean.getvalue()
        except Exception as exc:
            raise ValueError('Planche PNG transparente 9 × 4 invalide, cases de 16 à 512 pixels.') from exc
        ident=uuid.uuid4().hex; now=time.time()
        with self.db() as db:
            if db.execute("SELECT count(*) FROM packs WHERE client=? AND created>?",(client,now-3600)).fetchone()[0]>=5: raise ValueError('Maximum cinq propositions par heure.')
            if db.execute("SELECT count(*) FROM packs WHERE status='pending'").fetchone()[0]>=200: raise ValueError('Modération occupée ; réessaie plus tard.')
            db.execute('INSERT INTO packs VALUES(?,?,?,?,?,?,?,?,?)',(ident,*fields,'pending',raw,hashlib.sha256(raw).hexdigest(),now,client))
        return {'id':ident,'status':'pending','message':'Soumis à modération'}
    def catalog(self):
        with self.db() as db:
            rows=db.execute("SELECT id,name,author,license,sha256 FROM packs WHERE status='approved' ORDER BY created DESC LIMIT 500").fetchall()
        return {'version':1,'packs':[dict(zip(('id','name','author','license','sha256'),row)) for row in rows]}
    def download(self,ident):
        with self.db() as db: row=db.execute("SELECT png FROM packs WHERE id=? AND status='approved'",(ident,)).fetchone()
        return row[0] if row else None
    def moderate(self,ident,status):
        if status not in ('approved','rejected'):raise ValueError('Statut invalide')
        with self.db() as db:
            cursor=db.execute('UPDATE packs SET status=? WHERE id=?',(status,ident))
            if cursor.rowcount!=1:raise ValueError('Proposition introuvable')
    def export(self,target):
        target=Path(target);(target/'packs').mkdir(parents=True,exist_ok=True)
        catalog=self.catalog();allowed={p['id']+'.png' for p in catalog['packs']}
        # This destination is explicitly chosen by the moderation command.
        for p in (target/'packs').glob('*.png'):
            if re.fullmatch(r'[0-9a-f]{32}\.png',p.name) and p.name not in allowed:p.unlink()
        for p in catalog['packs']:(target/'packs'/(p['id']+'.png')).write_bytes(self.download(p['id']))
        (target/'catalog.json').write_text(json.dumps(catalog,ensure_ascii=False),encoding='utf-8')

def handler(store):
    class Handler(BaseHTTPRequestHandler):
        def setup(self):
            super().setup();self.connection.settimeout(20)
        def reply(self,code,value,mime='application/json'):
            raw=json.dumps(value,ensure_ascii=False).encode() if mime=='application/json' else value
            self.send_response(code);self.send_header('Content-Type',mime);self.send_header('Content-Length',str(len(raw)));self.send_header('Cache-Control','no-store');self.send_header('X-Content-Type-Options','nosniff');self.end_headers();self.wfile.write(raw)
        def do_GET(self):
            if self.path=='/catalog.json':return self.reply(200,store.catalog())
            match=re.fullmatch(r'/packs/([0-9a-f]{32})\.png',self.path)
            raw=store.download(match[1]) if match else None
            self.reply(200,raw,'image/png') if raw else self.reply(404,{'error':'Introuvable'})
        def do_POST(self):
            if self.path!='/submissions':return self.reply(404,{'error':'Introuvable'})
            try:
                size=int(self.headers.get('Content-Length','0'))
                if not 0<size<=MAX_BODY:raise ValueError('Envoi trop volumineux ou vide.')
                body=json.loads(self.rfile.read(size))
                if not isinstance(body,dict):raise ValueError('Objet JSON requis.')
                result=store.submit(body,self.client_address[0]);self.reply(201,result)
            except (ValueError,KeyError,TypeError) as exc:self.reply(400,{'error':str(exc)})
    return Handler

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--data',default=str(Path(__file__).parent/'Runtime'))
    sub=parser.add_subparsers(dest='command',required=True)
    serve=sub.add_parser('serve');serve.add_argument('--port',type=int,default=8766)
    sub.add_parser('pending')
    review=sub.add_parser('review');review.add_argument('id')
    for action in ('approve','reject'):
        p=sub.add_parser(action);p.add_argument('id')
    export=sub.add_parser('export');export.add_argument('directory')
    args=parser.parse_args();store=Store(args.data)
    if args.command=='serve':HTTPServer(('127.0.0.1',args.port),handler(store)).serve_forever()
    elif args.command=='pending':
        with store.db() as db:
            for row in db.execute("SELECT id,name,author,license FROM packs WHERE status='pending'"):print(row)
    elif args.command=='review':
        if not re.fullmatch(r'[0-9a-f]{32}',args.id):raise ValueError('Identifiant invalide')
        with store.db() as db:row=db.execute('SELECT name,author,license,status,png FROM packs WHERE id=?',(args.id,)).fetchone()
        if not row:raise ValueError('Proposition introuvable')
        folder=store.root/'Review';folder.mkdir(exist_ok=True)
        (folder/(args.id+'.png')).write_bytes(row[4])
        (folder/(args.id+'.json')).write_text(json.dumps(dict(zip(('name','author','license','status'),row[:4])),ensure_ascii=False),encoding='utf-8')
        print(folder/(args.id+'.png'))
    elif args.command=='export':store.export(args.directory)
    else:store.moderate(args.id,'approved' if args.command=='approve' else 'rejected')
if __name__=='__main__':main()
