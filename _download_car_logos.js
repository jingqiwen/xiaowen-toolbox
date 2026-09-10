const fs = require('fs');
const https = require('https');
const path = require('path');

const brands = JSON.parse(fs.readFileSync(process.env.BRANDS_JSON, 'utf8'));
const outDir = process.env.OUT_DIR;
const creditsFile = process.env.CREDITS_FILE;
fs.mkdirSync(outDir, { recursive: true });

const overrides = {
  'VW': 'Volkswagen', 'T': 'Toyota', 'H': 'Honda', 'N': 'Nissan', 'M': 'Mazda',
  'J': 'Jaguar', 'P': 'Porsche', 'L': 'Lexus', 'A': 'Acura', 'R': 'Renault',
  'C': 'Citroen', 'F': 'Ford', 'B': 'Bentley', '+': 'Chevrolet', 'Chevy': 'Chevrolet',
  'RR': 'Rolls-Royce', 'SF': 'Ferrari', 'LB': 'Lamborghini', 'AM': 'Aston Martin',
  'LR': 'Land Rover', 'INF': 'Infiniti', 'Sub': 'Subaru', 'Mits': 'Mitsubishi',
  'PEU': 'Peugeot', 'CIT': 'Citroen', 'Alfa': 'Alfa Romeo', 'Skoda': 'Skoda',
  'CHAN': 'Changan', 'HQ': 'Hongqi', 'NIO': 'NIO', 'XP': 'XPeng', 'Li': 'Li Auto',
  'Lynk': 'Lynk & Co', 'GWM': 'Great Wall Motors', 'BYD': 'BYD', 'Geely': 'Geely',
  'Chery': 'Chery', 'Wuling': 'Wuling', 'MG': 'MG', 'ORA': 'ORA', 'Tank': 'Tank',
  'WEY': 'WEY', 'Haval': 'Haval', 'Denza': 'Denza', 'Yangwang': 'Yangwang',
  'Zeekr': 'Zeekr', 'Voyah': 'Voyah', 'Avatr': 'Avatr', 'Deepal': 'Deepal',
  'Aion': 'Aion', 'Neta': 'Neta', 'Leapmotor': 'Leapmotor', 'AITO': 'AITO',
  'IM': 'IM Motors', 'Arcfox': 'Arcfox', 'HiPhi': 'HiPhi', 'WM': 'Weltmeister',
  'Exeed': 'Exeed', 'Jetour': 'Jetour', 'Baojun': 'Baojun', 'Roewe': 'Roewe',
  'Maxus': 'Maxus', 'Dongfeng': 'Dongfeng', 'FAW': 'FAW', 'Bestune': 'Bestune',
  'Trumpchi': 'GAC Trumpchi', 'JAC': 'JAC Motors', 'JMC': 'JMC', 'Zotye': 'Zotye',
  'Landwind': 'Landwind', 'Haima': 'Haima', 'Soueast': 'Soueast',
  'Brilliance': 'Brilliance Auto', 'Qoros': 'Qoros', 'Luxgen': 'Luxgen',
  'Lifan': 'Lifan', 'Leopaard': 'Leopaard', 'Hanteng': 'Hanteng', 'SWM': 'SWM',
  'Cowin': 'Cowin', 'Zhidou': 'Zhidou', 'Yudo': 'Yudo', 'Qiantu': 'Qiantu',
  'Youxia': 'Youxia', 'Byton': 'Byton', 'Seres': 'Seres', 'Skyworth': 'Skyworth',
  'Xiaomi': 'Xiaomi Auto', 'Jiyue': 'Jiyue', 'Polestones': 'Polestones',
  'Fangchengbao': 'Fangchengbao', 'MAN': 'MAN Truck', 'Neoplan': 'Neoplan',
  'Alpina': 'Alpina', 'Wiesmann': 'Wiesmann', 'Tom\'s': 'Toyota Racing',
  'Hino': 'Hino Motors', 'Isuzu': 'Isuzu', 'Daihatsu': 'Daihatsu',
  'Mitsuoka': 'Mitsuoka', 'Pontiac': 'Pontiac', 'Oldsmobile': 'Oldsmobile',
  'Saturn': 'Saturn Corporation', 'Rivian': 'Rivian', 'Lucid': 'Lucid Motors',
  'Fisker': 'Fisker', 'Saleen': 'Saleen', 'Hennessey': 'Hennessey',
  'Willys': 'Willys', 'Maybach': 'Maybach', 'Borgward': 'Borgward',
  'Opel': 'Opel', 'Cupra': 'Cupra', 'Seat': 'SEAT'
};

