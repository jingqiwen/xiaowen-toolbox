const fs = require('fs');
const https = require('https');
const path = require('path');

const brands = JSON.parse(fs.readFileSync(process.env.BRANDS_JSON, 'utf8'));
const drawableDir = process.env.DRAWABLE_DIR;
const mappingFile = process.env.MAPPING_FILE;
const dataFile = process.env.SIMPLE_DATA_FILE;
fs.mkdirSync(drawableDir, { recursive: true });

// 品牌 -> Simple Icons 英文名候选
const overrides = {
  'VW': 'Volkswagen', 'T': 'Toyota', 'H': 'Honda', 'N': 'Nissan', 'M': 'Mazda',
  'J': 'Jaguar', 'P': 'Porsche', 'L': 'Lexus', 'A': 'Acura', 'R': 'Renault',
  'C': 'Citroen', 'F': 'Ford', 'B': 'Bentley', '+': 'Chevrolet', 'Chevy': 'Chevrolet',
  'RR': 'Rolls-Royce', 'SF': 'Ferrari', 'LB': 'Lamborghini', 'AM': 'Aston Martin',
  'LR': 'Land Rover', 'INF': 'Infiniti', 'Sub': 'Subaru', 'Mits': 'Mitsubishi',
  'PEU': 'Peugeot', 'CIT': 'Citroen', 'Alfa': 'Alfa Romeo', 'Skoda': 'Skoda',
  'BYD': 'BYD', 'NIO': 'Nio', 'XP': 'XPeng', 'Li': 'Li Auto', 'Lynk': 'Lynk & Co',
  'GWM': 'Great Wall Motors', 'Geely': 'Geely', 'Chery': 'Chery', 'Wuling': 'Wuling',
  'MG': 'MG', 'Haval': 'Haval', 'Zeekr': 'Zeekr', 'Polestar': 'Polestar'
};

// 中文品牌名补充
const zhNames = {
  '大众': 'Volkswagen', '奥迪': 'Audi', '宝马': 'BMW', '奔驰': 'Mercedes', '迈巴赫': 'Maybach',
  '保时捷': 'Porsche', '欧宝': 'Opel', '斯柯达': 'Skoda', '西雅特': 'Seat', '布加迪': 'Bugatti',
  '丰田': 'Toyota', '本田': 'Honda', '日产': 'Nissan', '马自达': 'Mazda', '三菱': 'Mitsubishi',
  '斯巴鲁': 'Subaru', '铃木': 'Suzuki', '雷克萨斯': 'Lexus', '英菲尼迪': 'Infiniti', '讴歌': 'Acura',
  '大发': 'Daihatsu', '五十铃': 'Isuzu', '日野': 'Hino',
  '福特': 'Ford', '雪佛兰': 'Chevrolet', '别克': 'Buick', '凯迪拉克': 'Cadillac', 'GMC': 'GMC',
  '克莱斯勒': 'Chrysler', '道奇': 'Dodge', 'Jeep': 'Jeep', '林肯': 'Lincoln', '特斯拉': 'Tesla',
  '里维安': 'Rivian', '路西德': 'Lucid Motors',
  '比亚迪': 'BYD', '吉利': 'Geely', '长城': 'Great Wall Motors', '奇瑞': 'Chery', '长安': 'Changan',
  '红旗': 'Hongqi', '蔚来': 'Nio', '小鹏': 'XPeng', '理想': 'Li Auto', '领克': 'Lynk & Co',
  '极氪': 'Zeekr', '领克汽车': 'Lynk & Co', '岚图': 'Voyah', '阿维塔': 'Avatr', '深蓝': 'Deepal',
  '埃安': 'Aion', '哪吒': 'Neta', '零跑': 'Leapmotor', '问界': 'Aito', '高合': 'HiPhi',
  '星途': 'Exeed', '捷途': 'Jetour', '魏牌': 'WEY', '坦克': 'Tank', '哈弗': 'Haval', '欧拉': 'ORA',
  '五菱': 'Wuling', '宝骏': 'Baojun', '荣威': 'Roewe', '名爵': 'MG', '大通': 'Maxus',
  '东风': 'Dongfeng', '奔腾': 'Bestune', '广汽传祺': 'GAC', '江淮': 'JAC', '江铃': 'JMC',
  '众泰': 'Zotye', '海马': 'Haima', '观致': 'Qoros', '纳智捷': 'Luxgen', '力帆': 'Lifan',
  '斯威': 'SWM', '小米汽车': 'Xiaomi', '腾势': 'Denza', '仰望': 'Yangwang', '极越': 'Jiyue',
  '现代': 'Hyundai', '起亚': 'Kia', '捷尼赛思': 'Genesis', '双龙': 'SsangYong',
  '标致': 'Peugeot', '雪铁龙': 'Citroen', '雷诺': 'Renault', 'DS': 'DS Automobiles', '阿尔派': 'Alpine',
  '法拉利': 'Ferrari', '兰博基尼': 'Lamborghini', '玛莎拉蒂': 'Maserati', '阿尔法·罗密欧': 'Alfa Romeo',
  '菲亚特': 'Fiat', '帕加尼': 'Pagani', '蓝旗亚': 'Lancia', '阿巴斯': 'Abarth', '依维柯': 'Iveco',
  '杜卡迪': 'Ducati', '劳斯莱斯': 'Rolls-Royce', '宾利': 'Bentley', '迈凯伦': 'McLaren',
  '阿斯顿·马丁': 'Aston Martin', '路虎': 'Land Rover', '捷豹': 'Jaguar', 'MINI': 'Mini',
  '路特斯': 'Lotus', '摩根': 'Morgan', '罗孚': 'Rover', '沃克斯豪尔': 'Vauxhall',
  '沃尔沃': 'Volvo', '极星': 'Polestar', '萨博': 'Saab', '科尼赛克': 'Koenigsegg', '斯堪尼亚': 'Scania',
  '达契亚': 'Dacia', '拉达': 'Lada', '塔塔': 'Tata', '马恒达': 'Mahindra', '宝腾': 'Proton',
  'VinFast': 'VinFast', '哈雷戴维森': 'Harley-Davidson', 'KTM': 'KTM', '雅马哈': 'Yamaha',
  '川崎': 'Kawasaki', '阿普利亚': 'Aprilia', '皇家恩菲尔德': 'Royal Enfield'
};

