/**
 * Chevalier Academy — Production Responsive Website Bundler
 *
 * Automates bundling of shared Android brand assets, typography fonts, HTML,
 * Material 3 Light/Dark CSS, modular ES scripts, PWA manifest, SPA 404 fallback,
 * and SHA-256 integrity build manifest into web/dist/ for static deployment.
 */

import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, '..');
const webRoot = __dirname;
const distDir = path.join(webRoot, 'dist');
const distAssetsDir = path.join(distDir, 'assets');
const distJsDir = path.join(distDir, 'js');

export const SHARED_ANDROID_ASSETS = [
  { src: 'app/src/main/res/drawable/chevalier_emblem.png', dest: 'chevalier_emblem.png' },
  { src: 'app/src/main/res/drawable/chevalier_launcher_logo.png', dest: 'chevalier_launcher_logo.png' },
  { src: 'app/src/main/res/drawable/chevalier_splash_artwork.png', dest: 'chevalier_splash_artwork.png' },
  { src: 'app/src/main/res/font/playfair_display.ttf', dest: 'playfair_display.ttf' },
  { src: 'app/src/main/res/font/plus_jakarta_sans.ttf', dest: 'plus_jakarta_sans.ttf' }
];

export const WEB_JS_FILES = [
  'icons.js',
  'i18n.js',
  'store.js',
  'publicViews.js',
  'adminViews.js',
  'app.js'
];

function sha256Short(filePath) {
  const bytes = fs.readFileSync(filePath);
  return {
    hash: crypto.createHash('sha256').update(bytes).digest('hex').slice(0, 10),
    bytes: bytes.length
  };
}

function injectProductionHtmlHead(rawHtml, cssHash, appHash) {
  const preloadTags = [
    '  <link rel="preload" href="./assets/playfair_display.ttf" as="font" type="font/ttf" crossorigin />',
    '  <link rel="preload" href="./assets/plus_jakarta_sans.ttf" as="font" type="font/ttf" crossorigin />',
    '  <link rel="preload" href="./assets/chevalier_emblem.png" as="image" />'
  ].join('\n');

  return rawHtml
    .replace(
      '<link rel="stylesheet" href="./styles.css" />',
      `${preloadTags}\n  <link rel="stylesheet" href="./styles.css?v=${cssHash}" />`
    )
    .replace(
      '<script type="module" src="./js/app.js"></script>',
      `<script type="module" src="./js/app.js?v=${appHash}"></script>`
    );
}

export function buildWeb() {
  fs.rmSync(distDir, { recursive: true, force: true });
  fs.mkdirSync(distAssetsDir, { recursive: true });
  fs.mkdirSync(distJsDir, { recursive: true });

  const filesReport = [];

  // 1. Copy shared Android brand drawables & fonts directly from Android res/
  for (const asset of SHARED_ANDROID_ASSETS) {
    const from = path.join(projectRoot, asset.src);
    const to = path.join(distAssetsDir, asset.dest);
    if (!fs.existsSync(from)) {
      throw new Error(`Missing shared Android asset: ${asset.src}`);
    }
    fs.copyFileSync(from, to);
    const info = sha256Short(to);
    filesReport.push({ path: `assets/${asset.dest}`, source: asset.src, ...info });
  }

  // 2. Copy responsive M3 Light/Dark stylesheet
  const stylesSrc = path.join(webRoot, 'styles.css');
  const stylesDest = path.join(distDir, 'styles.css');
  fs.copyFileSync(stylesSrc, stylesDest);
  const cssInfo = sha256Short(stylesDest);
  filesReport.push({ path: 'styles.css', ...cssInfo });

  // 3. Copy modular ES scripts
  let appHash = '';
  for (const file of WEB_JS_FILES) {
    const from = path.join(webRoot, 'js', file);
    const to = path.join(distJsDir, file);
    if (!fs.existsSync(from)) {
      throw new Error(`Missing Web JS module: web/js/${file}`);
    }
    fs.copyFileSync(from, to);
    const info = sha256Short(to);
    if (file === 'app.js') appHash = info.hash;
    filesReport.push({ path: `js/${file}`, ...info });
  }

  // 4. Emit production index.html (with font/emblem preloads + cache-busting hashes) & 404.html SPA fallback
  const rawHtml = fs.readFileSync(path.join(webRoot, 'index.html'), 'utf8');
  const prodHtml = injectProductionHtmlHead(rawHtml, cssInfo.hash, appHash);
  const indexDest = path.join(distDir, 'index.html');
  const fallbackDest = path.join(distDir, '404.html');
  fs.writeFileSync(indexDest, prodHtml, 'utf8');
  fs.writeFileSync(fallbackDest, prodHtml, 'utf8');
  filesReport.push({ path: 'index.html', ...sha256Short(indexDest) });
  filesReport.push({ path: '404.html', ...sha256Short(fallbackDest) });

  // 5. Emit installable PWA web manifest matching Chevalier Academy Android theme
  const webManifest = {
    name: 'Chevalier Academy Matric Higher Secondary School',
    short_name: 'Chevalier Academy',
    description: 'Learn to Live & Love — Siluvathur Road, Dindigul - 624 306',
    start_url: './index.html#/home',
    display: 'standalone',
    background_color: '#FFF9E8',
    theme_color: '#8D2029',
    icons: [
      {
        src: './assets/chevalier_emblem.png',
        sizes: '512x512',
        type: 'image/png',
        purpose: 'any'
      },
      {
        src: './assets/chevalier_launcher_logo.png',
        sizes: '512x512',
        type: 'image/png',
        purpose: 'maskable'
      }
    ]
  };
  const manifestDest = path.join(distDir, 'manifest.webmanifest');
  fs.writeFileSync(manifestDest, JSON.stringify(webManifest, null, 2), 'utf8');
  filesReport.push({ path: 'manifest.webmanifest', ...sha256Short(manifestDest) });

  // 6. Emit build integrity manifest for deployment verification
  const totalBytes = filesReport.reduce((acc, f) => acc + f.bytes, 0);
  const buildManifest = {
    app: 'Chevalier Academy Responsive Website',
    branding: 'CHEVALIER ACADEMY MATRIC HIGHER SECONDARY SCHOOL',
    builtAt: new Date().toISOString(),
    totalBytes,
    copiedAssets: SHARED_ANDROID_ASSETS.length,
    copiedScripts: WEB_JS_FILES.length,
    files: filesReport
  };
  fs.writeFileSync(
    path.join(distDir, 'build-manifest.json'),
    JSON.stringify(buildManifest, null, 2),
    'utf8'
  );

  return {
    distDir,
    copiedAssets: SHARED_ANDROID_ASSETS.length,
    copiedScripts: WEB_JS_FILES.length,
    totalBytes,
    files: filesReport
  };
}

if (process.argv[1] && path.resolve(process.argv[1]) === __filename) {
  const result = buildWeb();
  const kb = (result.totalBytes / 1024).toFixed(1);
  console.log(`Chevalier Academy Web build succeeded -> ${result.distDir}`);
  console.log(
    `Reused ${result.copiedAssets} Android assets, bundled ${result.copiedScripts} ES modules (${kb} KB total).`
  );
}
