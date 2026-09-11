/**
 * 生成 app/src/main/assets/vocab_bulk.jsonl（考研 / 四级 / 六级 词表）。
 *
 * 数据源：ECDICT（skywind3000/ECDICT）ecdict.csv，tag 字段包含：
 *   zk 中考 / gk 高考 / cet4 四级 / cet6 六级 / ky 考研 / toefl / ielts / gre
 *
 * 两种规模：
 *   1. 官方大纲规模（App 默认）：
 *        四级 4500 = cet4 核心 + 按重要度补充中学基础词
 *        六级 5500 = cet6 核心 + 按重要度补充四级词
 *        考研 5500 = ky  核心 + 按重要度补充四级/六级/基础词
 *   2. 含基础词规模（App 可选“含基础词”开关）：
 *        四级 = cet4 ∪ gk ∪ zk
 *        六级 = cet6 ∪ cet4 ∪ gk ∪ zk
 *        考研 = ky   ∪ cet6 ∪ cet4 ∪ gk ∪ zk
 *
 * 补充词的“重要度”排序：牛津核心词优先 → 柯林斯星级高优先 → 词频高优先。
 *
 * 输出字段（尽量精简）：
 *   w 单词 / p 音标 / t 中文释义 / s 词性 / c 柯林斯星级 / o 牛津标记 / g 词库标签 / e 变形
 *
 * 用法：
 *   node _build_vocab.js
 *   可先用环境变量指定 CSV_PATH / OUT_PATH；未指定时优先使用缓存，必要时自动下载。
 */
const fs = require('fs');
const readline = require('readline');
const https = require('https');

const OUT_PATH = process.env.OUT_PATH || 'app/src/main/assets/vocab_bulk.jsonl';
const CSV_PATH = process.env.CSV_PATH || (process.env.TEMP || '/tmp') + '/ecdict.csv';

/** 官方大纲规模 */
const TARGET = { cet4: 4500, cet6: 5500, ky: 5500 };

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
  return String(s || '')
    // ECDICT 用字面的 \n 表示换行，这里统一还原成真实换行
    .replace(/\\n/g, '\n')
    .replace(/\r\n?/g, '\n')
    .replace(/\t/g, ' ')
    .trim();
}

/** 词频排序键：越小越常用；无频率数据的排最后 */
function freqKey(entry) {
  const frq = parseInt(entry.frq, 10) || 0;
  const bnc = parseInt(entry.bnc, 10) || 0;
  if (frq > 0) return frq;
  if (bnc > 0) return bnc + 1000000;
  return 2000000000;
}

/** 重要度排序键：牛津核心词优先 → 词频高优先（柯林斯星级仅作展示） */
function importance(entry) {
  const oxford = entry.oxford === 1 ? 0 : 1;
  return [oxford, freqKey(entry)];
}

/** 从候选词中按重要度取 n 个（排除已收录的） */
function topUp(pool, already, n) {
  const picked = [];
  const sorted = [...pool].sort((a, b) => {
    const pa = importance(a);
    const pb = importance(b);
    for (let i = 0; i < pa.length; i++) {
      if (pa[i] !== pb[i]) return pa[i] - pb[i];
    }
    return 0;
  });
  for (const entry of sorted) {
    if (picked.length >= n) break;
    if (already.has(entry.word)) continue;
    picked.push(entry);
  }
  return picked;
}

