const fs = require('fs');
const readline = require('readline');
const https = require('https');

const csvPath = process.env.CSV_PATH;
const outPath = process.env.OUT_PATH;

function download(url, dest) {
  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(dest);
    const req = https.get(url, { headers: { 'User-Agent': 'xiaowen-toolbox/1.0' } }, res => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        download(res.headers.location, dest).then(resolve).catch(reject);
        return;
      }
      if (res.statusCode !== 200) { reject(new Error('http ' + res.statusCode)); return; }
      let last = 0;
      res.on('data', chunk => {
        last += chunk.length;
        if (last % (10 * 1024 * 1024) < chunk.length) {
          console.log('downloaded ' + Math.round(last / 1024 / 1024) + ' MB');
        }
      });
      res.pipe(file);
      file.on('finish', () => file.close(() => resolve(dest)));
    });
    req.on('error', reject);
  });
}

// 解析一行 CSV（ECDICT 字段均为双引号包裹，内部引号用 "" 转义）
function parseCsvLine(line) {
  const fields = [];
  let cur = '';
  let inQuotes = false;
  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (inQuotes) {
      if (ch === '"') {
        if (line[i + 1] === '"') { cur += '"'; i++; }
        else inQuotes = false;
      } else cur += ch;
    } else {
      if (ch === '"') inQuotes = true;
      else if (ch === ',') { fields.push(cur); cur = ''; }
      else cur += ch;
    }
  }
  fields.push(cur);
  return fields;
}

function clean(s) {
  return String(s || '').replace(/\r?\n/g, '；').replace(/\t/g, ' ').trim();
}

(async () => {
  if (!fs.existsSync(csvPath) || fs.statSync(csvPath).size < 1000) {
    console.log('downloading ecdict.csv ...');
    const urls = [
      'https://ghproxy.net/https://raw.githubusercontent.com/skywind3000/ECDICT/master/ecdict.csv',
      'https://cdn.jsdelivr.net/gh/skywind3000/ECDICT@master/ecdict.csv'
    ];
    let lastErr = null;
    for (const url of urls) {
      try {
        await download(url, csvPath);
        if (fs.statSync(csvPath).size > 1000000) break;
      } catch (e) {
        lastErr = e;
        console.log('mirror failed: ' + e.message);
      }
    }
    if (lastErr && fs.statSync(csvPath).size < 1000000) throw lastErr;
  }
  console.log('csv size ' + Math.round(fs.statSync(csvPath).size / 1024 / 1024) + ' MB');
  const out = fs.createWriteStream(outPath);
  const rl = readline.createInterface({ input: fs.createReadStream(csvPath, { encoding: 'utf8' }), crlfDelay: Infinity });
  let header = true;
  let count = 0;
  const counts = { cet4: 0, cet6: 0, ky: 0 };
  for await (const line of rl) {
    if (header) { header = false; continue; }
    if (!line) continue;
    const f = parseCsvLine(line);
    if (f.length < 11) continue;
    const word = clean(f[0]);
    const phonetic = clean(f[1]);
    const translation = clean(f[3]);
    const pos = clean(f[4]);
    const collins = clean(f[5]);
    const oxford = parseInt(f[6] || '0', 10) || 0;
    const tag = clean(f[7]);
    const exchange = clean(f[10]);
    if (!word || !translation) continue;
    const isCet4 = /cet4/.test(tag);
    const isCet6 = /cet6/.test(tag);
    const isKy = /\bky\b/.test(tag);
    if (!isCet4 && !isCet6 && !isKy) continue;
    if (isCet4) counts.cet4++;
    if (isCet6) counts.cet6++;
    if (isKy) counts.ky++;
    const obj = {
      w: word,
      p: phonetic,
      t: translation,
      s: pos,
      c: collins,
      o: oxford,
      g: tag,
      e: exchange
    };
    out.write(JSON.stringify(obj) + '\n');
    count++;
  }
  out.end();
  console.log('DONE total=' + count + ' cet4=' + counts.cet4 + ' cet6=' + counts.cet6 + ' ky=' + counts.ky);
})();
