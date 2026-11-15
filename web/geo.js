/* Name matching for places. Villages in the official list are in English; people type English or Hindi.
   Both are reduced to a rough "sound skeleton" (consonants only), so "रामपुरा", "Rampura" and "Ramapura" meet. */
(function(){
  const CONS = {"क":"k","ख":"kh","ग":"g","घ":"gh","ङ":"n","च":"ch","छ":"chh","ज":"j","झ":"jh","ञ":"n","ट":"t","ठ":"th","ड":"d","ढ":"dh","ण":"n",
    "त":"t","थ":"th","द":"d","ध":"dh","न":"n","प":"p","फ":"ph","ब":"b","भ":"bh","म":"m","य":"y","र":"r","ल":"l","ळ":"l","व":"v","श":"sh","ष":"sh","स":"s","ह":"h",
    "क़":"k","ख़":"k","ग़":"g","ज़":"j","ड़":"r","ढ़":"r","फ़":"p"};
  const IND = {"अ":"a","आ":"a","इ":"i","ई":"i","उ":"u","ऊ":"u","ऋ":"ri","ए":"e","ऐ":"ai","ओ":"o","औ":"au","ऑ":"o"};
  const MAT = {"ा":"a","ि":"i","ी":"i","ु":"u","ू":"u","ृ":"ri","े":"e","ै":"ai","ो":"o","ौ":"au","ॉ":"o","ॅ":"e"};
  const VIRAMA = "्", ANUS = "ं", CHAND = "ँ", NUKTA = "़";
  const isDev = s => /[ऀ-ॿ]/.test(s);

  function translit(s){
    s = s.normalize("NFC");
    let out = "";
    const ch = [...s];
    for (let i = 0; i < ch.length; i++) {
      let c = ch[i];
      if (ch[i + 1] === NUKTA && CONS[c + NUKTA]) { c = c + NUKTA; i++; }
      else if (ch[i + 1] === NUKTA) i++;
      if (CONS[c] !== undefined) {
        out += CONS[c];
        const n = ch[i + 1];
        if (n === VIRAMA) { i++; }
        else if (MAT[n] !== undefined) { out += MAT[n]; i++; }
        else out += "a";
      } else if (IND[c] !== undefined) out += IND[c];
      else if (c === ANUS || c === CHAND) out += "n";
      else if (/[a-zA-Z0-9 ]/.test(c)) out += c;
      else if (/\s/.test(c)) out += " ";
    }
    return out;
  }

  // consonant skeleton of a name, English or Hindi
  function skey(s){
    s = (s || "").trim();
    if (!s) return "";
    if (isDev(s)) s = translit(s);
    s = s.normalize("NFKD").toLowerCase().replace(/[̀-ͯ]/g, "").replace(/[^a-z]/g, "");
    s = s.replace(/ph/g, "p").replace(/f/g, "p").replace(/h/g, "").replace(/w/g, "v").replace(/z/g, "j").replace(/q/g, "k");
    s = s.replace(/m/g, "n");                      // m and n are written interchangeably: संभर = Sambhar = Sanbhar
    s = s.replace(/[aeiouy]/g, "");
    return s.replace(/(.)\1+/g, "$1");
  }

  const STRIP = /\b(district|dist|tehsil|tahsil|taluka|taluk|tal|mandal|block|sub[- ]?division|division|rural|urban)\b/gi;
  const clean = s => (s || "").replace(STRIP, " ").replace(/\s+/g, " ").trim();

  // find the unit (from a list of {code,name}) that matches any of the candidate names
  function pick(list, candidates, loose){
    const keys = candidates.map(c => skey(clean(c))).filter(Boolean);
    for (const k of keys) { const m = list.find(u => skey(u.name) === k); if (m) return m; }
    if (!loose) return null;
    for (const k of keys) { if (k.length < 3) continue; const m = list.find(u => { const x = skey(u.name); return x.startsWith(k) || k.startsWith(x) && x.length >= 3; }); if (m) return m; }
    return null;
  }

  // villages matching what the person typed, best first
  function findVillages(list, q, limit){
    q = (q || "").trim();
    if (q.length < 1) return [];
    const k = skey(q), lq = q.toLowerCase();
    const out = [];
    for (const v of list) {
      const ln = v.name.toLowerCase(), vk = v._k || (v._k = skey(v.name));
      let score = 0;
      if (ln.startsWith(lq)) score = 1;
      else if (k && vk.startsWith(k)) score = 2;
      else if (ln.includes(lq)) score = 3;
      else if (k.length >= 3 && vk.includes(k)) score = 4;
      if (score) out.push([score, v]);
    }
    out.sort((a, b) => a[0] - b[0] || a[1].name.localeCompare(b[1].name));
    return out.slice(0, limit || 12).map(x => x[1]);
  }

  window.KaamoGeo = { skey, pick, findVillages, clean };
})();