function requestJson(url) {
  return new Promise((resolve, reject) => {
    const req = https.get(url, { headers: { 'User-Agent': 'xiaowen-toolbox/1.0 (personal, non-commercial)' } }, res => {
      let data = '';
      res.on('data', c => data += c);
      res.on('end', () => {
        try { resolve(JSON.parse(data)); } catch (e) { reject(e); }
      });
    });
    req.on('error', reject);
    req.setTimeout(20000, () => req.destroy(new Error('timeout')));
  });
}

function download(url, dest) {
  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(dest);
    const req = https.get(url, { headers: { 'User-Agent': 'xiaowen-toolbox/1.0 (personal, non-commercial)' } }, res => {
      if (res.statusCode !== 200) { reject(new Error('http ' + res.statusCode)); return; }
      res.pipe(file);
      file.on('finish', () => { file.close(() => resolve(true)); });
    });
    req.on('error', err => { try { fs.unlinkSync(dest); } catch (_) {} reject(err); });
    req.setTimeout(30000, () => req.destroy(new Error('download timeout')));
  });
}

function clean(s) { return String(s || '').replace(/<[^>]+>/g, '').trim(); }
function safe(s) { return String(s).replace(/[^A-Za-z0-9_-]/g, '_').substring(0, 40); }

async function processBrand(brand, index, credits, lock) {
  const base = overrides[brand.badge] || (brand.badge.length <= 2 ? brand.name : brand.badge);
  const queries = [base + ' logo', base + ' car logo', base + ' emblem', base + ' marque'];
  for (const query of queries) {
    const url = 'https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch=' +
      encodeURIComponent(query) + '&gsrnamespace=6&gsrlimit=20&prop=imageinfo&iiprop=url%7Cextmetadata%7Cmime&iiurlwidth=256&format=json&origin=*';
    let data;
    try { data = await requestJson(url); } catch (_) { continue; }
    const pages = data && data.query && data.query.pages ? Object.values(data.query.pages) : [];
    for (const page of pages) {
      const info = page.imageinfo && page.imageinfo[0];
      if (!info) continue;
      const mime = info.mime || '';
      if (!/^image\/(svg\+xml|png|jpeg|webp)$/.test(mime)) continue;
      const meta = info.extmetadata || {};
      const license = clean(meta.LicenseShortName && meta.LicenseShortName.value);
      if (/fair use|non-free|nonfree|copyright only|all rights reserved/i.test(license)) continue;
      if (!/public domain|pd-|cc0|cc[ -]?by|attribution|creative commons|no restrictions/i.test(license)) continue;
      const title = page.title || '';
      if (!/logo|emblem|marque/i.test(title)) continue;
      const fileUrl = info.thumburl || info.url;
      if (!fileUrl) continue;
      const fileName = String(index).padStart(3, '0') + '_' + safe(brand.badge) + '.png';
      const dest = path.join(outDir, fileName);
      if (fs.existsSync(dest)) {
        lock.push({ brand: brand.name, file: fileName, title, license, artist: clean(meta.Artist && meta.Artist.value), source: info.descriptionurl || '' });
        return true;
      }
      try {
        await download(fileUrl, dest);
        lock.push({
          brand: brand.name, file: fileName, title: title,
          license: license,
          artist: clean(meta.Artist && meta.Artist.value),
          source: info.descriptionurl || ''
        });
        return true;
      } catch (_) { /* try next */ }
    }
  }
  return false;
}

(async () => {
  let credits = [];
  if (fs.existsSync(creditsFile)) {
    try { credits = JSON.parse(fs.readFileSync(creditsFile, 'utf8')); } catch (_) { credits = []; }
  }
  // 只保留文件真实存在的记录，缺失的重新下载；同一品牌去重
  credits = credits.filter(c => c && c.file && fs.existsSync(path.join(outDir, c.file)));
  const seenBrand = new Set();
  credits = credits.filter(c => { if (seenBrand.has(c.brand)) return false; seenBrand.add(c.brand); return true; });
  const done = new Set(credits.map(c => c.brand));
  let ok = credits.length;
  const queue = brands.map((b, i) => ({ b, i })).filter(x => !done.has(x.b.brand));
  const concurrency = 4;
  let cursor = 0;
  async function worker() {
    while (cursor < queue.length) {
      const item = queue[cursor++];
      const lock = [];
      const success = await processBrand(item.b, item.i, credits, lock);
      if (success && lock.length) {
        credits.push(lock[0]);
        ok++;
        console.log('OK ' + item.b.name + ' -> ' + lock[0].file + ' [' + lock[0].license + ']');
      } else {
        console.log('SKIP ' + item.b.name);
      }
      fs.writeFileSync(creditsFile, JSON.stringify(credits, null, 2));
      await new Promise(r => setTimeout(r, 150));
    }
  }
  await Promise.all(Array.from({ length: concurrency }, worker));
  console.log('DONE ' + ok + ' / ' + brands.length);
})();
