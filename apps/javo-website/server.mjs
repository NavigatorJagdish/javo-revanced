import http from 'node:http';
import { readFile, stat } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root = path.join(path.dirname(fileURLToPath(import.meta.url)), 'dist');
const types = {'.html':'text/html; charset=utf-8','.css':'text/css; charset=utf-8','.js':'text/javascript; charset=utf-8','.svg':'image/svg+xml','.json':'application/json','.png':'image/png','.apk':'application/vnd.android.package-archive','.zip':'application/zip'};
const server = http.createServer(async(req,res)=>{
  try {
    if(!['GET','HEAD'].includes(req.method)){res.writeHead(405);return res.end();}
    const url = new URL(req.url,'http://localhost');
    const requested = decodeURIComponent(url.pathname);
    let target = path.resolve(root, '.'+requested);
    if(!target.startsWith(root+path.sep) && target!==root){res.writeHead(403);return res.end();}
    let status = 200;
    try{if((await stat(target)).isDirectory())target=path.join(target,'index.html');await stat(target);}catch{target=path.join(root,'404.html');status=404;}
    const data = await readFile(target);
    res.writeHead(status,{'Content-Type':types[path.extname(target)]||'application/octet-stream','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer','Content-Security-Policy':"default-src 'self'; img-src 'self'; style-src 'self'; script-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'"});
    res.end(req.method==='HEAD'?undefined:data);
  }catch{res.writeHead(400);res.end('Invalid request');}
});
server.listen(Number(process.env.PORT||4173),'127.0.0.1',()=>console.log('Javo local preview: http://127.0.0.1:'+server.address().port));