function request(url) {
  return new Promise((resolve, reject) => {
    const req = https.get(url, { headers: { 'User-Agent': 'xiaowen-toolbox/1.0' } }, res => {
      let d = '';
      res.on('data', c => d += c);
      res.on('end', () => resolve(d));
    });
    req.on('error', reject);
    req.setTimeout(20000, () => req.destroy(new Error('timeout')));
  });
}

function norm(s) {
  return String(s || '').toLowerCase().replace(/[^a-z0-9]/g, '');
}

(async () => {
  const flat = JSON.parse(fs.readFileSync(dataFile, 'utf8'));
  const slugs = (flat.files || [])
    .map(f => f.name)
    .filter(name => /^\/icons\/[^/]+\.svg$/.test(name))
    .map(name => name.replace('/icons/', '').replace('.svg', ''));
  const byNorm = new Map();
  slugs.forEach(slug => byNorm.set(norm(slug), slug));

  const mapping = {};
  let ok = 0;
  for (const brand of brands) {
    const candidate = zhNames[brand.name] || overrides[brand.badge] || brand.badge;
    const n = norm(candidate);
    let slug = byNorm.get(n);
    if (!slug && n.length >= 4) {
      for (const [key, value] of byNorm) {
        if (key.length >= 4 && (key.startsWith(n) || n.startsWith(key))) { slug = value; break; }
      }
    }
    if (!slug) { console.log('NOICON ' + brand.name + ' (' + candidate + ')'); continue; }
    const icon = { slug };
    const resName = 'carlogo_' + icon.slug.replace(/[^a-z0-9_]/g, '_');
    const xmlPath = path.join(drawableDir, resName + '.xml');
    try {
      const svg = fs.existsSync(xmlPath) ? null : await request('https://cdn.jsdelivr.net/npm/simple-icons@16.30.0/icons/' + icon.slug + '.svg');
      if (!fs.existsSync(xmlPath)) {
        const paths = [...svg.matchAll(/d="([^"]+)"/g)].map(m => m[1]);
        if (!paths.length) { console.log('NOPATH ' + brand.name); continue; }
        const body = paths.map(d => '    <path android:fillColor="#FFFFFF" android:pathData="' + d.replace(/"/g, '&quot;') + '" />').join('\n');
        const xml = '<?xml version="1.0" encoding="utf-8"?>\n' +
          '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n' +
          '    android:width="24dp" android:height="24dp"\n' +
          '    android:viewportWidth="24" android:viewportHeight="24">\n' + body + '\n</vector>\n';
        fs.writeFileSync(xmlPath, xml);
      }
      mapping[brand.name] = { slug: icon.slug, res: resName, title: icon.slug };
      ok++;
      console.log('OK ' + brand.name + ' -> ' + icon.slug);
    } catch (e) {
      console.log('FAIL ' + brand.name + ' ' + e.message);
    }
    fs.writeFileSync(mappingFile, JSON.stringify(mapping, null, 2));
    await new Promise(r => setTimeout(r, 80));
  }
  console.log('DONE ' + ok + ' / ' + brands.length);
})();