(async () => {
  if (!fs.existsSync(CSV_PATH) || fs.statSync(CSV_PATH).size < 1000) {
    console.log('downloading ecdict.csv ...');
    const urls = [
      'https://ghproxy.net/https://raw.githubusercontent.com/skywind3000/ECDICT/master/ecdict.csv',
      'https://cdn.jsdelivr.net/gh/skywind3000/ECDICT@master/ecdict.csv'
    ];
    let lastErr = null;
    for (const url of urls) {
      try {
        await download(url, CSV_PATH);
        if (fs.statSync(CSV_PATH).size > 1000000) break;
      } catch (e) {
        lastErr = e;
        console.log('mirror failed: ' + e.message);
      }
    }
    if (lastErr && fs.statSync(CSV_PATH).size < 1000000) throw lastErr;
  }
  console.log('csv: ' + CSV_PATH + ' (' + Math.round(fs.statSync(CSV_PATH).size / 1024 / 1024) + ' MB)');

  const rl = readline.createInterface({ input: fs.createReadStream(CSV_PATH, { encoding: 'utf8' }), crlfDelay: Infinity });

  const byWord = new Map();
  const core = { cet4: new Set(), cet6: new Set(), ky: new Set() };
  const basic = new Map(); // gk / zk 基础词
  let header = true;

  for await (const line of rl) {
    if (header) { header = false; continue; }
    if (!line) continue;
    const f = parseCsvLine(line);
    if (f.length < 11) continue;
    const entry = {
      word: clean(f[0]).toLowerCase(),
      phonetic: clean(f[1]),
      definition: clean(f[2]),
      translation: clean(f[3]),
      pos: clean(f[4]),
      collins: clean(f[5]),
      oxford: parseInt(f[6] || '0', 10) || 0,
      tags: clean(f[7]).split(/\s+/).filter(Boolean),
      bnc: clean(f[8]),
      frq: clean(f[9]),
      exchange: clean(f[10])
    };
    if (!entry.word || !entry.translation) continue;
    if (!byWord.has(entry.word)) byWord.set(entry.word, entry);
    if (entry.tags.includes('cet4')) core.cet4.add(entry.word);
    if (entry.tags.includes('cet6')) core.cet6.add(entry.word);
    if (entry.tags.includes('ky')) core.ky.add(entry.word);
    if (entry.tags.includes('gk') || entry.tags.includes('zk')) basic.set(entry.word, entry);
  }

  const entryOf = w => byWord.get(w) || {
    word: w, phonetic: '', definition: '', translation: '', pos: '', collins: '', oxford: 0,
    tags: [], bnc: '', frq: '', exchange: ''
  };

  // ---- 默认规模：按官方大纲词数补齐（重要度优先） ----
  const cet4Set = new Set(core.cet4);
  const cet4Extra = topUp([...basic.values()], cet4Set, TARGET.cet4 - cet4Set.size);
  cet4Extra.forEach(e => cet4Set.add(e.word));

  const cet6Set = new Set(core.cet6);
  const cet6Extra = topUp([...cet4Set].filter(w => !cet6Set.has(w)).map(entryOf), cet6Set, TARGET.cet6 - cet6Set.size);
  cet6Extra.forEach(e => cet6Set.add(e.word));

  const kySet = new Set(core.ky);
  const kyPool = new Map();
  [...cet6Set, ...cet4Set, ...basic.keys()].forEach(w => {
    if (!kySet.has(w) && !kyPool.has(w)) kyPool.set(w, entryOf(w));
  });
  const kyExtra = topUp([...kyPool.values()], kySet, TARGET.ky - kySet.size);
  kyExtra.forEach(e => kySet.add(e.word));

  // ---- “含基础词”规模 = 逐级并入下级/基础词表 ----
  const ext4 = new Set([...cet4Set, ...basic.keys()]);
  const ext6 = new Set([...cet6Set, ...ext4]);
  const extKy = new Set([...kySet, ...ext6]);

  // ---- 输出并集，标签同时保留大纲规模与基础词标记 ----
  const finalWords = new Set([...extKy]);
  const out = fs.createWriteStream(OUT_PATH);
  let count = 0;
  for (const w of [...finalWords].sort()) {
    const e = entryOf(w);
    const gk = e.tags.includes('gk') ? 'gk' : '';
    const zk = e.tags.includes('zk') ? 'zk' : '';
    const others = e.tags.filter(t => !['cet4', 'cet6', 'ky', 'gk', 'zk'].includes(t));
    const tags = [
      cet4Set.has(w) ? 'cet4' : '',
      cet6Set.has(w) ? 'cet6' : '',
      kySet.has(w) ? 'ky' : '',
      gk,
      zk,
      ...others
    ].filter(Boolean).join(' ');
    const rawFrq = parseInt(e.frq, 10) || 0;
    const rawBnc = parseInt(e.bnc, 10) || 0;
    out.write(JSON.stringify({
      w: e.word,
      p: e.phonetic,
      // t：中文释义（保留换行，App 端按词性排版）
      t: e.translation,
      // d：英文释义（截断，供“精讲”展示）
      d: e.definition ? e.definition.slice(0, 500) : '',
      s: e.pos,
      c: e.collins,
      o: e.oxford,
      g: tags,
      e: e.exchange,
      // f：词频排名（COCA，0 表示无数据）
      f: rawFrq > 0 ? rawFrq : rawBnc
    }) + '\n');
    count++;
  }
  out.end();
  console.log('DONE');
  console.log('  默认规模：四级=' + cet4Set.size + '（核心 ' + core.cet4.size + '+补充 ' + cet4Extra.length + '）' +
    ' 六级=' + cet6Set.size + '（核心 ' + core.cet6.size + '+补充 ' + cet6Extra.length + '）' +
    ' 考研=' + kySet.size + '（核心 ' + core.ky.size + '+补充 ' + kyExtra.length + '）');
  console.log('  含基础：四级=' + ext4.size + ' 六级=' + ext6.size + ' 考研=' + extKy.size);
  console.log('  资产总词数=' + count);
})();
