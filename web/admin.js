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


/* ---------- language ---------- */
let LANG = (store.get("kaamo.admin.lang") === "en") ? "en" : "hi";
const TX = {
  en: {
    appName:"Kaamo", console:"Admin console", demo:"Demo",
    signIn:"Admin sign in", signInSub:"Sign in with the mobile number that was added as admin.", mobile:"Mobile number", sendOtp:"Send OTP",
    enterOtp:"Enter the OTP", otpSub:"We sent a 6-digit code to +91 ", verify:"Verify and sign in", demoOtp:"Demo mode: any 6 digits work.",
    noAccess:"No access", noAccessSub:"This number has not been given admin access. Ask the owner to add it.", signOut:"Sign out",
    nForm:"Form fields", nCats:"Categories", nCustom:"Custom questions", nPolicy:"Policy updates", nInvites:"Invites",
    hForm:"Form fields", dForm:"Choose what a service provider fills in when they register, and rename the fields in Hindi and English.",
    hCats:"Categories", dCats:"The kinds of work people can list. Hidden categories disappear from the form and the directory.",
    hCustom:"Custom questions", dCustom:"Add your own questions to the registration form, such as years of experience.",
    hPolicy:"Policy updates", dPolicy:"Ask every user to accept the updated Terms and Privacy Policy.",
    hInvites:"Invites", dInvites:"Send a personal link so a service provider can list with a form that is already filled in.",
    formVer:"Form version", unsaved:"You have unsaved changes", save:"Save changes", discard:"Discard", saved:"Saved.", discarded:"Changes discarded.",
    fieldCol:"Field", showCol:"On the form", hiCol:"Hindi name", enCol:"English name", defName:"Default",
    vOff:"Hidden", vOpt:"Optional", vReq:"Required", lawLocked:"Required by law", lawLockedHint:"Vehicle documents cannot be hidden or made optional. You can still rename them.",
    alwaysReq:"Name and mobile number are always required. The declaration and the phone-sharing consent screens cannot be changed.",
    inviteOnly:"Invite only", inviteOnlyHint:"When on, people without an invite link cannot register as a service provider.",
    rules:"Registration rules",
    builtIn:"Built in", hidden:"Hidden", visible:"Visible in the app", visibleHint:"Turn off to hide this category from the form and the directory.",
    catName:"Name", hindi:"Hindi", english:"English", vehicle:"Machine or vehicle", vehicleHint:"Asks for the RC number and insurance date.",
    fitness:"Fitness certificate", fitnessHint:"Also asks for the fitness certificate date. Needs a machine or vehicle.",
    subs:"Sub-categories", subHi:"Hindi", subEn:"English (used as the ID)", noSubs:"No sub-categories yet.", addSub:"Add sub-category", addCat:"Add category", rmCat:"Delete category",
    cannotDelete:"Built-in categories can be hidden but not deleted.",
    catPrompt:"New category", catPromptBody:"Enter the English name. You can add the Hindi name afterwards.", catPh:"For example: Mason",
    cfPrompt:"New question", cfPromptBody:"Enter the question in English. You can add the Hindi version afterwards.", cfPh:"For example: Years of experience",
    nameNeedsLatin:"Use English letters in the name.", catExists:"This category already exists.",
    cfEnabled:"Show on the form", cfRequired:"Required", cfPublic:"Show on directory card", cfPublicHint:"Customers will see the answer, so do not ask for private details such as a phone number.",
    cfOptions:"Choices", cfOptHi:"Hindi", cfOptEn:"English", addOpt:"Add choice", cfWhere:"Which categories", allCats:"All categories",
    tText:"Text", tNumber:"Number", tSelect:"Choice list", tYesno:"Yes / No", newField:"Add question", noCustom:"No custom questions yet. Add one to see it on the form.",
    rmCf:"Delete question", rmCfAsk:"Delete this question?", rmCfBody:"Answers already collected are kept, but the question disappears from the form.",
    rmCatAsk:"Delete this category?", rmCatBody:"People already listed under it will be hidden from the directory.",
    del:"Delete", cancel:"Cancel", ok:"OK", add:"Add",
    polHow:"How it works", polHowBody:"Update the Terms and Privacy pages first, then change the version here. After you save, every user sees a “terms have changed” screen the next time they open the app and must accept it to continue. Even without a change, each user sees it once a year.",
    polVersion:"Current version", polVersionPh:"For example: v1.1_2026", polSumHi:"Summary of changes (Hindi)", polSumEn:"Summary of changes (English)", polSumHint:"Shown to users on the screen. Keep it short and plain.",
    newInvite:"Create an invite", invName:"Name (optional)", invMobile:"Mobile number (optional)", invMobileHint:"If set, only this number can use the invite.",
    invCat:"Category (optional)", invCatHint:"Pre-selected in the form.", invDays:"Valid for (days)", create:"Create invite",
    invDone:"Invite created", invWa:"Send on WhatsApp", invCopy:"Copy link", copied:"Link copied.", copyManual:"Copy this link",
    allInvites:"All invites", thName:"Name", thMobile:"Mobile", thCat:"Category", thStatus:"Status", thExpires:"Expires", noInvites:"No invites yet. Create one above.",
    stActive:"Active", stUsed:"Used", stRevoked:"Revoked", stExpired:"Expired", revoke:"Revoke", revokeAsk:"Revoke this invite?", revokeBody:"The link will stop working straight away.",
    waMsg:(n, l) => `Hello${n ? " " + n : ""}, list your service on Kaamo using this link: ${l}`,
    loadFail:"Could not load the settings. Check your internet and try again.", badMobile:"Enter a valid 10-digit mobile number.", otpFail:"Could not send the OTP.", otpWrong:"The OTP is wrong or has expired.",
    unknown:"Something went wrong", tryAgain:"Try again.",
  },
  hi: {
    appName:"Kaamo", console:"एडमिन कंसोल", demo:"डेमो",
    signIn:"एडमिन लॉगिन", signInSub:"जिस मोबाइल नंबर को एडमिन बनाया गया है, उसी से लॉगिन करें।", mobile:"मोबाइल नंबर", sendOtp:"OTP भेजें",
    enterOtp:"OTP डालें", otpSub:"6 अंकों का कोड +91 ", verify:"जाँचें और लॉगिन करें", demoOtp:"डेमो मोड: कोई भी 6 अंक चलेंगे।",
    noAccess:"पहुँच नहीं है", noAccessSub:"इस नंबर को एडमिन का अधिकार नहीं मिला है। मालिक से जुड़वाएँ।", signOut:"साइन आउट",
    nForm:"फ़ॉर्म के फ़ील्ड", nCats:"श्रेणियाँ", nCustom:"कस्टम सवाल", nPolicy:"पॉलिसी अपडेट", nInvites:"इनवाइट",
    hForm:"फ़ॉर्म के फ़ील्ड", dForm:"रजिस्टर करते समय सेवा देने वाले को क्या भरना है, यह तय करें और फ़ील्ड के नाम हिंदी व अंग्रेज़ी में बदलें।",
    hCats:"श्रेणियाँ", dCats:"लोग किस तरह के काम लिस्ट कर सकते हैं। छिपाई गई श्रेणी फ़ॉर्म और डायरेक्टरी दोनों से हट जाती है।",
    hCustom:"कस्टम सवाल", dCustom:"रजिस्ट्रेशन फ़ॉर्म में अपने सवाल जोड़ें, जैसे अनुभव के साल।",
    hPolicy:"पॉलिसी अपडेट", dPolicy:"हर यूज़र से नई शर्तें और प्राइवेसी पॉलिसी स्वीकार करवाएँ।",
    hInvites:"इनवाइट", dInvites:"सेवा देने वाले को निजी लिंक भेजें, ताकि वह पहले से भरे हुए फ़ॉर्म से लिस्ट हो सके।",
    formVer:"फ़ॉर्म संस्करण", unsaved:"बिना सेव किए बदलाव हैं", save:"बदलाव सेव करें", discard:"रद्द करें", saved:"सेव हो गया।", discarded:"बदलाव रद्द किए।",
    fieldCol:"फ़ील्ड", showCol:"फ़ॉर्म में", hiCol:"हिंदी नाम", enCol:"English नाम", defName:"डिफ़ॉल्ट",
    vOff:"छिपा", vOpt:"वैकल्पिक", vReq:"ज़रूरी", lawLocked:"कानूनन ज़रूरी", lawLockedHint:"वाहन के दस्तावेज़ छिपाए या वैकल्पिक नहीं किए जा सकते। नाम बदल सकते हैं।",
    alwaysReq:"नाम और मोबाइल नंबर हमेशा ज़रूरी रहते हैं। घोषणा और फ़ोन दिखाने की सहमति वाली स्क्रीन बदली नहीं जा सकतीं।",
    inviteOnly:"सिर्फ़ इनवाइट से", inviteOnlyHint:"चालू होने पर बिना इनवाइट लिंक वाले लोग सेवा देने वाले के रूप में रजिस्टर नहीं कर पाएँगे।",
    rules:"रजिस्ट्रेशन के नियम",
    builtIn:"बिल्ट-इन", hidden:"छिपी", visible:"ऐप में दिखे", visibleHint:"बंद करने पर यह श्रेणी फ़ॉर्म और डायरेक्टरी से छिप जाएगी।",
    catName:"नाम", hindi:"हिंदी", english:"English", vehicle:"मशीन या वाहन", vehicleHint:"RC नंबर और बीमा की तारीख माँगता है।",
    fitness:"फिटनेस प्रमाणपत्र", fitnessHint:"फिटनेस की तारीख भी माँगता है। इसके लिए मशीन या वाहन चालू होना चाहिए।",
    subs:"उप-श्रेणियाँ", subHi:"हिंदी", subEn:"English (पहचान के लिए)", noSubs:"अभी कोई उप-श्रेणी नहीं।", addSub:"उप-श्रेणी जोड़ें", addCat:"श्रेणी जोड़ें", rmCat:"श्रेणी हटाएँ",
    cannotDelete:"बिल्ट-इन श्रेणी छिपाई जा सकती है, हटाई नहीं।",
    catPrompt:"नई श्रेणी", catPromptBody:"English में नाम लिखें। हिंदी नाम बाद में जोड़ सकते हैं।", catPh:"जैसे: Mason",
    cfPrompt:"नया सवाल", cfPromptBody:"English में सवाल लिखें। हिंदी बाद में जोड़ सकते हैं।", cfPh:"जैसे: Years of experience",
    nameNeedsLatin:"नाम में English अक्षर होने चाहिए।", catExists:"यह श्रेणी पहले से है।",
    cfEnabled:"फ़ॉर्म में दिखाएँ", cfRequired:"ज़रूरी", cfPublic:"डायरेक्टरी कार्ड पर दिखाएँ", cfPublicHint:"जवाब ग्राहकों को दिखेगा, इसलिए फ़ोन नंबर जैसी निजी जानकारी न माँगें।",
    cfOptions:"विकल्प", cfOptHi:"हिंदी", cfOptEn:"English", addOpt:"विकल्प जोड़ें", cfWhere:"किन श्रेणियों में", allCats:"सभी श्रेणियाँ",
    tText:"टेक्स्ट", tNumber:"नंबर", tSelect:"चुनने वाली सूची", tYesno:"हाँ / नहीं", newField:"सवाल जोड़ें", noCustom:"अभी कोई कस्टम सवाल नहीं। एक जोड़ें, वह फ़ॉर्म में दिखेगा।",
    rmCf:"सवाल हटाएँ", rmCfAsk:"यह सवाल हटाएँ?", rmCfBody:"पहले से मिले जवाब सुरक्षित रहेंगे, पर सवाल फ़ॉर्म से हट जाएगा।",
    rmCatAsk:"यह श्रेणी हटाएँ?", rmCatBody:"इसमें पहले से लिस्ट लोग डायरेक्टरी से छिप जाएँगे।",
    del:"हटाएँ", cancel:"रद्द करें", ok:"ठीक है", add:"जोड़ें",
    polHow:"यह कैसे काम करता है", polHowBody:"पहले टर्म्स और प्राइवेसी पेज अपडेट करें, फिर यहाँ संस्करण बदलें। सेव करने के बाद हर यूज़र को अगली बार ऐप खोलने पर “शर्तें बदली हैं” स्क्रीन दिखेगी और आगे बढ़ने के लिए स्वीकार करना होगा। बदलाव न हो तब भी हर यूज़र को साल में एक बार यह स्क्रीन दिखती है।",
    polVersion:"वर्तमान संस्करण", polVersionPh:"जैसे: v1.1_2026", polSumHi:"बदलाव का सार (हिंदी)", polSumEn:"बदलाव का सार (English)", polSumHint:"यह यूज़र को स्क्रीन पर दिखेगा। छोटा और सरल रखें।",
    newInvite:"नया इनवाइट बनाएँ", invName:"नाम (वैकल्पिक)", invMobile:"मोबाइल नंबर (वैकल्पिक)", invMobileHint:"डालने पर सिर्फ़ इसी नंबर से इनवाइट चलेगा।",
    invCat:"श्रेणी (वैकल्पिक)", invCatHint:"फ़ॉर्म में पहले से चुनी मिलेगी।", invDays:"कितने दिन चले", create:"इनवाइट बनाएँ",
    invDone:"इनवाइट बन गया", invWa:"WhatsApp पर भेजें", invCopy:"लिंक कॉपी करें", copied:"लिंक कॉपी हो गया।", copyManual:"यह लिंक कॉपी करें",
    allInvites:"सभी इनवाइट", thName:"नाम", thMobile:"मोबाइल", thCat:"श्रेणी", thStatus:"स्थिति", thExpires:"समाप्ति", noInvites:"अभी कोई इनवाइट नहीं। ऊपर से बनाएँ।",
    stActive:"चालू", stUsed:"इस्तेमाल हुआ", stRevoked:"रद्द", stExpired:"समाप्त", revoke:"रद्द करें", revokeAsk:"यह इनवाइट रद्द करें?", revokeBody:"लिंक तुरंत काम करना बंद कर देगा।",
    waMsg:(n, l) => `नमस्ते${n ? " " + n : ""}, Kaamo पर अपनी सेवा लिस्ट करने के लिए यह लिंक खोलें: ${l}`,
    loadFail:"सेटिंग लोड नहीं हो पाई। इंटरनेट जाँचकर फिर कोशिश करें।", badMobile:"सही 10 अंकों का मोबाइल नंबर डालें।", otpFail:"OTP नहीं भेज पाए।", otpWrong:"OTP गलत है या समाप्त हो गया।",
    unknown:"गड़बड़ हुई", tryAgain:"फिर कोशिश करें।",
  },
};
const ERR = {
  en: { not_admin:"You do not have admin access.", bad_code:"A category code must be 2–30 lowercase letters, digits or underscores.", duplicate_code:"A category code is used twice.",
    bad_label:"A name is empty or too long.", bad_sub:"A sub-category name is empty or too long.", too_many_subs:"A category can have at most 30 sub-categories.",
    prohibited_term:"Words like sand, gravel, bricks or soil are not allowed in any name or choice.", no_enabled_category:"Keep at least one category visible.",
    bad_categories:"There must be between 1 and 40 categories.", too_many_custom:"At most 20 custom questions.", bad_field_id:"A question ID must be lowercase letters, digits or underscores.",
    duplicate_field_id:"A question ID is used twice.", bad_field_type:"A question type is not valid.", bad_options:"A choice list needs 2 to 20 choices.",
    bad_field_category:"A question points to a category that does not exist.", bad_policy_version:"The policy version is empty or too long.", bad_mobile:"Enter a valid 10-digit mobile number.",
    bad_category:"The category is not valid.", bad_days:"Days must be between 1 and 90.", bad_config:"The settings are invalid or too large." },
  hi: { not_admin:"आपके पास एडमिन का अधिकार नहीं है।", bad_code:"श्रेणी का कोड 2–30 छोटे अंग्रेज़ी अक्षर, अंक या _ में हो।", duplicate_code:"श्रेणी का कोड दोहराया गया है।",
    bad_label:"नाम खाली है या बहुत लंबा है।", bad_sub:"किसी उप-श्रेणी का नाम खाली है या बहुत लंबा है।", too_many_subs:"एक श्रेणी में अधिकतम 30 उप-श्रेणियाँ हो सकती हैं।",
    prohibited_term:"रेत, बजरी, ईंट, मिट्टी जैसे शब्द किसी नाम या विकल्प में नहीं हो सकते।", no_enabled_category:"कम से कम एक श्रेणी दिखती रहने दें।",
    bad_categories:"श्रेणियाँ 1 से 40 के बीच होनी चाहिए।", too_many_custom:"अधिकतम 20 कस्टम सवाल।", bad_field_id:"सवाल की ID छोटे अंग्रेज़ी अक्षर, अंक या _ में हो।",
    duplicate_field_id:"सवाल की ID दोहराई गई है।", bad_field_type:"सवाल का प्रकार सही नहीं है।", bad_options:"चुनने वाली सूची में 2 से 20 विकल्प चाहिए।",
    bad_field_category:"किसी सवाल में ऐसी श्रेणी चुनी है जो मौजूद नहीं।", bad_policy_version:"पॉलिसी का संस्करण खाली है या बहुत लंबा है।", bad_mobile:"सही 10 अंकों का मोबाइल नंबर डालें।",
    bad_category:"श्रेणी सही नहीं है।", bad_days:"दिन 1 से 90 के बीच हों।", bad_config:"सेटिंग गलत है या बहुत बड़ी है।" },
};
const t = k => { const v = TX[LANG][k]; return v === undefined ? k : v; };
const errMsg = r => { const m = (r && r.json && r.json.message) || ""; return ERR[LANG][m] || (t("unknown") + (m ? ": " + m : "") + ". " + t("tryAgain")); };

