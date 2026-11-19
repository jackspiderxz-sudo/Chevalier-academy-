/**
 * Lightweight zero-dependency Node HTTP server for previewing the
 * Chevalier Academy Responsive Website locally.
 * Serves web/dist/ if built, or serves web/ directly while mapping /assets/*
 * straight to Android app/src/main/res/drawable/ and app/src/main/res/font/.
 */

import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, '..');
const webRoot = __dirname;
const distDir = path.join(webRoot, 'dist');

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.webmanifest': 'application/manifest+json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ttf': 'font/ttf',
  '.ico': 'image/x-icon'
};

const LIVE_ASSET_MAP = {
  '/assets/chevalier_emblem.png': 'app/src/main/res/drawable/chevalier_emblem.png',
  '/assets/chevalier_launcher_logo.png': 'app/src/main/res/drawable/chevalier_launcher_logo.png',
  '/assets/chevalier_splash_artwork.png': 'app/src/main/res/drawable/chevalier_splash_artwork.png',
  '/assets/playfair_display.ttf': 'app/src/main/res/font/playfair_display.ttf',
  '/assets/plus_jakarta_sans.ttf': 'app/src/main/res/font/plus_jakarta_sans.ttf'
};

export function createWebServer({ useDist = fs.existsSync(path.join(distDir, 'index.html')) } = {}) {
  const baseDir = useDist ? distDir : webRoot;

  return http.createServer((req, res) => {
    const reqUrl = new URL(req.url || '/', 'http://localhost');
    let pathname = decodeURIComponent(reqUrl.pathname);
    if (pathname === '/') pathname = '/index.html';

    let filePath = null;
    if (!useDist && LIVE_ASSET_MAP[pathname]) {
      filePath = path.join(projectRoot, LIVE_ASSET_MAP[pathname]);
    } else {
      const candidate = path.resolve(baseDir, `.${pathname}`);
      if (candidate.startsWith(baseDir) && fs.existsSync(candidate) && fs.statSync(candidate).isFile()) {
        filePath = candidate;
      } else if (!path.extname(pathname)) {
        filePath = path.join(baseDir, 'index.html');
      }
    }

    if (!filePath || !fs.existsSync(filePath)) {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('Not Found');
      return;
    }

    const ext = path.extname(filePath).toLowerCase();
    res.writeHead(200, {
      'Content-Type': MIME_TYPES[ext] || 'application/octet-stream',
      'Cache-Control': 'no-cache'
    });
    fs.createReadStream(filePath).pipe(res);
  });
}

if (process.argv[1] && path.resolve(process.argv[1]) === __filename) {
  const port = Number(process.env.PORT || 8080);
  const server = createWebServer();
  server.listen(port, '0.0.0.0', () => {
    console.log(`Chevalier Academy Responsive Website running at http://localhost:${port}`);
  });
}
