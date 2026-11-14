(() => {
"use strict";
const CFGX = window.KAAMO_CONFIG || {};
const DEMO = !CFGX.url || !CFGX.anonKey || /[?&]demo\b/.test(location.search);
const BUILTIN = ["plumber","electrician","construction","borewell","jcb","tractor","transport"];
const LOCK_VEHICLE = ["borewell","jcb","tractor","transport"], LOCK_FIT = ["borewell","jcb","transport"];
const $ = s => document.querySelector(s);
const esc = s => String(s ?? "").replace(/[&<>"']/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
const store = { get(k){ try { return localStorage.getItem(k); } catch(e){ return null; } }, set(k,v){ try { localStorage.setItem(k,v); } catch(e){} }, del(k){ try { localStorage.removeItem(k); } catch(e){} } };
const clone = o => JSON.parse(JSON.stringify(o));
function toast(m){ const el = $("#toast"); el.textContent = m; el.hidden = false; clearTimeout(toast.t); toast.t = setTimeout(() => el.hidden = true, 3500); }

const ERR = {
  not_admin:"आपके पास एडमिन का अधिकार नहीं है।", bad_code:"श्रेणी का कोड सिर्फ़ छोटे अंग्रेज़ी अक्षर, अंक और _ में हो (2–30)।", duplicate_code:"श्रेणी का कोड दोहराया गया है।",
  bad_label:"नाम खाली या बहुत लंबा है।", bad_sub:"किसी उप-श्रेणी का नाम खाली या बहुत लंबा है।", too_many_subs:"एक श्रेणी में अधिकतम 30 उप-श्रेणियाँ।",
  prohibited_term:"रेत, बजरी, ईंट, मिट्टी जैसे प्रतिबंधित शब्द किसी नाम या विकल्प में नहीं हो सकते।", no_enabled_category:"कम से कम एक श्रेणी चालू रखें।",
  bad_categories:"श्रेणियाँ 1 से 40 तक होनी चाहिए।", too_many_custom:"अधिकतम 20 कस्टम फ़ील्ड।", bad_field_id:"फ़ील्ड का ID सिर्फ़ छोटे अंग्रेज़ी अक्षर, अंक और _ में हो।",
  duplicate_field_id:"फ़ील्ड का ID दोहराया गया है।", bad_field_type:"फ़ील्ड का प्रकार गलत है।", bad_options:"चुनने वाली सूची में 2 से 20 विकल्प चाहिए।",
  bad_field_category:"फ़ील्ड में ऐसी श्रेणी चुनी है जो मौजूद नहीं।", bad_policy_version:"पॉलिसी का संस्करण खाली या बहुत लंबा है।", bad_mobile:"सही 10 अंकों का मोबाइल नंबर डालें।",
  bad_category:"श्रेणी सही नहीं है।", bad_days:"दिन 1 से 90 के बीच हों।", bad_config:"सेटिंग बहुत बड़ी या गलत है।",
};
const errMsg = r => { const m = (r && r.json && r.json.message) || ""; return ERR[m] || ("गड़बड़ हुई" + (m ? ": " + m : "") + "। फिर कोशिश करें।"); };

/* ---------- backend ---------- */
const sess = { s:null, load(){ try { this.s = JSON.parse(store.get("kaamo.session") || "null"); } catch(e){ this.s = null; } },
  save(j){ if (!j || !j.access_token) return false; this.s = { access_token:j.access_token, refresh_token:j.refresh_token }; store.set("kaamo.session", JSON.stringify(this.s)); return true; },
  clear(){ this.s = null; store.del("kaamo.session"); } };
async function raw(path, body, bearer){
  try {
    const r = await fetch(CFGX.url + path, { method:"POST", headers:{ apikey:CFGX.anonKey, "Content-Type":"application/json", Authorization:"Bearer " + (bearer || CFGX.anonKey) }, body:JSON.stringify(body || {}) });
    const t = await r.text(); let json = null; try { json = t ? JSON.parse(t) : null; } catch(e){}
    return { ok:r.ok, code:r.status, json };
  } catch(e){ return { ok:false, code:-1, json:null }; }
}
async function rpc(fn, args){
  if (!sess.s) return { ok:false, code:401, json:null };
  let r = await raw("/rest/v1/rpc/" + fn, args, sess.s.access_token);
  if (r.code === 401 && sess.s.refresh_token) {
    const t = await raw("/auth/v1/token?grant_type=refresh_token", { refresh_token:sess.s.refresh_token });
    if (t.ok && sess.save(t.json)) r = await raw("/rest/v1/rpc/" + fn, args, sess.s.access_token);
  }
  if (r.code === 401) sess.clear();
  return r;
}
const demoInv = () => { try { return JSON.parse(store.get("kaamo.demo.invites") || "[]"); } catch(e){ return []; } };
const B = DEMO ? {
  async sendOtp(){ return true; },
  async verify(m, code){ if (!/^\d{6}$/.test(code)) return false; sess.s = { access_token:"demo" }; return true; },
  async isAdmin(){ return true; },
  async getCfg(){ try { const o = JSON.parse(store.get("kaamo.demo.cfg") || "null"); if (o) return { version:1, config:o }; } catch(e){} return { version:1, config:clone(window.KAAMO_DEFAULT_CFG) }; },
  async saveCfg(c){ store.set("kaamo.demo.cfg", JSON.stringify(c)); return { ok:true, json:1 }; },
  async createInvite(name, mobile, category, days){
    const code = Array.from(crypto.getRandomValues(new Uint8Array(6))).map(b => b.toString(16).padStart(2,"0")).join("").toUpperCase();
    const l = demoInv(); l.unshift({ id:code, code, name, mobile, category, status:"active", expires_at:new Date(Date.now()+days*864e5).toISOString(), created_at:new Date().toISOString() });
    store.set("kaamo.demo.invites", JSON.stringify(l)); return { ok:true, json:{ code } };
  },
  async listInvites(){ return { ok:true, json:demoInv() }; },
  async revoke(id){ const l = demoInv(); l.forEach(i => { if (i.id === id) i.status = "revoked"; }); store.set("kaamo.demo.invites", JSON.stringify(l)); return { ok:true }; },
} : {
  async sendOtp(m){ return (await raw("/auth/v1/otp", { phone:"+91" + m, create_user:true })).ok; },
  async verify(m, code){ const r = await raw("/auth/v1/verify", { type:"sms", phone:"+91" + m, token:code }); return r.ok && sess.save(r.json); },
  async isAdmin(){ const r = await rpc("is_admin", {}); return r.ok && r.json === true; },
  async getCfg(){ const r = await rpc("get_form_config", {}); return r.ok ? r.json : null; },
  async saveCfg(c){ return rpc("admin_save_form_config", { p_config:c }); },
  async createInvite(name, mobile, category, days){ return rpc("admin_create_invite", { p_name:name, p_mobile:mobile || null, p_category:category || null, p_days:days }); },
  async listInvites(){ return rpc("admin_list_invites", {}); },
  async revoke(id){ return rpc("admin_revoke_invite", { p_id:id }); },
};

/* ---------- state ---------- */
const A = { stage:"login", mobile:"", otp:false, busy:false, err:"", tab:"form", cfg:null, version:0, invites:[], lastInvite:null, dirty:false };
const slug = s => (s || "").toLowerCase().replace(/[^a-z0-9]+/g, "_").replace(/^_+|_+$/g, "").slice(0, 28);
const catLabel = c => c.label_hi || c.label_en;
const BUILTIN_FIELDS = [
  ["village","गाँव / इलाका","Home village / area", true], ["district","ज़िला","District", true], ["areas","सेवा के इलाके","Areas you serve", true],
  ["rate","अनुमानित दाम","Indicative rate", true], ["gps","काम की जगह (GPS)","Use my work location (GPS)", true],
  ["rc","वाहन RC नंबर","Vehicle RC number", false], ["insurance","बीमा की तारीख","Insurance valid until", false], ["fitness","फिटनेस की तारीख","Fitness certificate valid until", false],
];
function getPath(o, p){ return p.split(".").reduce((x, k) => x == null ? x : x[k], o); }
function setPath(o, p, v){ const ks = p.split("."); const last = ks.pop(); const t = ks.reduce((x, k) => x[k], o); t[last] = v; }

/* ---------- views ---------- */
function inp(path, label, opts = {}){
  const v = getPath(A.cfg, path) ?? "";
  return `<div class="field"><label>${esc(label)}</label><input data-p="${path}" value="${esc(v)}" ${opts.ph ? `placeholder="${esc(opts.ph)}"` : ""} ${opts.disabled ? "disabled" : ""}></div>`;
}
function chk(path, label, disabled){
  return `<label class="check"><input type="checkbox" data-p="${path}" data-t="bool" ${getPath(A.cfg, path) ? "checked" : ""} ${disabled ? "disabled" : ""}><span>${esc(label)}</span></label>`;
}
const tabsBar = () => [["form","फ़ॉर्म के फ़ील्ड"],["cats","श्रेणियाँ"],["custom","कस्टम फ़ील्ड"],["policy","पॉलिसी"],["invites","इनवाइट"]]
  .map(([k,l]) => `<button class="pill ${A.tab===k?"on":""}" data-tab="${k}">${l}</button>`).join("");

function viewLogin(){
  return `<div class="screen"><div class="top"><div class="brand">Kaamo Admin</div></div><div class="stack" style="margin-top:20px;max-width:420px">
    <h2>${A.otp ? "OTP डालें" : "एडमिन लॉगिन"}</h2>
    ${A.otp ? `<div class="field"><input id="otp" inputmode="numeric" maxlength="6" style="letter-spacing:8px;text-align:center;font-size:22px" autocomplete="one-time-code"></div>${DEMO ? '<p class="xs muted">डेमो मोड: कोई भी 6 अंक चलेंगे।</p>' : ""}<button class="btn" data-a="verify" ${A.busy ? "disabled" : ""}>जाँचें</button>`
    : `<div class="field"><label>मोबाइल नंबर</label><div class="prefix"><span>+91</span><input id="mobile" inputmode="numeric" maxlength="10" value="${esc(A.mobile)}"></div></div><button class="btn" data-a="sendOtp" ${A.busy ? "disabled" : ""}>OTP भेजें</button>`}
    ${A.err ? `<div class="errtxt">${esc(A.err)}</div>` : ""}</div></div>`;
}
function viewNoAccess(){
  return `<div class="screen"><div class="top"><div class="brand">Kaamo Admin</div></div><div class="stack"><h2>पहुँच नहीं है</h2><p class="muted">इस नंबर को एडमिन का अधिकार नहीं मिला है।</p><button class="btn ghost" data-a="signOut">साइन आउट</button></div></div>`;
}
function tabForm(){
  const rows = BUILTIN_FIELDS.map(([k, hi, en, toggle]) => `<div class="card gap8"><div class="spread"><h3>${hi} <span class="muted small">(${esc(en)})</span></h3></div>
    ${toggle ? `<div class="grid2">${chk(`fields.${k}.enabled`, "फ़ॉर्म में दिखाएँ")}${chk(`fields.${k}.required`, "ज़रूरी हो", !A.cfg.fields[k].enabled)}</div>` : `<p class="xs muted">यह कानूनी तौर पर तय है (वाहन के दस्तावेज़): बंद या वैकल्पिक नहीं हो सकता। सिर्फ़ नाम बदल सकते हैं।</p>`}
    <div class="grid2">${inp(`fields.${k}.label_hi`, "हिंदी नाम (खाली = डिफ़ॉल्ट)")}${inp(`fields.${k}.label_en`, "English name (blank = default)")}</div></div>`).join("");
  return `<div class="stack"><p class="muted small">नाम और मोबाइल नंबर हमेशा ज़रूरी रहते हैं। घोषणा और फ़ोन-दिखाने की सहमति वाली स्क्रीन बदली नहीं जा सकतीं।</p>${rows}
    <div class="card">${chk("registration.invite_only", "लिस्टिंग सिर्फ़ इनवाइट से हो (बिना इनवाइट वाले नए लोग रजिस्टर नहीं कर पाएँगे)")}</div></div>`;
}
function tabCats(){
  const html = A.cfg.categories.map((c, i) => {
    const built = BUILTIN.includes(c.code), lv = LOCK_VEHICLE.includes(c.code), lf = LOCK_FIT.includes(c.code);
    const subs = (c.subs || []).map((s, j) => `<div class="row" style="margin-bottom:8px"><input data-p="categories.${i}.subs.${j}.hi" value="${esc(s.hi)}" placeholder="हिंदी" style="flex:1;border:1px solid var(--border);border-radius:12px;padding:10px;font:inherit"><input data-p="categories.${i}.subs.${j}.en" value="${esc(s.en)}" placeholder="English (पहचान)" style="flex:1;border:1px solid var(--border);border-radius:12px;padding:10px;font:inherit"><button class="mini red" data-a="rmSub" data-i="${i}" data-j="${j}">✕</button></div>`).join("");
    return `<div class="card gap8"><div class="spread"><h3>${esc(catLabel(c))} <span class="chip">${esc(c.code)}</span></h3>
      ${built ? `<span class="xs muted">बिल्ट-इन (हटाई नहीं जा सकती)</span>` : `<button class="mini red" data-a="rmCat" data-i="${i}">श्रेणी हटाएँ</button>`}</div>
      <div class="grid2">${inp(`categories.${i}.label_hi`, "हिंदी नाम")}${inp(`categories.${i}.label_en`, "English name")}</div>
      <div class="grid3">${chk(`categories.${i}.enabled`, "चालू (दिखे)")}${chk(`categories.${i}.vehicle`, "वाहन/मशीन (बीमा माँगें)", lv)}${chk(`categories.${i}.fitness`, "फिटनेस भी माँगें", lf || !c.vehicle)}</div>
      <label class="small muted">उप-श्रेणियाँ</label>${subs || '<p class="xs muted">कोई नहीं</p>'}
      <button class="mini" data-a="addSub" data-i="${i}">+ उप-श्रेणी जोड़ें</button></div>`;
  }).join("");
  return `<div class="stack"><p class="muted small">किसी श्रेणी को "चालू" से हटाने पर वह फ़ॉर्म और डायरेक्टरी से छिप जाती है। रेत, बजरी, ईंट, मिट्टी जैसे शब्द वाली श्रेणी या विकल्प सेव नहीं होंगे।</p>${html}<button class="btn ghost sm" data-a="addCat">+ नई श्रेणी</button></div>`;
}
function tabCustom(){
  const list = A.cfg.custom.map((f, i) => {
    const all = !f.categories || f.categories.includes("*");
    const catBoxes = A.cfg.categories.map(c => `<label class="check" style="padding:4px 0"><input type="checkbox" data-cfcat="${i}" value="${c.code}" ${all || f.categories.includes(c.code) ? "checked" : ""} ${all ? "disabled" : ""}><span>${esc(catLabel(c))}</span></label>`).join("");
    const opts = f.type === "select" ? `<label class="small muted">विकल्प</label>${(f.options || []).map((o, j) => `<div class="row" style="margin-bottom:8px"><input data-p="custom.${i}.options.${j}.hi" value="${esc(o.hi)}" placeholder="हिंदी" style="flex:1;border:1px solid var(--border);border-radius:12px;padding:10px;font:inherit"><input data-p="custom.${i}.options.${j}.en" value="${esc(o.en)}" placeholder="English" style="flex:1;border:1px solid var(--border);border-radius:12px;padding:10px;font:inherit"><button class="mini red" data-a="rmOpt" data-i="${i}" data-j="${j}">✕</button></div>`).join("")}<button class="mini" data-a="addOpt" data-i="${i}">+ विकल्प</button>` : "";
    return `<div class="card gap8"><div class="spread"><h3>${esc(f.label_hi || f.label_en || f.id)} <span class="chip">${esc(f.type)}</span> <span class="chip">${esc(f.id)}</span></h3><button class="mini red" data-a="rmCustom" data-i="${i}">हटाएँ</button></div>
      <div class="grid2">${inp(`custom.${i}.label_hi`, "हिंदी नाम")}${inp(`custom.${i}.label_en`, "English name")}</div>
      <div class="grid3">${chk(`custom.${i}.enabled`, "चालू")}${chk(`custom.${i}.required`, "ज़रूरी")}${chk(`custom.${i}.show_public`, "डायरेक्टरी कार्ड पर दिखाएँ")}</div>
      ${opts}
      <div><label class="small muted">किन श्रेणियों में</label><label class="check" style="padding:4px 0"><input type="checkbox" data-cfall="${i}" ${all ? "checked" : ""}><span>सभी श्रेणियाँ</span></label>${catBoxes}</div></div>`;
  }).join("");
  return `<div class="stack"><p class="muted small">अपने सवाल जोड़ें, जैसे "अनुभव (साल)" या "अपने औज़ार हैं?"। "डायरेक्टरी कार्ड पर दिखाएँ" चुनने पर जवाब ग्राहकों को दिखेगा, इसलिए उसमें फ़ोन नंबर जैसी निजी जानकारी न माँगें।</p>${list || '<p class="muted">अभी कोई कस्टम फ़ील्ड नहीं।</p>'}
    <div class="row wrap"><select id="newType" style="border:1px solid var(--border);border-radius:12px;padding:10px;font:inherit"><option value="text">टेक्स्ट</option><option value="number">नंबर</option><option value="select">चुनने वाली सूची</option><option value="yesno">हाँ / नहीं</option></select><button class="btn ghost sm" data-a="addCustom">+ नया फ़ील्ड</button></div></div>`;
}
function tabPolicy(){
  return `<div class="stack"><div class="notice"><b>कैसे काम करता है</b>नया संस्करण लिखकर सेव करने पर हर यूज़र को अगली बार ऐप खोलने पर "शर्तें बदली हैं" स्क्रीन दिखेगी और स्वीकार किए बिना आगे नहीं बढ़ पाएगा। पहले टर्म्स/प्राइवेसी पेज अपडेट करें, फिर यहाँ संस्करण बदलें। बिना बदलाव के भी हर यूज़र को साल में एक बार यही स्क्रीन दिखती है।</div>
    ${inp("policy.version", "वर्तमान संस्करण (जैसे v1.1_2026)")}
    <div class="field"><label>बदलाव का सार (हिंदी)</label><textarea data-p="policy.summary_hi" rows="4">${esc(A.cfg.policy.summary_hi)}</textarea></div>
    <div class="field"><label>Summary of changes (English)</label><textarea data-p="policy.summary_en" rows="4">${esc(A.cfg.policy.summary_en)}</textarea></div></div>`;
}
function tabInvites(){
  const link = c => location.origin + location.pathname.replace(/admin\.html$/, "") + "?invite=" + c;
  const last = A.lastInvite ? (() => { const L = link(A.lastInvite.code); const msg = `नमस्ते${A.lastInvite.name ? " " + A.lastInvite.name : ""}, Kaamo पर अपनी सेवा लिस्ट करने के लिए यह लिंक खोलें: ${L}`;
    const wa = "https://wa.me/" + (A.lastInvite.mobile ? "91" + A.lastInvite.mobile : "") + "?text=" + encodeURIComponent(msg);
    return `<div class="notice"><b>इनवाइट बन गया</b><code class="link">${esc(L)}</code><div class="row wrap"><a class="btn sm" style="text-decoration:none" href="${esc(wa)}" target="_blank" rel="noopener">WhatsApp पर भेजें</a><button class="mini" data-a="copy" data-v="${esc(L)}">लिंक कॉपी करें</button></div></div>`; })() : "";
  const st = { active:"चालू", used:"इस्तेमाल हुआ", revoked:"रद्द", expired:"समाप्त" };
  const rows = A.invites.map(i => `<tr><td>${esc(i.name || "—")}</td><td>${esc(i.mobile ? "••••••" + i.mobile.slice(-4) : "—")}</td><td>${esc(i.category ? (catLabel((A.cfg.categories.find(c => c.code === i.category)) || { label_en:i.category })) : "—")}</td><td>${st[i.status] || i.status}</td><td>${esc((i.expires_at || "").slice(0,10))}</td><td>${i.status === "active" ? `<button class="mini" data-a="copy" data-v="${esc(link(i.code))}">कॉपी</button> <button class="mini red" data-a="revoke" data-id="${esc(i.id)}">रद्द</button>` : ""}</td></tr>`).join("");
  return `<div class="stack">${last}<div class="card gap8"><h3>नया इनवाइट</h3>
    <div class="grid2"><div class="field"><label>नाम (वैकल्पिक)</label><input id="invName" maxlength="80"></div>
    <div class="field"><label>मोबाइल नंबर (वैकल्पिक: डालने पर सिर्फ़ इसी नंबर से रजिस्टर हो पाएगा)</label><input id="invMobile" inputmode="numeric" maxlength="10"></div></div>
    <div class="grid2"><div class="field"><label>श्रेणी (वैकल्पिक, पहले से भरी मिलेगी)</label><select id="invCat"><option value="">—</option>${A.cfg.categories.filter(c => c.enabled !== false).map(c => `<option value="${c.code}">${esc(catLabel(c))}</option>`).join("")}</select></div>
    <div class="field"><label>कितने दिन चले</label><input id="invDays" inputmode="numeric" value="14" maxlength="2"></div></div>
    <button class="btn sm" data-a="createInvite">इनवाइट बनाएँ</button></div>
    <div class="card"><h3 style="margin-bottom:10px">सभी इनवाइट</h3><div class="tablewrap"><table class="inv"><tr><th>नाम</th><th>नंबर</th><th>श्रेणी</th><th>स्थिति</th><th>समाप्ति</th><th></th></tr>${rows || '<tr><td colspan="6" class="muted">अभी कोई इनवाइट नहीं।</td></tr>'}</table></div></div></div>`;
}
function viewMain(){
  const body = { form:tabForm, cats:tabCats, custom:tabCustom, policy:tabPolicy, invites:tabInvites }[A.tab]();
  const showSave = A.tab !== "invites";
  return `<div class="screen"><div class="top"><div class="brand">Kaamo Admin ${DEMO ? '<span class="chip">डेमो</span>' : ""}</div><button class="link" data-a="signOut">साइन आउट</button></div>
    <p class="muted small">फ़ॉर्म संस्करण ${A.version}</p><div class="tabs">${tabsBar()}</div>${body}
    ${showSave ? `<div class="savebar"><button class="btn" data-a="save">सेव करें</button><span class="small muted">${A.dirty ? "बिना सेव किए बदलाव हैं" : ""}</span></div>` : ""}</div>`;
}
function render(){
  const keep = document.activeElement && document.activeElement.id;
  $("#app").innerHTML = A.stage === "login" ? viewLogin() : A.stage === "noaccess" ? viewNoAccess() : viewMain();
  if (keep) { const e = document.getElementById(keep); if (e) e.focus(); }
}

/* ---------- actions ---------- */
async function loadAll(){
  const r = await B.getCfg();
  if (!r) { A.err = "सेटिंग लोड नहीं हो पाई।"; A.stage = "login"; return render(); }
  A.cfg = clone(r.config); A.version = r.version; A.dirty = false;
  A.cfg.custom = A.cfg.custom || []; A.cfg.registration = A.cfg.registration || { invite_only:false };
  A.stage = "main"; render();
  const iv = await B.listInvites(); if (iv.ok && Array.isArray(iv.json)) { A.invites = iv.json; if (A.tab === "invites") render(); }
}
const act = {
  async sendOtp(){ A.mobile = ($("#mobile").value || "").replace(/\D/g, ""); if (!/^[6-9]\d{9}$/.test(A.mobile)) { A.err = "सही 10 अंकों का मोबाइल नंबर डालें।"; return render(); }
    A.busy = true; A.err = ""; render(); const ok = await B.sendOtp(A.mobile); A.busy = false; if (ok) A.otp = true; else A.err = "OTP नहीं भेज पाए।"; render(); },
  async verify(){ const code = ($("#otp").value || "").trim(); A.busy = true; render(); const ok = await B.verify(A.mobile, code); A.busy = false;
    if (!ok) { A.err = "OTP गलत या समाप्त।"; return render(); } A.err = "";
    if (!(await B.isAdmin())) { A.stage = "noaccess"; return render(); } await loadAll(); },
  signOut(){ sess.clear(); A.stage = "login"; A.otp = false; A.cfg = null; render(); },
  async save(){
    const r = await B.saveCfg(A.cfg);
    if (!r.ok) return toast(errMsg(r));
    toast("सेव हो गया।"); await loadAll();
  },
  addCat(){ const en = prompt("नई श्रेणी का नाम (English)"); if (!en) return; const code = slug(en); if (code.length < 2) return toast("नाम में अंग्रेज़ी अक्षर होने चाहिए।");
    if (A.cfg.categories.some(c => c.code === code)) return toast("यह श्रेणी पहले से है।");
    A.cfg.categories.push({ code, label_en:en.trim(), label_hi:"", enabled:true, vehicle:false, fitness:false, cev:false, subs:[] }); A.dirty = true; render(); },
  rmCat(el){ if (!confirm("यह श्रेणी हटाएँ? इसमें लिस्ट लोग डायरेक्टरी से छिप जाएँगे।")) return; A.cfg.categories.splice(+el.dataset.i, 1);
    A.cfg.custom.forEach(f => { if (f.categories) f.categories = f.categories.filter(x => x === "*" || A.cfg.categories.some(c => c.code === x)); }); A.dirty = true; render(); },
  addSub(el){ A.cfg.categories[+el.dataset.i].subs.push({ en:"", hi:"" }); A.dirty = true; render(); },
  rmSub(el){ A.cfg.categories[+el.dataset.i].subs.splice(+el.dataset.j, 1); A.dirty = true; render(); },
  addCustom(){ const type = $("#newType").value; const en = prompt("फ़ील्ड का नाम (English)"); if (!en) return; let id = slug(en); if (id.length < 2) return toast("नाम में अंग्रेज़ी अक्षर होने चाहिए।");
    if (!/^[a-z]/.test(id)) id = "f_" + id; while (A.cfg.custom.some(f => f.id === id)) id += "_2";
    A.cfg.custom.push({ id, type, label_en:en.trim(), label_hi:"", required:false, enabled:true, show_public:false, options:type === "select" ? [{en:"",hi:""},{en:"",hi:""}] : [], categories:["*"] }); A.dirty = true; render(); },
  rmCustom(el){ if (!confirm("यह फ़ील्ड हटाएँ?")) return; A.cfg.custom.splice(+el.dataset.i, 1); A.dirty = true; render(); },
  addOpt(el){ A.cfg.custom[+el.dataset.i].options.push({ en:"", hi:"" }); A.dirty = true; render(); },
  rmOpt(el){ A.cfg.custom[+el.dataset.i].options.splice(+el.dataset.j, 1); A.dirty = true; render(); },
  async createInvite(){
    const name = ($("#invName").value || "").trim(), mobile = ($("#invMobile").value || "").replace(/\D/g, ""), cat = $("#invCat").value, days = parseInt($("#invDays").value, 10) || 14;
    if (mobile && !/^[6-9]\d{9}$/.test(mobile)) return toast(ERR.bad_mobile);
    const r = await B.createInvite(name, mobile, cat, days);
    if (!r.ok) return toast(errMsg(r));
    A.lastInvite = { code:r.json.code, name, mobile }; const iv = await B.listInvites(); if (iv.ok) A.invites = iv.json; render(); },
  async revoke(el){ if (!confirm("यह इनवाइट रद्द करें?")) return; const r = await B.revoke(el.dataset.id); if (!r.ok) return toast(errMsg(r)); const iv = await B.listInvites(); if (iv.ok) A.invites = iv.json; render(); },
  async copy(el){ try { await navigator.clipboard.writeText(el.dataset.v); toast("कॉपी हो गया।"); } catch(e){ prompt("लिंक कॉपी करें:", el.dataset.v); } },
};
document.addEventListener("click", e => {
  const t = e.target.closest("[data-a],[data-tab]"); if (!t) return;
  if (t.dataset.tab) { A.tab = t.dataset.tab; return render(); }
  if (act[t.dataset.a]) act[t.dataset.a](t);
});
document.addEventListener("input", e => {
  const el = e.target; if (!el.dataset || !el.dataset.p || !A.cfg) return;
  setPath(A.cfg, el.dataset.p, el.value); A.dirty = true;
});
document.addEventListener("change", e => {
  const el = e.target; if (!A.cfg) return;
  if (el.dataset.p && el.dataset.t === "bool") {
    setPath(A.cfg, el.dataset.p, el.checked); A.dirty = true;
    if (/^fields\.\w+\.enabled$/.test(el.dataset.p) && !el.checked) setPath(A.cfg, el.dataset.p.replace("enabled", "required"), false);
    if (/^categories\.\d+\.vehicle$/.test(el.dataset.p) && !el.checked) setPath(A.cfg, el.dataset.p.replace("vehicle", "fitness"), false);
    if (/^categories\.\d+\.fitness$/.test(el.dataset.p) && el.checked) setPath(A.cfg, el.dataset.p.replace("fitness", "vehicle"), true);
    render(); return;
  }
  if (el.dataset.cfall !== undefined) { const f = A.cfg.custom[+el.dataset.cfall]; f.categories = el.checked ? ["*"] : A.cfg.categories.map(c => c.code); A.dirty = true; render(); return; }
  if (el.dataset.cfcat !== undefined) { const f = A.cfg.custom[+el.dataset.cfcat]; const set = new Set(f.categories.filter(x => x !== "*")); el.checked ? set.add(el.value) : set.delete(el.value); f.categories = [...set]; if (!f.categories.length) f.categories = ["*"]; A.dirty = true; render(); }
});
window.addEventListener("beforeunload", e => { if (A.dirty) { e.preventDefault(); e.returnValue = ""; } });

/* ---------- boot ---------- */
sess.load(); render();
(async () => {
  if (DEMO) return;
  if (sess.s) { if (await B.isAdmin()) await loadAll(); else if (sess.s) { /* expired or not admin */ const r = await rpc("is_admin", {}); if (r.code === 401) { A.stage = "login"; } else { A.stage = "noaccess"; } render(); } }
})();
})();