/* ---------- state ---------- */
const A = { stage:"login", mobile:"", otp:false, busy:false, err:"", tab:"form", cfg:null, version:0, invites:[], lastInvite:null, dirty:false, open:new Set() };
const slug = s => (s || "").toLowerCase().replace(/[^a-z0-9]+/g, "_").replace(/^_+|_+$/g, "").slice(0, 28);
const catLabel = c => (LANG === "hi" ? (c.label_hi || c.label_en) : (c.label_en || c.label_hi)) || c.code;
const BUILTIN_FIELDS = [
  ["village","गाँव / इलाका","Home village / area", true], ["district","ज़िला","District", true], ["areas","सेवा के इलाके","Areas you serve", true],
  ["rate","अनुमानित दाम","Indicative rate", true], ["gps","काम की जगह (GPS)","Use my work location (GPS)", true],
  ["rc","वाहन RC नंबर","Vehicle RC number", false], ["insurance","बीमा की तारीख","Insurance valid until", false], ["fitness","फिटनेस की तारीख","Fitness certificate valid until", false],
];
const getPath = (o, p) => p.split(".").reduce((x, k) => x == null ? x : x[k], o);
const setPath = (o, p, v) => { const ks = p.split("."); const last = ks.pop(); ks.reduce((x, k) => x[k], o)[last] = v; };

/* ---------- small ui pieces ---------- */
const ICON = {
  form:'<path d="M5 4h14v16H5z"/><path d="M9 9h6M9 13h6M9 17h3"/>', cats:'<path d="M4 4h7l9 9-7 7-9-9z"/><circle cx="8.5" cy="8.5" r="1"/>',
  custom:'<path d="M12 5v14M5 12h14"/><rect x="3.5" y="3.5" width="17" height="17" rx="3"/>', policy:'<path d="M12 3l7 3v5c0 5-3 8-7 10-4-2-7-5-7-10V6z"/><path d="M9 12l2 2 4-4"/>',
  invites:'<path d="M4 6h16v12H4z"/><path d="M4 7l8 6 8-6"/>', lock:'<rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V8a4 4 0 018 0v3"/>',
  trash:'<path d="M5 7h14M9 7V4h6v3M7 7l1 13h8l1-13"/>', plus:'<path d="M12 5v14M5 12h14"/>', out:'<path d="M9 4H5v16h4M16 8l4 4-4 4M20 12H9"/>', chev:'<path d="M9 6l6 6-6 6"/>',
};
const ic = (n, cls = "") => `<svg class="i ${cls}" viewBox="0 0 24 24" aria-hidden="true">${ICON[n]}</svg>`;
const LOGO = `<svg class="logo" viewBox="0 0 525 781" aria-hidden="true"><g transform="translate(-377,-177)"><path fill="#fff" d="M377,180H464A88,88 0 0 1 552,268V870A88,88 0 0 1 464,958H377Z M815,177H902V453A88,88 0 0 1 814,541H572V453A88,88 0 0 1 660,365H727V265A88,88 0 0 1 815,177Z M572,653A88,88 0 0 1 660,565H814A88,88 0 0 1 902,653V955H815A88,88 0 0 1 727,867V740H660A88,88 0 0 1 572,653Z"/></g></svg>`;
const langSeg = light => `<div class="lang ${light ? "light" : ""}" role="group" aria-label="Language"><button data-lang="en" class="${LANG==="en"?"on":""}" ${LANG==="en"?'aria-pressed="true"':'aria-pressed="false"'}>EN</button><button data-lang="hi" class="${LANG==="hi"?"on":""}" ${LANG==="hi"?'aria-pressed="true"':'aria-pressed="false"'}>हिन्दी</button></div>`;
const sw = (path, disabled) => `<span class="sw"><input type="checkbox" data-p="${path}" data-t="bool" ${getPath(A.cfg, path) ? "checked" : ""} ${disabled ? "disabled" : ""}><i></i></span>`;
const setting = (path, title, hint, disabled) => `<label class="setting ${disabled ? "off" : ""}"><span class="txt"><b>${esc(title)}</b>${hint ? `<small>${esc(hint)}</small>` : ""}</span>${sw(path, disabled)}</label>`;
const field = (path, label, ph) => `<div class="fld"><label>${esc(label)}</label><input type="text" data-p="${path}" value="${esc(getPath(A.cfg, path) ?? "")}" ${ph ? `placeholder="${esc(ph)}"` : ""}></div>`;
const isOpen = k => A.open.has(k) ? "open" : "";

function ask({ title, body, input, value, placeholder, ok, danger }){
  return new Promise(res => {
    const d = $("#dlg");
    d.innerHTML = `<form method="dialog" class="dlg"><h3>${esc(title)}</h3>${body ? `<p>${esc(body)}</p>` : ""}${input ? `<input type="text" id="dlgIn" value="${esc(value || "")}" placeholder="${esc(placeholder || "")}" autocomplete="off">` : ""}
      <div class="acts"><button class="btn ${danger ? "danger" : ""}" value="ok">${esc(ok || t("ok"))}</button><button class="btn ghost" value="cancel" formnovalidate>${esc(t("cancel"))}</button></div></form>`;
    d.onclose = () => res(d.returnValue === "ok" ? (input ? ($("#dlgIn").value || "").trim() : true) : null);
    d.returnValue = ""; d.showModal();
    const i = $("#dlgIn"); if (i) { i.focus(); i.select(); }
  });
}

/* ---------- views ---------- */
function viewAuth(inner){
  return `<div class="auth"><div class="card"><div class="tools">${langSeg(true)}</div>
    <div class="brand">${LOGO.replace('#fff', '#064E3B')}<div><b>${t("appName")}</b><small>${t("console")}${DEMO ? `<span class="demo">${t("demo")}</span>` : ""}</small></div></div>${inner}</div></div>`;
}
function viewLogin(){
  const inner = A.otp
    ? `<h1>${t("enterOtp")}</h1><p class="sub">${t("otpSub")}${esc(A.mobile)}</p>
       <div class="fld"><input type="text" id="otp" class="otp" inputmode="numeric" maxlength="6" autocomplete="one-time-code" aria-label="OTP"></div>
       ${DEMO ? `<p class="sub" style="margin:10px 0 0;font-size:13px">${t("demoOtp")}</p>` : ""}
       ${A.err ? `<p class="errtxt" style="margin-top:10px">${esc(A.err)}</p>` : ""}<button class="btn" data-a="verify" ${A.busy ? "disabled" : ""}>${t("verify")}</button>`
    : `<h1>${t("signIn")}</h1><p class="sub">${t("signInSub")}</p>
       <div class="fld"><label for="mobile">${t("mobile")}</label><div class="prefix"><span>+91</span><input type="text" id="mobile" inputmode="numeric" maxlength="10" value="${esc(A.mobile)}" autocomplete="tel-national"></div></div>
       ${A.err ? `<p class="errtxt" style="margin-top:10px">${esc(A.err)}</p>` : ""}<button class="btn" data-a="sendOtp" ${A.busy ? "disabled" : ""}>${t("sendOtp")}</button>`;
  return viewAuth(inner);
}
const viewNoAccess = () => viewAuth(`<h1>${t("noAccess")}</h1><p class="sub">${t("noAccessSub")}</p><button class="btn ghost" data-a="signOut">${t("signOut")}</button>`);

function tabForm(){
  const vis = (k, locked) => {
    const f = A.cfg.fields[k]; const v = !f.enabled ? "off" : f.required ? "req" : "opt";
    const b = (val, label, cls) => `<button type="button" role="radio" aria-checked="${v === val}" class="${cls || ""}" data-a="vis" data-k="${k}" data-v="${val}">${label}</button>`;
    return `<div class="seg" role="radiogroup" aria-label="${esc(t("showCol"))}">${b("off", t("vOff"), "off")}${b("opt", t("vOpt"))}${b("req", t("vReq"))}</div>`;
  };
  const rows = BUILTIN_FIELDS.map(([k, hi, en, toggle]) => {
    const name = LANG === "hi" ? hi : en, other = LANG === "hi" ? en : hi;
    return `<div class="row-s"><div class="nm"><b>${esc(name)}</b><small>${esc(other)}</small></div>
      <div style="min-width:236px">${toggle ? vis(k) : `<span class="chip lock">${ic("lock")}${t("lawLocked")}</span>`}</div>
      <div class="fld"><label class="sr">${t("hiCol")}</label><input type="text" data-p="fields.${k}.label_hi" value="${esc(A.cfg.fields[k].label_hi || "")}" placeholder="${esc(t("defName"))}: ${esc(hi)}"></div>
      <div class="fld"><label class="sr">${t("enCol")}</label><input type="text" data-p="fields.${k}.label_en" value="${esc(A.cfg.fields[k].label_en || "")}" placeholder="${esc(t("defName"))}: ${esc(en)}"></div></div>`;
  }).join("");
  return `<div class="note" style="margin-bottom:16px">${t("alwaysReq")}</div>
    <div class="panel"><div class="colh"><span>${t("fieldCol")}</span><span>${t("showCol")}</span><span>${t("hiCol")}</span><span>${t("enCol")}</span></div>${rows}</div>
    <div class="panel"><header><h2>${t("rules")}</h2></header><div class="body">${setting("registration.invite_only", t("inviteOnly"), t("inviteOnlyHint"))}</div></div>`;
}
function tabCats(){
  const items = A.cfg.categories.map((c, i) => {
    const built = BUILTIN.includes(c.code), lv = LOCK_VEHICLE.includes(c.code), lf = LOCK_FIT.includes(c.code);
    const subs = (c.subs || []).map((s, j) => `<div class="subrow"><input type="text" data-p="categories.${i}.subs.${j}.hi" value="${esc(s.hi)}" placeholder="${esc(t("subHi"))}"><input type="text" data-p="categories.${i}.subs.${j}.en" value="${esc(s.en)}" placeholder="${esc(t("subEn"))}"><button class="ico" data-a="rmSub" data-i="${i}" data-j="${j}" aria-label="${esc(t("del"))}">${ic("trash")}</button></div>`).join("");
    return `<details class="item" data-k="cat:${esc(c.code)}" ${isOpen("cat:" + c.code)}><summary>${ic("chev", "chev")}<span class="ttl">${esc(catLabel(c))}<small>${esc(c.code)}</small></span>
        ${c.enabled === false ? `<span class="chip">${t("hidden")}</span>` : ""}${built ? `<span class="chip">${t("builtIn")}</span>` : ""}</summary>
      <div class="inner">
        ${setting(`categories.${i}.enabled`, t("visible"), t("visibleHint"))}
        <div class="grid2">${field(`categories.${i}.label_hi`, t("catName") + " (" + t("hindi") + ")")}${field(`categories.${i}.label_en`, t("catName") + " (" + t("english") + ")")}</div>
        ${setting(`categories.${i}.vehicle`, t("vehicle"), t("vehicleHint"), lv)}
        ${setting(`categories.${i}.fitness`, t("fitness"), t("fitnessHint"), lf || !c.vehicle)}
        <div><div class="sec">${t("subs")}</div>${subs || `<p class="lbl">${t("noSubs")}</p>`}<button class="add" data-a="addSub" data-i="${i}">${ic("plus")}${t("addSub")}</button></div>
        <div>${built ? `<p class="lbl">${t("cannotDelete")}</p>` : `<button class="btn ghost sm" data-a="rmCat" data-i="${i}">${ic("trash")}${t("rmCat")}</button>`}</div>
      </div></details>`;
  }).join("");
  return `<div class="panel">${items}</div><button class="add" data-a="addCat">${ic("plus")}${t("addCat")}</button>`;
}
function tabCustom(){
  const typeName = { text:t("tText"), number:t("tNumber"), select:t("tSelect"), yesno:t("tYesno") };
  const items = A.cfg.custom.map((f, i) => {
    const all = !f.categories || f.categories.includes("*");
    const catBoxes = A.cfg.categories.map(c => `<label class="setting" style="padding:5px 0"><span class="txt"><b style="font-weight:500">${esc(catLabel(c))}</b></span><span class="sw"><input type="checkbox" data-cfcat="${i}" value="${esc(c.code)}" ${all || f.categories.includes(c.code) ? "checked" : ""} ${all ? "disabled" : ""}><i></i></span></label>`).join("");
    const opts = f.type === "select" ? `<div><div class="sec">${t("cfOptions")}</div>${(f.options || []).map((o, j) => `<div class="subrow"><input type="text" data-p="custom.${i}.options.${j}.hi" value="${esc(o.hi)}" placeholder="${esc(t("cfOptHi"))}"><input type="text" data-p="custom.${i}.options.${j}.en" value="${esc(o.en)}" placeholder="${esc(t("cfOptEn"))}"><button class="ico" data-a="rmOpt" data-i="${i}" data-j="${j}" aria-label="${esc(t("del"))}">${ic("trash")}</button></div>`).join("")}<button class="add" data-a="addOpt" data-i="${i}">${ic("plus")}${t("addOpt")}</button></div>` : "";
    const ttl = (LANG === "hi" ? (f.label_hi || f.label_en) : (f.label_en || f.label_hi)) || f.id;
    return `<details class="item" data-k="cf:${esc(f.id)}" ${isOpen("cf:" + f.id)}><summary>${ic("chev", "chev")}<span class="ttl">${esc(ttl)}<small>${esc(typeName[f.type] || f.type)}</small></span>${f.enabled === false ? `<span class="chip">${t("hidden")}</span>` : ""}${f.required ? `<span class="chip">${t("vReq")}</span>` : ""}</summary>
      <div class="inner">
        <div class="grid2">${field(`custom.${i}.label_hi`, t("hindi"))}${field(`custom.${i}.label_en`, t("english"))}</div>
        ${setting(`custom.${i}.enabled`, t("cfEnabled"))}${setting(`custom.${i}.required`, t("cfRequired"))}${setting(`custom.${i}.show_public`, t("cfPublic"), t("cfPublicHint"))}
        ${opts}
        <div><div class="sec">${t("cfWhere")}</div><label class="setting" style="padding:5px 0"><span class="txt"><b>${t("allCats")}</b></span><span class="sw"><input type="checkbox" data-cfall="${i}" ${all ? "checked" : ""}><i></i></span></label>${catBoxes}</div>
        <div><button class="btn ghost sm" data-a="rmCustom" data-i="${i}">${ic("trash")}${t("rmCf")}</button></div>
      </div></details>`;
  }).join("");
  return `${items ? `<div class="panel">${items}</div>` : `<div class="panel"><div class="empty">${t("noCustom")}</div></div>`}
    <div class="row-add" style="display:flex;gap:10px;flex-wrap:wrap;align-items:center"><select id="newType" style="width:auto" aria-label="${esc(t("newField"))}"><option value="text">${t("tText")}</option><option value="number">${t("tNumber")}</option><option value="select">${t("tSelect")}</option><option value="yesno">${t("tYesno")}</option></select><button class="add" data-a="addCustom">${ic("plus")}${t("newField")}</button></div>`;
}
function tabPolicy(){
  return `<div class="note" style="margin-bottom:16px"><b>${t("polHow")}</b>${t("polHowBody")}</div>
    <div class="panel"><div class="body">
      <div class="fld" style="max-width:280px">${field("policy.version", t("polVersion"), t("polVersionPh"))}</div>
      <div class="fld"><label>${t("polSumHi")}</label><textarea data-p="policy.summary_hi" rows="4">${esc(A.cfg.policy.summary_hi)}</textarea></div>
      <div class="fld"><label>${t("polSumEn")}</label><textarea data-p="policy.summary_en" rows="4">${esc(A.cfg.policy.summary_en)}</textarea><p class="lbl" style="margin-top:6px">${t("polSumHint")}</p></div>
    </div></div>`;
}
const inviteLink = c => location.origin + location.pathname.replace(/admin\.html$/, "") + "?invite=" + c;
function tabInvites(){
  const last = A.lastInvite ? (() => { const L = inviteLink(A.lastInvite.code); const msg = TX[LANG].waMsg(A.lastInvite.name, L);
    const wa = "https://wa.me/" + (A.lastInvite.mobile ? "91" + A.lastInvite.mobile : "") + "?text=" + encodeURIComponent(msg);
    return `<div class="note" style="margin-bottom:16px"><b>${t("invDone")}</b><code class="link">${esc(L)}</code><div style="display:flex;gap:10px;flex-wrap:wrap"><a class="btn sm" href="${esc(wa)}" target="_blank" rel="noopener">${t("invWa")}</a><button class="btn ghost sm" data-a="copy" data-v="${esc(L)}">${t("invCopy")}</button></div></div>`; })() : "";
  const st = { active:["stActive","ok"], used:["stUsed",""], revoked:["stRevoked","bad"], expired:["stExpired",""] };
  const rows = A.invites.map(i => { const s = st[i.status] || [i.status, ""]; return `<tr><td>${esc(i.name || "—")}</td><td>${esc(i.mobile ? "•••••• " + i.mobile.slice(-4) : "—")}</td>
    <td>${esc(i.category ? catLabel(A.cfg.categories.find(c => c.code === i.category) || { code:i.category, label_en:i.category }) : "—")}</td>
    <td><span class="chip ${s[1]}">${esc(t(s[0]))}</span></td><td>${esc((i.expires_at || "").slice(0,10))}</td>
    <td style="text-align:right;white-space:nowrap">${i.status === "active" ? `<button class="btn ghost sm" data-a="copy" data-v="${esc(inviteLink(i.code))}">${t("invCopy")}</button> <button class="btn ghost sm" data-a="revoke" data-id="${esc(i.id)}">${t("revoke")}</button>` : ""}</td></tr>`; }).join("");
  return `${last}<div class="panel"><header><h2>${t("newInvite")}</h2></header><div class="body">
      <div class="grid2"><div class="fld"><label for="invName">${t("invName")}</label><input type="text" id="invName" maxlength="80"></div>
        <div class="fld"><label for="invMobile">${t("invMobile")}</label><input type="text" id="invMobile" inputmode="numeric" maxlength="10"><p class="lbl" style="margin:5px 0 0">${t("invMobileHint")}</p></div></div>
      <div class="grid2"><div class="fld"><label for="invCat">${t("invCat")}</label><select id="invCat"><option value="">—</option>${A.cfg.categories.filter(c => c.enabled !== false).map(c => `<option value="${esc(c.code)}">${esc(catLabel(c))}</option>`).join("")}</select><p class="lbl" style="margin:5px 0 0">${t("invCatHint")}</p></div>
        <div class="fld"><label for="invDays">${t("invDays")}</label><input type="text" id="invDays" inputmode="numeric" value="14" maxlength="2"></div></div>
      <div><button class="btn" data-a="createInvite">${t("create")}</button></div></div></div>
    <div class="panel"><header><h2>${t("allInvites")}</h2></header>${rows ? `<div class="tablewrap"><table class="t"><tr><th>${t("thName")}</th><th>${t("thMobile")}</th><th>${t("thCat")}</th><th>${t("thStatus")}</th><th>${t("thExpires")}</th><th></th></tr>${rows}</table></div>` : `<div class="empty">${t("noInvites")}</div>`}</div>`;
}
const TABS = [["form","nForm","hForm","dForm"],["cats","nCats","hCats","dCats"],["custom","nCustom","hCustom","dCustom"],["policy","nPolicy","hPolicy","dPolicy"],["invites","nInvites","hInvites","dInvites"]];
function viewMain(){
  const cur = TABS.find(x => x[0] === A.tab);
  const body = { form:tabForm, cats:tabCats, custom:tabCustom, policy:tabPolicy, invites:tabInvites }[A.tab]();
  return `<div class="shell"><aside class="side"><div class="brand">${LOGO}<div><b>${t("appName")}</b><small>${t("console")}${DEMO ? `<span class="demo">${t("demo")}</span>` : ""}</small></div></div>
      <nav class="nav" aria-label="${esc(t("console"))}">${TABS.map(([k, n]) => `<button data-tab="${k}" ${A.tab === k ? 'aria-current="page"' : ""}>${ic(k)}<span>${t(n)}</span></button>`).join("")}</nav>
      <div class="foot">${langSeg(false)}<button class="signout" data-a="signOut">${ic("out")}<span>${t("signOut")}</span></button></div></aside>
    <main class="main"><div class="head"><h1>${t(cur[2])}</h1><p>${t(cur[3])}</p>${A.tab !== "invites" && A.tab !== "policy" ? `<div class="meta"><span class="chip">${t("formVer")} ${A.version}</span></div>` : ""}</div>${body}</main>
    <div class="bar" id="bar" ${A.dirty ? "" : "hidden"}><span class="msg">${t("unsaved")}</span><button class="btn ghost" data-a="discard">${t("discard")}</button><button class="btn" data-a="save">${t("save")}</button></div></div>`;
}
function render(){
  const ae = document.activeElement, keep = ae && ae.id, scroll = window.scrollY;
  document.documentElement.lang = LANG;
  $("#app").innerHTML = A.stage === "login" ? viewLogin() : A.stage === "noaccess" ? viewNoAccess() : viewMain();
  if (keep) { const e = document.getElementById(keep); if (e) e.focus(); }
  window.scrollTo(0, scroll);
}
const syncBar = () => { const b = $("#bar"); if (b) b.hidden = !A.dirty; };
const dirty = () => { A.dirty = true; syncBar(); };

/* ---------- actions ---------- */
async function loadAll(){
  const r = await B.getCfg();
  if (!r) { A.err = t("loadFail"); A.stage = "login"; return render(); }
  A.cfg = clone(r.config); A.version = r.version; A.dirty = false;
  A.cfg.custom = A.cfg.custom || []; A.cfg.registration = A.cfg.registration || { invite_only:false };
  A.stage = "main"; render();
  const iv = await B.listInvites(); if (iv.ok && Array.isArray(iv.json)) { A.invites = iv.json; if (A.tab === "invites") render(); }
}
const refreshInvites = async () => { const iv = await B.listInvites(); if (iv.ok && Array.isArray(iv.json)) A.invites = iv.json; };
const act = {
  async sendOtp(){ A.mobile = ($("#mobile").value || "").replace(/\D/g, ""); if (!/^[6-9]\d{9}$/.test(A.mobile)) { A.err = t("badMobile"); return render(); }
    A.busy = true; A.err = ""; render(); const ok = await B.sendOtp(A.mobile); A.busy = false; if (ok) A.otp = true; else A.err = t("otpFail"); render(); },
  async verify(){ const code = ($("#otp").value || "").trim(); A.busy = true; render(); const ok = await B.verify(A.mobile, code); A.busy = false;
    if (!ok) { A.err = t("otpWrong"); return render(); } A.err = "";
    if (!(await B.isAdmin())) { A.stage = "noaccess"; return render(); } await loadAll(); },
  signOut(){ sess.clear(); A.stage = "login"; A.otp = false; A.cfg = null; A.dirty = false; A.err = ""; render(); },
  async save(){ const r = await B.saveCfg(A.cfg); if (!r.ok) return toast(errMsg(r)); toast(t("saved")); await loadAll(); },
  async discard(){ await loadAll(); toast(t("discarded")); },
  vis(el){ const f = A.cfg.fields[el.dataset.k], v = el.dataset.v; f.enabled = v !== "off"; f.required = v === "req"; dirty(); render(); },
  async addCat(){
    const en = await ask({ title:t("catPrompt"), body:t("catPromptBody"), input:true, placeholder:t("catPh"), ok:t("add") }); if (!en) return;
    const code = slug(en); if (code.length < 2) return toast(t("nameNeedsLatin"));
    if (A.cfg.categories.some(c => c.code === code)) return toast(t("catExists"));
    A.cfg.categories.push({ code, label_en:en, label_hi:"", enabled:true, vehicle:false, fitness:false, cev:false, subs:[] }); A.open.add("cat:" + code); dirty(); render(); },
  async rmCat(el){ if (!(await ask({ title:t("rmCatAsk"), body:t("rmCatBody"), ok:t("del"), danger:true }))) return; A.cfg.categories.splice(+el.dataset.i, 1);
    A.cfg.custom.forEach(f => { if (f.categories) f.categories = f.categories.filter(x => x === "*" || A.cfg.categories.some(c => c.code === x)); }); dirty(); render(); },
  addSub(el){ A.cfg.categories[+el.dataset.i].subs.push({ en:"", hi:"" }); dirty(); render(); },
  rmSub(el){ A.cfg.categories[+el.dataset.i].subs.splice(+el.dataset.j, 1); dirty(); render(); },
  async addCustom(){
    const type = $("#newType").value; const en = await ask({ title:t("cfPrompt"), body:t("cfPromptBody"), input:true, placeholder:t("cfPh"), ok:t("add") }); if (!en) return;
    let id = slug(en); if (id.length < 2) return toast(t("nameNeedsLatin"));
    if (!/^[a-z]/.test(id)) id = "f_" + id; while (A.cfg.custom.some(f => f.id === id)) id += "_2";
    A.cfg.custom.push({ id, type, label_en:en, label_hi:"", required:false, enabled:true, show_public:false, options:type === "select" ? [{en:"",hi:""},{en:"",hi:""}] : [], categories:["*"] }); A.open.add("cf:" + id); dirty(); render(); },
  async rmCustom(el){ if (!(await ask({ title:t("rmCfAsk"), body:t("rmCfBody"), ok:t("del"), danger:true }))) return; A.cfg.custom.splice(+el.dataset.i, 1); dirty(); render(); },
  addOpt(el){ A.cfg.custom[+el.dataset.i].options.push({ en:"", hi:"" }); dirty(); render(); },
  rmOpt(el){ A.cfg.custom[+el.dataset.i].options.splice(+el.dataset.j, 1); dirty(); render(); },
  async createInvite(){
    const name = ($("#invName").value || "").trim(), mobile = ($("#invMobile").value || "").replace(/\D/g, ""), cat = $("#invCat").value, days = parseInt($("#invDays").value, 10) || 14;
    if (mobile && !/^[6-9]\d{9}$/.test(mobile)) return toast(t("badMobile"));
    const r = await B.createInvite(name, mobile, cat, days); if (!r.ok) return toast(errMsg(r));
    A.lastInvite = { code:r.json.code, name, mobile }; await refreshInvites(); render(); },
  async revoke(el){ if (!(await ask({ title:t("revokeAsk"), body:t("revokeBody"), ok:t("revoke"), danger:true }))) return;
    const r = await B.revoke(el.dataset.id); if (!r.ok) return toast(errMsg(r)); await refreshInvites(); render(); },
  async copy(el){ try { await navigator.clipboard.writeText(el.dataset.v); toast(t("copied")); } catch(e){ await ask({ title:t("copyManual"), input:true, value:el.dataset.v }); } },
};
document.addEventListener("click", e => {
  const l = e.target.closest("[data-lang]"); if (l) { LANG = l.dataset.lang; store.set("kaamo.admin.lang", LANG); return render(); }
  const tb = e.target.closest("[data-tab]"); if (tb) { A.tab = tb.dataset.tab; render(); return window.scrollTo(0, 0); }
  const a = e.target.closest("[data-a]"); if (a && act[a.dataset.a]) act[a.dataset.a](a);
});
document.addEventListener("input", e => { const el = e.target; if (!el.dataset || !el.dataset.p || !A.cfg || el.dataset.t === "bool") return; setPath(A.cfg, el.dataset.p, el.value); dirty(); });
document.addEventListener("toggle", e => { const d = e.target; if (!d.matches || !d.matches("details.item")) return; d.open ? A.open.add(d.dataset.k) : A.open.delete(d.dataset.k); }, true);
document.addEventListener("change", e => {
  const el = e.target; if (!A.cfg) return;
  if (el.dataset.p && el.dataset.t === "bool") {
    setPath(A.cfg, el.dataset.p, el.checked); dirty();
    if (/^categories\.\d+\.vehicle$/.test(el.dataset.p) && !el.checked) setPath(A.cfg, el.dataset.p.replace("vehicle", "fitness"), false);
    if (/^categories\.\d+\.fitness$/.test(el.dataset.p) && el.checked) setPath(A.cfg, el.dataset.p.replace("fitness", "vehicle"), true);
    render(); return;
  }
  if (el.dataset.cfall !== undefined) { const f = A.cfg.custom[+el.dataset.cfall]; f.categories = el.checked ? ["*"] : A.cfg.categories.map(c => c.code); dirty(); render(); return; }
  if (el.dataset.cfcat !== undefined) { const f = A.cfg.custom[+el.dataset.cfcat]; const set = new Set(f.categories.filter(x => x !== "*")); el.checked ? set.add(el.value) : set.delete(el.value); f.categories = [...set]; if (!f.categories.length) f.categories = ["*"]; dirty(); render(); }
});
window.addEventListener("beforeunload", e => { if (A.dirty) { e.preventDefault(); e.returnValue = ""; } });

/* ---------- boot ---------- */
sess.load(); render();
(async () => {
  if (DEMO) return;
  if (sess.s) { if (await B.isAdmin()) await loadAll(); else { const r = await rpc("is_admin", {}); A.stage = r.code === 401 ? "login" : "noaccess"; render(); } }
})();
})();
