(() => {
"use strict";
const APP_NAME = "Kaamo";
const CFG = window.KAAMO_CONFIG || {};
const DEMO = !CFG.url || !CFG.anonKey || /[?&]demo\b/.test(location.search);
const VER = "v1.0_2026";
const GRIEVANCE_EMAIL = "grievance@kaamo.com";

/* ---------------- storage helpers ---------------- */
const store = {
  get(k){ try { return localStorage.getItem(k); } catch(e){ return null; } },
  set(k,v){ try { localStorage.setItem(k,v); } catch(e){} },
  del(k){ try { localStorage.removeItem(k); } catch(e){} },
};

/* ---------------- i18n ---------------- */
const CATS = [
  {code:"plumber", en:"Plumber", hi:"प्लंबर", pen:"Plumbers", phi:"प्लंबर", vehicle:false, subs:["Pipe fitting","Leak repair","Bathroom fitting","Water tank"]},
  {code:"electrician", en:"Electrician", hi:"इलेक्ट्रीशियन", pen:"Electricians", phi:"इलेक्ट्रीशियन", vehicle:false, subs:["House wiring","Fan / light repair","Inverter / UPS"]},
  {code:"construction", en:"Construction workers", hi:"निर्माण मज़दूर", pen:"Workers", phi:"मज़दूर", vehicle:false, subs:["Mason (mistri)","Labour","Painter","Carpenter","Tiles"]},
  {code:"borewell", en:"Borewell", hi:"बोरवेल", pen:"Borewell", phi:"बोरवेल", vehicle:true, fitness:true, subs:["Borewell drilling","Motor / pump repair","Flushing"]},
  {code:"jcb", en:"JCB / Earthmover", hi:"JCB / अर्थमूवर", pen:"JCB", phi:"JCB", vehicle:true, fitness:true, subs:["Excavation","Loading","Demolition","Leveling"]},
  {code:"tractor", en:"Tractor", hi:"ट्रैक्टर", pen:"Tractors", phi:"ट्रैक्टर", vehicle:true, subs:["Trolley","Cultivator","Water Tanker","Thresher"]},
  {code:"transport", en:"Transport / Trailer", hi:"ट्रांसपोर्ट / ट्रेलर", pen:"Transport", phi:"ट्रांसपोर्ट", vehicle:true, fitness:true, subs:[]},
];
const catBy = c => CATS.find(x => x.code === c);

const T = {
  en: {
    tagline:"Find local help, fast", ask:"What work do you need?",
    chooseRole:"How will you use Kaamo?", roleCustomer:"I need work done", roleCustomerSub:"Find plumbers, borewell, JCB, workers near you",
    rolePartner:"I provide a service", rolePartnerSub:"List yourself or your machine and get calls",
    directoryOnly:"Kaamo is a directory only. We don't own, verify, insure or supervise any service provider, machine, operator or work.",
    back:"Back", continue:"Continue", language:"Language",
    mobile:"Mobile number", sendOtp:"Send OTP", enterOtp:"Enter the 6-digit OTP", verify:"Verify & continue", resend:"Change number",
    otpSent:"OTP sent to +91 ", otpFail:"Could not send OTP. Try again.", otpBad:"Wrong or expired OTP.",
    demoNote:"Demo mode: any 6-digit code works.",
    agreeTerms:"I have read and agree to the Terms of Use and Privacy Notice.", terms:"Terms", privacy:"Privacy",
    yourName:"Your name", yourArea:"Your area / city (optional)", finish:"Finish",
    homeVillage:"Home village / area", district:"District", category:"Service category", subs:"What do you do?",
    cevUse:"How will the machine be used?", cevOn:"On-site machine hire (excavation, demolition or loading at the worksite)", cevLow:"Low-bed trailer transport required (chain excavator)",
    serviceAreas:"Areas you serve", addArea:"Add", areaPh:"e.g. Hoshangabad Road", rate:"Indicative rate (optional)", ratePh:"e.g. ₹900 / hour",
    useGps:"Use my work location (GPS)", gpsSet:"Work location saved (approx. 1 km)", gpsFail:"Could not get location. You can continue without it.",
    gpsNote:"Only an approximate location (about 1 km) is stored and it is never shown to customers.",
    flagged:"Hauling sand, bricks, soil or other construction material is not allowed on Kaamo.",
    declTitle:"Your declaration", declAgree:"I make this declaration.", docs:"Documents", rc:"Vehicle RC number",
    insurance:"Insurance valid until (DD/MM/YYYY)", fitness:"Fitness certificate valid until (DD/MM/YYYY)", dateBad:"Enter a future date as DD/MM/YYYY.",
    noticeLang:"Notice language", phoneTitle:"Show my phone number?", phoneLead:"Customers can call you only if you allow it. You can change this any time.",
    phoneToggle:"Show my number to Kaamo customers", phoneConsent:"Done — open directory",
    search:"Search plumber, JCB, borewell…", all:"All", live:"Use my live location", liveOn:"Showing nearest first", liveFail:"Location not available. Allow location access in your browser.",
    away:"km away", verifiedAcc:"Phone verified account", selfDecl:"Self-declared by provider", call:"Call partner", report:"Report",
    noResults:"No one found. Try another category or clear search.", loading:"Loading…",
    precallTitle:"Before you call", precall:"You are speaking directly with the service provider. Kaamo does not verify the person, machine, licence, insurance or skill. Please check these yourself and agree the price before any work begins.",
    precallCheck:"I understand and will check the provider, documents and site safety myself.", callNow:"Call now", cancel:"Cancel",
    signIn:"Please sign in to call.", rateLimited:"You have reached the call limit. Try again later.", unavailable:"This listing is no longer available.", netErr:"Network problem. Try again.",
    reportTitle:"Report this listing", r_stolen_or_illegal:"Stolen or illegal vehicle", r_fake:"Fake or fraudulent listing", r_unsafe_use:"Unsafe or prohibited use", r_wrong_info:"Wrong information", r_other:"Something else",
    details:"Details (optional)", submit:"Submit", reported:"Thank you. We will review this report.",
    account:"Account", yourPhone:"Show my number to customers", lastChange:"Last changed: ", removeListing:"Remove my listing", removeAsk:"Remove your listing? You can register again later.", removed:"Your listing has been removed.",
    signOut:"Sign out", help:"Help & legal", grievance:"Grievance Officer", grievanceSla:"We acknowledge every complaint within 24 hours and aim to resolve it within 15 days.",
    directory:"Directory", myListing:"Your listing", saved:"Saved.", fail:"Something went wrong. Try again.",
    required:"Please fill this in.", mobileBad:"Enter a valid 10-digit mobile number.", tick:"Please tick to continue.",
  },
  hi: {
    tagline:"अपने पास मदद, तुरंत", ask:"आपको क्या काम है?",
    chooseRole:"आप Kaamo कैसे इस्तेमाल करेंगे?", roleCustomer:"मुझे काम करवाना है", roleCustomerSub:"अपने पास प्लंबर, बोरवेल, JCB, मज़दूर खोजें",
    rolePartner:"मैं सेवा देता/देती हूँ", rolePartnerSub:"खुद को या अपनी मशीन को लिस्ट करें और कॉल पाएँ",
    directoryOnly:"Kaamo केवल एक डायरेक्टरी है। हम किसी सेवा देने वाले, मशीन, ऑपरेटर या काम के मालिक नहीं हैं, और न ही उनकी जाँच, बीमा या निगरानी करते हैं।",
    back:"वापस", continue:"आगे बढ़ें", language:"भाषा",
    mobile:"मोबाइल नंबर", sendOtp:"OTP भेजें", enterOtp:"6 अंकों का OTP डालें", verify:"जाँचें और आगे बढ़ें", resend:"नंबर बदलें",
    otpSent:"OTP भेजा गया: +91 ", otpFail:"OTP नहीं भेज पाए। फिर कोशिश करें।", otpBad:"OTP गलत है या समाप्त हो गया।",
    demoNote:"डेमो मोड: कोई भी 6 अंकों का कोड चलेगा।",
    agreeTerms:"मैंने उपयोग की शर्तें और गोपनीयता सूचना पढ़ ली हैं और सहमत हूँ।", terms:"शर्तें", privacy:"गोपनीयता",
    yourName:"आपका नाम", yourArea:"आपका इलाका / शहर (वैकल्पिक)", finish:"पूरा करें",
    homeVillage:"गाँव / इलाका", district:"ज़िला", category:"सेवा की श्रेणी", subs:"आप क्या काम करते हैं?",
    cevUse:"मशीन का उपयोग कैसे होगा?", cevOn:"साइट पर मशीन किराये पर (खुदाई, तोड़फोड़ या लोडिंग)", cevLow:"लो-बेड ट्रेलर से ढुलाई चाहिए (चेन एक्सकेवेटर)",
    serviceAreas:"आप किन इलाकों में सेवा देते हैं", addArea:"जोड़ें", areaPh:"जैसे होशंगाबाद रोड", rate:"अनुमानित दाम (वैकल्पिक)", ratePh:"जैसे ₹900 / घंटा",
    useGps:"मेरी काम की जगह (GPS) लें", gpsSet:"काम की जगह सेव हुई (लगभग 1 किमी)", gpsFail:"लोकेशन नहीं मिली। आप बिना इसके आगे बढ़ सकते हैं।",
    gpsNote:"केवल अनुमानित लोकेशन (लगभग 1 किमी) सेव होती है और ग्राहकों को कभी नहीं दिखती।",
    flagged:"रेत, ईंट, मिट्टी या अन्य निर्माण सामग्री की ढुलाई Kaamo पर मना है।",
    declTitle:"आपकी घोषणा", declAgree:"मैं यह घोषणा करता/करती हूँ।", docs:"दस्तावेज़", rc:"वाहन RC नंबर",
    insurance:"बीमा कब तक वैध (DD/MM/YYYY)", fitness:"फिटनेस सर्टिफिकेट कब तक वैध (DD/MM/YYYY)", dateBad:"भविष्य की तारीख DD/MM/YYYY में डालें।",
    noticeLang:"सूचना की भाषा", phoneTitle:"फ़ोन नंबर दिखाएँ?", phoneLead:"ग्राहक आपको तभी कॉल कर पाएँगे जब आप अनुमति देंगे। इसे कभी भी बदल सकते हैं।",
    phoneToggle:"मेरा नंबर Kaamo के ग्राहकों को दिखाएँ", phoneConsent:"हो गया — डायरेक्टरी खोलें",
    search:"प्लंबर, JCB, बोरवेल खोजें…", all:"सभी", live:"मेरी लाइव लोकेशन लें", liveOn:"सबसे पास वाले पहले", liveFail:"लोकेशन नहीं मिली। ब्राउज़र में लोकेशन की अनुमति दें।",
    away:"किमी दूर", verifiedAcc:"फ़ोन सत्यापित अकाउंट", selfDecl:"सेवा देने वाले की स्वयं-घोषणा", call:"कॉल करें", report:"रिपोर्ट",
    noResults:"कोई नहीं मिला। दूसरी श्रेणी चुनें या खोज साफ़ करें।", loading:"लोड हो रहा है…",
    precallTitle:"कॉल से पहले", precall:"आप सीधे सेवा देने वाले से बात कर रहे हैं। Kaamo किसी व्यक्ति, मशीन, लाइसेंस, बीमा या हुनर की जाँच नहीं करता। काम शुरू करने से पहले इन्हें खुद जाँच लें और दाम पहले तय कर लें।",
    precallCheck:"मैं समझता/समझती हूँ और सेवा देने वाले, दस्तावेज़ व साइट की सुरक्षा खुद जाँचूँगा/जाँचूँगी।", callNow:"अभी कॉल करें", cancel:"रद्द करें",
    signIn:"कॉल करने के लिए साइन इन करें।", rateLimited:"कॉल की सीमा पूरी हो गई। बाद में कोशिश करें।", unavailable:"यह लिस्टिंग अब उपलब्ध नहीं है।", netErr:"नेटवर्क में दिक्कत। फिर कोशिश करें।",
    reportTitle:"इस लिस्टिंग की रिपोर्ट करें", r_stolen_or_illegal:"चोरी या अवैध वाहन", r_fake:"नकली या धोखाधड़ी वाली लिस्टिंग", r_unsafe_use:"असुरक्षित या प्रतिबंधित उपयोग", r_wrong_info:"गलत जानकारी", r_other:"कुछ और",
    details:"विवरण (वैकल्पिक)", submit:"भेजें", reported:"धन्यवाद। हम इस रिपोर्ट की जाँच करेंगे।",
    account:"अकाउंट", yourPhone:"ग्राहकों को मेरा नंबर दिखाएँ", lastChange:"आख़िरी बदलाव: ", removeListing:"मेरी लिस्टिंग हटाएँ", removeAsk:"अपनी लिस्टिंग हटाएँ? आप बाद में फिर रजिस्टर कर सकते हैं।", removed:"आपकी लिस्टिंग हटा दी गई है।",
    signOut:"साइन आउट", help:"सहायता और कानूनी", grievance:"शिकायत अधिकारी", grievanceSla:"हम हर शिकायत की 24 घंटे में पावती देते हैं और 15 दिनों में निपटाने का लक्ष्य रखते हैं।",
    directory:"डायरेक्टरी", myListing:"आपकी लिस्टिंग", saved:"सेव हो गया।", fail:"कुछ गड़बड़ हुई। फिर कोशिश करें।",
    required:"कृपया यह भरें।", mobileBad:"सही 10 अंकों का मोबाइल नंबर डालें।", tick:"आगे बढ़ने के लिए टिक करें।",
  },
};

const DECL = {
  en: {
    intro:"I confirm that the details and documents I submitted are accurate and current.",
    tractor:"I will use and list my tractor only for lawful farm work (tillage, sowing, harvesting, threshing). I will not carry sand, bricks, earth or other construction material for hire through Kaamo.",
    jcb:"My machine has a valid RTO fitness certificate where required, and every operator holds a valid licence.",
    transport:"My vehicle is registered, permitted and insured for goods carriage, and every driver holds a valid licence.",
    borewell:"My borewell machine and operators follow the applicable rules, and I hold any permission required where I work.",
    other:"I work independently and I am responsible for my own work.",
    dir:"I understand Kaamo is only a digital directory. It does not guarantee any machine's fitness or the quality of any work.",
    resp:"I am personally responsible for any accident, fine or challan, and I will keep Kaamo harmless from related claims.",
    valid:"I will keep my documents valid. My listing is hidden automatically when a document expires, and Kaamo may suspend or remove it if information is false or a prohibited use is detected.",
    phone:"We collect your name, mobile number, village or area and service category to create and display your service-partner listing. If you turn on \"Show my number\", your mobile number will be visible to Kaamo customers, who may contact you directly by phone. Kaamo does not arrange or guarantee any resulting hire. You can turn off phone visibility or remove your listing in your account at any time. This stops new Kaamo disclosures but cannot retrieve information already copied by third parties.",
  },
  hi: {
    intro:"मैं पुष्टि करता/करती हूँ कि मेरे द्वारा दिए गए सभी विवरण और दस्तावेज़ सही और वैध हैं।",
    tractor:"मैं अपने ट्रैक्टर का उपयोग और लिस्टिंग केवल खेती के कामों (जुताई, बुआई, कटाई, थ्रेशिंग) के लिए करूँगा/करूँगी। Kaamo के माध्यम से किराये पर रेत, बजरी, ईंट, मिट्टी या अन्य निर्माण सामग्री नहीं ढोऊँगा/ढोऊँगी।",
    jcb:"मेरी मशीन का RTO फिटनेस सर्टिफिकेट (जहाँ आवश्यक हो) वैध है और हर ऑपरेटर के पास वैध लाइसेंस है।",
    transport:"मेरा वाहन माल ढुलाई के लिए पंजीकृत, परमिट-युक्त और बीमा-युक्त है, और हर ड्राइवर के पास वैध लाइसेंस है।",
    borewell:"मेरी बोरवेल मशीन और ऑपरेटर पर लागू नियम लागू हैं और जहाँ अनुमति ज़रूरी है वहाँ मेरे पास वह अनुमति है।",
    other:"मैं स्वतंत्र रूप से काम करता/करती हूँ और अपने काम के लिए स्वयं ज़िम्मेदार हूँ।",
    dir:"मैं समझता/समझती हूँ कि Kaamo केवल एक डिजिटल डायरेक्टरी है। वह किसी मशीन की फिटनेस या काम की गुणवत्ता की गारंटी नहीं देता।",
    resp:"किसी भी हादसे, जुर्माने या चालान के लिए मैं स्वयं ज़िम्मेदार रहूँगा/रहूँगी और Kaamo को संबंधित दावों से मुक्त रखूँगा/रखूँगी।",
    valid:"मैं अपने दस्तावेज़ वैध रखूँगा/रखूँगी। समाप्त हो चुके दस्तावेज़ों पर मेरी लिस्टिंग अपने-आप छिप जाएगी, और जानकारी गलत पाए जाने या प्रतिबंधित उपयोग मिलने पर Kaamo लिस्टिंग निलंबित या हटा सकता है।",
    phone:"हम आपकी सेवा-साझेदार लिस्टिंग बनाने और दिखाने के लिए आपका नाम, मोबाइल नंबर, गाँव/इलाका और सेवा की श्रेणी लेते हैं। यदि आप \"मेरा नंबर दिखाएँ\" चालू करते हैं, तो आपका मोबाइल नंबर Kaamo के ग्राहकों को दिखेगा और वे आपको सीधे फ़ोन कर सकते हैं। Kaamo किसी भी किराये की व्यवस्था नहीं करता और न ही उसकी गारंटी देता है। आप कभी भी अपने अकाउंट में फ़ोन नंबर दिखाना बंद कर सकते हैं या अपनी लिस्टिंग हटा सकते हैं। इससे Kaamo द्वारा आगे का प्रदर्शन रुक जाएगा, लेकिन जो जानकारी तीसरे पक्ष पहले ही कॉपी कर चुके हैं उसे वापस नहीं लिया जा सकता।",
  },
};
const declFor = (cat, lang) => {
  const d = DECL[lang];
  const specific = {tractor:d.tractor, jcb:d.jcb, transport:d.transport, borewell:d.borewell}[cat] || d.other;
  return [d.intro, specific, d.dir, d.resp, d.valid];
};

const PROHIBITED = new Set(["sand","bajri","bajari","mitti","eent","int","brick","bricks","cement","mining","रेत","बजरी","मिट्टी","ईंट"]);
const flagged = s => (s || "").toLowerCase().split(/[^\p{L}\p{M}]+/u).some(w => PROHIBITED.has(w));

/* ---------------- state ---------------- */
const S = {
  lang: store.get("kaamo.lang") || "hi",
  screen: "splash",
  role: store.get("kaamo.role") || null,
  mobile: "", otpStage: false, otpBusy: false, termsOk: false, err: {},
  // registration
  name:"", area:"", village:"", district:"", cat:"plumber", subs:[], cev:"on_site", areas:[], rate:"", lat:null, lng:null, gpsMsg:"",
  rc:"", insurance:"", fitnessDate:"", declOk:false, noticeLang:"hi", phonePublic:false, lastChange:null,
  // directory
  partners:[], loading:false, q:"", filter:"all", userLoc:null, locMsg:"", reloadKey:0,
};
const t = k => T[S.lang][k] ?? k;
const $ = sel => document.querySelector(sel);
const esc = s => String(s ?? "").replace(/[&<>"']/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));

function toast(msg){
  const el = $("#toast"); el.textContent = msg; el.hidden = false;
  clearTimeout(toast.t); toast.t = setTimeout(() => { el.hidden = true; }, 3200);
}

/* ---------------- Supabase client (fetch-based) ---------------- */
const api = {
  session: null,
  load(){ try { this.session = JSON.parse(store.get("kaamo.session") || "null"); } catch(e){ this.session = null; } },
  save(body){
    try {
      const j = typeof body === "string" ? JSON.parse(body) : body;
      if (!j.access_token) return false;
      this.session = { access_token:j.access_token, refresh_token:j.refresh_token };
      store.set("kaamo.session", JSON.stringify(this.session));
      return true;
    } catch(e){ return false; }
  },
  clear(){ this.session = null; store.del("kaamo.session"); },
  get signedIn(){ return !!(this.session && this.session.access_token); },
  async raw(path, body, bearer){
    const headers = { "apikey":CFG.anonKey, "Content-Type":"application/json", "Authorization":"Bearer " + (bearer || CFG.anonKey) };
    try {
      const r = await fetch(CFG.url + path, { method:"POST", headers, body: JSON.stringify(body || {}) });
      const text = await r.text();
      let json = null; try { json = text ? JSON.parse(text) : null; } catch(e){}
      return { ok:r.ok, code:r.status, json };
    } catch(e){ return { ok:false, code:-1, json:null }; }
  },
  auth(p, body){ return this.raw("/auth/v1" + p, body); },
  async refresh(){
    if (!this.session || !this.session.refresh_token) return false;
    const r = await this.raw("/auth/v1/token?grant_type=refresh_token", { refresh_token:this.session.refresh_token });
    return r.ok && this.save(r.json);
  },
  async rpc(fn, args){
    if (!this.signedIn) return { ok:false, code:401, json:null };
    let r = await this.raw("/rest/v1/rpc/" + fn, args, this.session.access_token);
    if (r.code === 401 && await this.refresh()) r = await this.raw("/rest/v1/rpc/" + fn, args, this.session.access_token);
    if (r.code === 401) this.clear();
    return r;
  },
};

/* ---------------- repositories (live + demo) ---------------- */
const DEMO_PARTNERS = [
  {id:"d1", full_name:"Ramesh Yadav", service_category:"jcb", sub_categories:["Excavation","Loading"], village:"Misrod", district:"Bhopal", distance_km:5, masked_phone:"98•••••012", phone_verified:true, indicative_rate:"₹1,100 / hour"},
  {id:"d2", full_name:"Mohan Sahu", service_category:"borewell", sub_categories:["Borewell drilling"], village:"Bairagarh", district:"Bhopal", distance_km:4, masked_phone:"97•••••345", phone_verified:true, indicative_rate:"₹90 / ft"},
  {id:"d3", full_name:"Sunil Verma", service_category:"plumber", sub_categories:["Leak repair","Bathroom fitting"], village:"Arera Colony", district:"Bhopal", distance_km:3, masked_phone:"96•••••678", phone_verified:true, indicative_rate:"₹400 / visit"},
  {id:"d4", full_name:"Imran Khan", service_category:"electrician", sub_categories:["House wiring"], village:"Kolar Road", district:"Bhopal", distance_km:8, masked_phone:"99•••••210", phone_verified:false, indicative_rate:null},
  {id:"d5", full_name:"Bhura Singh", service_category:"tractor", sub_categories:["Trolley","Cultivator"], village:"Berasia", district:"Bhopal", distance_km:32, masked_phone:"94•••••774", phone_verified:true, indicative_rate:"₹700 / hour"},
  {id:"d6", full_name:"Dinesh Kushwaha", service_category:"construction", sub_categories:["Mason (mistri)","Tiles"], village:"Ashoka Garden", district:"Bhopal", distance_km:6, masked_phone:"93•••••905", phone_verified:true, indicative_rate:"₹800 / day"},
  {id:"d7", full_name:"Kailash Transport", service_category:"transport", sub_categories:[], village:"Mandideep", district:"Raisen", distance_km:21, masked_phone:"91•••••560", phone_verified:true, indicative_rate:null},
];
const demoPhone = id => "+91 98765 4" + String(1000 + (id.charCodeAt(1) || 0)).slice(-4) ;

const repo = {
  async sendOtp(m){
    if (DEMO) return true;
    return (await api.auth("/otp", { phone:"+91" + m, create_user:true })).ok;
  },
  async verifyOtp(m, code){
    if (DEMO) { api.session = { access_token:"demo" }; return /^\d{6}$/.test(code); }
    const r = await api.auth("/verify", { type:"sms", phone:"+91" + m, token:code });
    return r.ok && api.save(r.json);
  },
  async list(){
    if (DEMO) {
      let rows = DEMO_PARTNERS.map(p => ({...p}));
      if (!S.userLoc) rows.forEach(p => p.distance_km = null);
      return rows;
    }
    const args = S.userLoc ? { p_lat:S.userLoc.lat, p_lng:S.userLoc.lng } : {};
    const r = await api.rpc("list_partners", args);
    return r.ok && Array.isArray(r.json) ? r.json : [];
  },
  async reveal(id){
    if (DEMO) return { status:"ok", phone:demoPhone(id) };
    const r = await api.rpc("reveal_contact", { p_partner_id:id });
    if (r.code === -1) return { status:"network" };
    if (r.code === 401) return { status:"not_signed_in" };
    if (!r.ok || !r.json) return { status:"network" };
    return r.json;
  },
  async consent(type, action, version){
    if (DEMO) return true;
    return (await api.rpc("record_consent", { p_type:type, p_action:action, p_version:version, p_language:S.lang })).ok;
  },
  async saveCustomer(){
    if (DEMO) return true;
    return (await api.rpc("register_customer", { p_full_name:S.name.trim(), p_location:S.area.trim() })).ok;
  },
  async savePartner(){
    if (DEMO) return true;
    const c = catBy(S.cat);
    return (await api.rpc("register_partner", {
      p_full_name:S.name.trim(), p_village:S.village.trim(), p_district:S.district.trim(), p_category:S.cat,
      p_sub_categories:S.subs, p_service_areas:S.areas, p_phone_public:S.phonePublic, p_notice_language:S.noticeLang,
      p_indicative_rate:S.rate.trim() || null, p_rc_number:c.vehicle ? (S.rc.trim() || null) : null,
      p_insurance_until:c.vehicle ? isoDate(S.insurance) : null, p_fitness_until:c.fitness ? isoDate(S.fitnessDate) : null,
      p_cev_use:S.cat === "jcb" ? S.cev : null, p_latitude:S.lat, p_longitude:S.lng,
    })).ok;
  },
  async setPhone(v){ if (DEMO) return true; return (await api.rpc("set_phone_visibility", { p_visible:v })).ok; },
  async remove(){ if (DEMO) return true; return (await api.rpc("remove_listing", {})).ok; },
  async report(id, reason, details){
    if (DEMO) return true;
    return (await api.rpc("report_listing", { p_partner_id:id, p_reason:reason, p_details:details || null })).ok;
  },
};

/* ---------------- utilities ---------------- */
function parseDate(s){
  const m = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec((s || "").trim());
  if (!m) return null;
  const d = new Date(Date.UTC(+m[3], +m[2] - 1, +m[1]));
  return d.getUTCFullYear() === +m[3] && d.getUTCMonth() === +m[2] - 1 && d.getUTCDate() === +m[1] ? d : null;
}
const isoDate = s => { const d = parseDate(s); return d ? d.toISOString().slice(0,10) : null; };
const futureDate = s => { const d = parseDate(s); return !!d && d.getTime() > Date.now(); };
const validMobile = m => /^[6-9]\d{9}$/.test(m);
function getPosition(){
  return new Promise((res, rej) => {
    if (!navigator.geolocation) return rej(new Error("unsupported"));
    navigator.geolocation.getCurrentPosition(p => res({lat:p.coords.latitude, lng:p.coords.longitude}), rej, {enableHighAccuracy:true, timeout:12000, maximumAge:60000});
  });
}
const round2 = n => Math.round(n * 100) / 100; // ~1 km

const LOGO = `<svg viewBox="0 0 525 781" aria-hidden="true"><defs><linearGradient id="lg" gradientUnits="userSpaceOnUse" x1="0" y1="177" x2="0" y2="958"><stop offset="0" stop-color="#036450"/><stop offset="1" stop-color="#05918A"/></linearGradient></defs><g transform="translate(-377,-177)"><path fill="url(#lg)" d="M377,180H464A88,88 0 0 1 552,268V870A88,88 0 0 1 464,958H377Z M815,177H902V453A88,88 0 0 1 814,541H572V453A88,88 0 0 1 660,365H727V265A88,88 0 0 1 815,177Z M572,653A88,88 0 0 1 660,565H814A88,88 0 0 1 902,653V955H815A88,88 0 0 1 727,867V740H660A88,88 0 0 1 572,653Z"/></g></svg>`;

/* ---------------- navigation ---------------- */
function go(screen){ S.screen = screen; S.err = {}; render(); window.scrollTo(0,0); }
function setLang(l){ S.lang = l; store.set("kaamo.lang", l); document.documentElement.lang = l; render(); }
const langSeg = () => `<div class="seg" role="group" aria-label="${esc(t("language"))}"><button data-lang="en" class="${S.lang==="en"?"on":""}">EN</button><button data-lang="hi" class="${S.lang==="hi"?"on":""}">हिन्दी</button></div>`;
const topbar = (back, right) => `<div class="top">${back ? `<button class="link" data-act="back">← ${esc(t("back"))}</button>` : `<div class="brand">${LOGO}${APP_NAME}</div>`}${right || langSeg()}</div>`;
const field = (id, label, val, opts = {}) => `<div class="field ${S.err[id] ? "err" : ""}"><label for="${id}">${esc(label)}</label><input id="${id}" value="${esc(val)}" ${opts.ph ? `placeholder="${esc(opts.ph)}"` : ""} ${opts.mode ? `inputmode="${opts.mode}"` : ""} ${opts.max ? `maxlength="${opts.max}"` : ""} autocomplete="off">${S.err[id] ? `<div class="errtxt">${esc(S.err[id])}</div>` : ""}</div>`;
const steps = (n, total) => `<div class="steps">${Array.from({length:total}, (_, i) => `<i class="${i < n ? "on" : ""}"></i>`).join("")}</div>`;
const legalLinks = () => ["terms:"+t("terms"), "privacy:"+t("privacy"), "rules:"+(S.lang==="hi"?"नियम":"Rules"), "grievance:"+(S.lang==="hi"?"शिकायत":"Grievance")].map(x => { const [k,l] = x.split(":"); return `<a href="${k}.html?lang=${S.lang}" target="_blank" rel="noopener">${esc(l)}</a>`; }).join(" · ");
const catName = c => S.lang === "hi" ? c.hi : c.en;
const catPill = c => S.lang === "hi" ? c.phi : c.pen;

/* ---------------- screens ---------------- */
const views = {
  splash(){ return `<div class="center"><div class="splash-mark">${LOGO}</div><div class="splash-name">${APP_NAME}</div><p class="muted">${esc(t("tagline"))}</p></div>`; },

  role(){
    return `<div class="screen">${topbar(false)}
      <div class="gap24" style="margin-top:20px"><h1>${esc(t("chooseRole"))}</h1>
      <div class="stack">
        <button class="card" data-role="customer"><h3>${esc(t("roleCustomer"))}</h3><p class="muted small">${esc(t("roleCustomerSub"))}</p></button>
        <button class="card" data-role="partner"><h3>${esc(t("rolePartner"))}</h3><p class="muted small">${esc(t("rolePartnerSub"))}</p></button>
      </div>
      <div class="notice">${esc(t("directoryOnly"))}</div></div></div>`;
  },

  auth(){
    const stage = S.otpStage;
    return `<div class="screen">${topbar(true)}
      <div class="stack" style="margin-top:12px"><h2>${esc(stage ? t("enterOtp") : t("mobile"))}</h2>
      ${stage ? `<p class="muted small">${esc(t("otpSent"))}${esc(S.mobile)}</p>
        <div class="field ${S.err.otp ? "err" : ""}"><input id="otp" inputmode="numeric" maxlength="6" autocomplete="one-time-code" aria-label="OTP" style="letter-spacing:8px;text-align:center;font-size:22px">${S.err.otp ? `<div class="errtxt">${esc(S.err.otp)}</div>` : ""}</div>
        ${DEMO ? `<p class="xs muted">${esc(t("demoNote"))}</p>` : ""}
        <button class="btn" data-act="verify" ${S.otpBusy ? "disabled" : ""}>${esc(t("verify"))}</button>
        <button class="link mut" data-act="changeNum">${esc(t("resend"))}</button>`
      : `<div class="field ${S.err.mobile ? "err" : ""}"><label for="mobile">${esc(t("mobile"))}</label><div class="prefix"><span>+91</span><input id="mobile" value="${esc(S.mobile)}" inputmode="numeric" maxlength="10" autocomplete="tel-national"></div>${S.err.mobile ? `<div class="errtxt">${esc(S.err.mobile)}</div>` : ""}</div>
        <label class="check"><input type="checkbox" id="terms" ${S.termsOk ? "checked" : ""}><span>${esc(t("agreeTerms"))}</span></label>
        ${S.err.terms ? `<div class="errtxt">${esc(S.err.terms)}</div>` : ""}
        <p class="xs">${legalLinks()}</p>
        <button class="btn" data-act="sendOtp" ${S.otpBusy ? "disabled" : ""}>${esc(t("sendOtp"))}</button>
        <div class="notice">${esc(t("directoryOnly"))}</div>`}
      </div></div>`;
  },

  customer(){
    return `<div class="screen">${topbar(true)}<div class="stack" style="margin-top:12px"><h2>${esc(t("yourName"))}</h2>
      ${field("name", t("yourName"), S.name)}${field("area", t("yourArea"), S.area)}
      <button class="btn" data-act="saveCustomer">${esc(t("finish"))}</button></div></div>`;
  },

  partner(){
    const c = catBy(S.cat);
    return `<div class="screen">${topbar(true)}${steps(1,3)}<div class="stack">
      ${field("name", t("yourName"), S.name)}
      ${field("village", t("homeVillage"), S.village)}${field("district", t("district"), S.district)}
      <div class="field"><label>${esc(t("category"))}</label><div class="pills" style="flex-wrap:wrap;overflow:visible">${CATS.map(x => `<button class="pill ${x.code===S.cat?"on":""}" data-cat="${x.code}">${esc(catName(x))}</button>`).join("")}</div></div>
      ${c.subs.length ? `<div><label class="small muted">${esc(t("subs"))}</label>${c.subs.map(s => `<label class="check"><input type="checkbox" data-sub="${esc(s)}" ${S.subs.includes(s) ? "checked" : ""}><span>${esc(s)}</span></label>`).join("")}</div>` : ""}
      ${S.cat === "jcb" ? `<div><label class="small muted">${esc(t("cevUse"))}</label>
        <label class="check"><input type="radio" name="cev" value="on_site" ${S.cev==="on_site"?"checked":""}><span>${esc(t("cevOn"))}</span></label>
        <label class="check"><input type="radio" name="cev" value="low_bed_transport" ${S.cev==="low_bed_transport"?"checked":""}><span>${esc(t("cevLow"))}</span></label></div>` : ""}
      <div class="field ${S.err.areas ? "err" : ""}"><label for="areaIn">${esc(t("serviceAreas"))}</label>
        <div class="row"><input id="areaIn" placeholder="${esc(t("areaPh"))}" style="flex:1;border:1px solid var(--border);border-radius:14px;padding:14px;font:inherit;font-size:16px"><button class="btn sm" data-act="addArea">${esc(t("addArea"))}</button></div>
        ${S.err.areas ? `<div class="errtxt">${esc(S.err.areas)}</div>` : ""}
        <div class="tags" style="margin-top:10px">${S.areas.map((a,i) => `<span class="tag">${esc(a)}<button data-rm="${i}" aria-label="remove">×</button></span>`).join("")}</div></div>
      ${field("rate", t("rate"), S.rate, {ph:t("ratePh")})}
      <div class="card"><button class="btn ghost sm" data-act="gps">${esc(t("useGps"))}</button><p class="xs muted" style="margin-top:8px">${esc(S.gpsMsg || t("gpsNote"))}</p></div>
      <button class="btn" data-act="partnerNext">${esc(t("continue"))}</button></div></div>`;
  },

  declaration(){
    const c = catBy(S.cat);
    const lines = declFor(S.cat, S.lang);
    return `<div class="screen">${topbar(true)}${steps(2,3)}<div class="stack"><h2>${esc(t("declTitle"))}</h2>
      <div class="field"><label>${esc(t("noticeLang"))}</label><div class="seg"><button data-nl="en" class="${S.noticeLang==="en"?"on":""}">English</button><button data-nl="hi" class="${S.noticeLang==="hi"?"on":""}">हिन्दी</button></div></div>
      ${c.vehicle ? `<h3>${esc(t("docs"))}</h3>${field("rc", t("rc"), S.rc)}${field("insurance", t("insurance"), S.insurance, {ph:"DD/MM/YYYY", mode:"numeric", max:10})}${c.fitness ? field("fitnessDate", t("fitness"), S.fitnessDate, {ph:"DD/MM/YYYY", mode:"numeric", max:10}) : ""}` : ""}
      <div class="card gap8">${declFor(S.cat, S.noticeLang).map(l => `<p class="small">• ${esc(l)}</p>`).join("")}</div>
      <label class="check"><input type="checkbox" id="declOk" ${S.declOk ? "checked" : ""}><span>${esc(t("declAgree"))}</span></label>
      ${S.err.decl ? `<div class="errtxt">${esc(S.err.decl)}</div>` : ""}
      ${flagged([...S.subs, ...S.areas, S.rate].join(" ")) ? `<div class="errtxt">${esc(t("flagged"))}</div>` : ""}
      <button class="btn" data-act="declNext">${esc(t("continue"))}</button></div></div>`;
  },

  consent(){
    return `<div class="screen">${topbar(true)}${steps(3,3)}<div class="stack"><h2>${esc(t("phoneTitle"))}</h2><p class="muted">${esc(t("phoneLead"))}</p>
      <div class="card"><div class="spread"><span>${esc(t("phoneToggle"))}</span><label class="switch"><input type="checkbox" id="phonePub" ${S.phonePublic ? "checked" : ""}><i></i></label></div></div>
      <div class="notice small">${esc(DECL[S.noticeLang].phone)}</div>
      <button class="btn" data-act="partnerFinish">${esc(t("phoneConsent"))}</button></div></div>`;
  },

  directory(){
    const f = S.filter;
    let rows = S.partners.filter(p => (f === "all" || p.service_category === f));
    const q = S.q.trim().toLowerCase();
    if (q) rows = rows.filter(p => [p.full_name, p.village, p.district, ...(p.sub_categories || []), (catBy(p.service_category) || {}).en, (catBy(p.service_category) || {}).hi].join(" ").toLowerCase().includes(q));
    if (S.userLoc) rows = rows.slice().sort((a,b) => (a.distance_km ?? 1e9) - (b.distance_km ?? 1e9));
    const cards = rows.map(p => {
      const c = catBy(p.service_category) || {en:p.service_category, hi:p.service_category};
      return `<div class="card dir-card">
        <div class="spread"><div class="grow"><div class="name">${esc(p.full_name)}</div><div class="meta">${esc(catName(c))}${p.sub_categories && p.sub_categories.length ? " · " + esc(p.sub_categories.join(", ")) : ""}</div></div>${p.distance_km != null ? `<span class="chip ok">${p.distance_km} ${esc(t("away"))}</span>` : ""}</div>
        <div class="meta">${esc([p.village, p.district].filter(Boolean).join(", "))}${p.indicative_rate ? " · " + esc(p.indicative_rate) : ""}</div>
        <div class="row wrap"><span class="phone">${esc(p.masked_phone || "")}</span><span class="chip ${p.phone_verified ? "ok" : ""}">${esc(p.phone_verified ? t("verifiedAcc") : t("selfDecl"))}</span></div>
        <div class="row"><button class="btn sm grow" data-call="${esc(p.id)}">${esc(t("call"))}</button><button class="btn ghost sm" data-report="${esc(p.id)}">${esc(t("report"))}</button></div></div>`;
    }).join("");
    return `<div class="screen">${topbar(false, `<div class="row">${langSeg()}<button class="link" data-act="account">${esc(t("account"))}</button></div>`)}
      <div class="stack"><h1>${esc(t("ask"))}</h1>
      <div class="field"><input id="q" type="search" value="${esc(S.q)}" placeholder="${esc(t("search"))}" aria-label="search"></div>
      <div class="pills"><button class="pill ${f==="all"?"on":""}" data-filter="all">${esc(t("all"))}</button>${CATS.map(c => `<button class="pill ${f===c.code?"on":""}" data-filter="${c.code}">${esc(catPill(c))}</button>`).join("")}</div>
      <div class="row wrap"><button class="btn ghost sm" data-act="live">📍 ${esc(t("live"))}</button>${S.locMsg ? `<span class="xs muted">${esc(S.locMsg)}</span>` : ""}</div>
      <div class="notice xs">${esc(t("directoryOnly"))}</div>
      <div class="list">${S.loading ? `<p class="empty">${esc(t("loading"))}</p>` : cards || `<p class="empty">${esc(t("noResults"))}</p>`}</div></div></div>`;
  },

  account(){
    const isPartner = S.role === "partner";
    return `<div class="screen">${topbar(true)}<div class="stack"><h2>${esc(t("account"))}</h2>
      ${isPartner ? `<div class="card"><div class="spread"><span>${esc(t("yourPhone"))}</span><label class="switch"><input type="checkbox" id="phoneAcc" ${S.phonePublic ? "checked" : ""}><i></i></label></div>${S.lastChange ? `<p class="xs muted" style="margin-top:8px">${esc(t("lastChange"))}${esc(new Date(S.lastChange).toLocaleString())}</p>` : ""}</div>
      <button class="btn danger" data-act="remove">${esc(t("removeListing"))}</button>` : ""}
      <div class="card gap8"><h3>${esc(t("help"))}</h3><p class="small">${esc(t("directoryOnly"))}</p>
        <p class="small"><b>${esc(t("grievance"))}:</b> <a href="mailto:${GRIEVANCE_EMAIL}">${GRIEVANCE_EMAIL}</a></p><p class="xs muted">${esc(t("grievanceSla"))}</p><p class="xs">${legalLinks()}</p></div>
      <button class="btn ghost" data-act="signOut">${esc(t("signOut"))}</button></div></div>`;
  },
};

/* ---------------- sheets ---------------- */
function openSheet(html){
  const root = $("#sheet-root");
  root.innerHTML = `<div class="scrim" data-close="1"><div class="sheet" role="dialog" aria-modal="true">${html}</div></div>`;
}
const closeSheet = () => { $("#sheet-root").innerHTML = ""; };

function precallSheet(p){
  openSheet(`<div class="stack"><h2>${esc(t("precallTitle"))}</h2><p>${esc(t("precall"))}</p>
    <label class="check"><input type="checkbox" id="pcOk"><span>${esc(t("precallCheck"))}</span></label>
    <button class="btn" id="pcGo" disabled>${esc(t("callNow"))}</button><button class="btn ghost" data-close="1">${esc(t("cancel"))}</button></div>`);
  const cb = $("#pcOk"), go_ = $("#pcGo");
  cb.onchange = () => { go_.disabled = !cb.checked; };
  go_.onclick = async () => {
    go_.disabled = true;
    const r = await repo.reveal(p.id);
    if (r.status === "ok") {
      openSheet(`<div class="stack" style="text-align:center"><p class="muted">${esc(p.full_name)}</p><div class="bignum">${esc(r.phone)}</div>
        <a class="btn" href="tel:${esc(String(r.phone).replace(/[^+\d]/g, ""))}">${esc(t("callNow"))}</a><button class="btn ghost" data-close="1">${esc(t("cancel"))}</button></div>`);
    } else {
      closeSheet();
      toast(r.status === "rate_limited" ? t("rateLimited") : r.status === "not_signed_in" ? t("signIn") : r.status === "network" ? t("netErr") : t("unavailable"));
      if (r.status === "not_signed_in") go("role");
    }
  };
}

function reportSheet(p){
  const reasons = ["stolen_or_illegal","fake","unsafe_use","wrong_info","other"];
  openSheet(`<div class="stack"><h2>${esc(t("reportTitle"))}</h2><p class="muted small">${esc(p.full_name)}</p>
    ${reasons.map((r,i) => `<label class="check"><input type="radio" name="rr" value="${r}" ${i===0?"checked":""}><span>${esc(t("r_" + r))}</span></label>`).join("")}
    <div class="field"><label for="rd">${esc(t("details"))}</label><textarea id="rd" rows="3" maxlength="1000"></textarea></div>
    <button class="btn" id="rsub">${esc(t("submit"))}</button><button class="btn ghost" data-close="1">${esc(t("cancel"))}</button></div>`);
  $("#rsub").onclick = async () => {
    const reason = (document.querySelector('input[name="rr"]:checked') || {}).value || "other";
    const ok = await repo.report(p.id, reason, $("#rd").value.trim());
    closeSheet(); toast(ok ? t("reported") : (api.signedIn || DEMO ? t("fail") : t("signIn")));
  };
}

/* ---------------- actions ---------------- */
async function loadPartners(){
  S.loading = true; render();
  S.partners = await repo.list();
  S.loading = false; render();
}

async function enterDirectory(){
  go("directory");
  loadPartners();
}

const actions = {
  back(){
    const prev = { auth:"role", customer:"auth", partner:"auth", declaration:"partner", consent:"declaration", account:"directory" }[S.screen];
    if (S.screen === "auth" && S.otpStage) { S.otpStage = false; return render(); }
    go(prev || "role");
  },
  async sendOtp(){
    S.mobile = ($("#mobile").value || "").replace(/\D/g, ""); S.termsOk = $("#terms").checked; S.err = {};
    if (!validMobile(S.mobile)) S.err.mobile = t("mobileBad");
    if (!S.termsOk) S.err.terms = t("tick");
    if (S.err.mobile || S.err.terms) return render();
    S.otpBusy = true; render();
    const ok = await repo.sendOtp(S.mobile);
    S.otpBusy = false;
    if (ok) { S.otpStage = true; S.err = {}; } else S.err.mobile = t("otpFail");
    render();
  },
  changeNum(){ S.otpStage = false; render(); },
  async verify(){
    const code = ($("#otp").value || "").trim();
    S.otpBusy = true; render();
    const ok = await repo.verifyOtp(S.mobile, code);
    S.otpBusy = false;
    if (!ok) { S.err.otp = t("otpBad"); return render(); }
    store.set("kaamo.role", S.role);
    repo.consent("TermsAndPrivacy", "Granted", VER);
    S.otpStage = false;
    go(S.role === "partner" ? "partner" : "customer");
  },
  async saveCustomer(){
    S.name = ($("#name").value || ""); S.area = ($("#area").value || "");
    if (!S.name.trim()) { S.err.name = t("required"); return render(); }
    if (!(await repo.saveCustomer())) return toast(t("fail"));
    enterDirectory();
  },
  addArea(){
    const v = ($("#areaIn").value || "").trim(); syncPartner();
    if (v) { if (flagged(v)) { S.err.areas = t("flagged"); return render(); } S.areas.push(v); S.err = {}; }
    render();
  },
  async gps(){
    syncPartner();
    try {
      const p = await getPosition(); S.lat = round2(p.lat); S.lng = round2(p.lng); S.gpsMsg = t("gpsSet");
    } catch(e){ S.gpsMsg = t("gpsFail"); }
    render();
  },
  partnerNext(){
    syncPartner(); S.err = {};
    if (!S.name.trim()) S.err.name = t("required");
    if (!S.village.trim()) S.err.village = t("required");
    if (!S.district.trim()) S.err.district = t("required");
    if (!S.areas.length) S.err.areas = t("required");
    if (flagged([...S.subs, ...S.areas, S.rate, S.village].join(" "))) S.err.areas = t("flagged");
    if (Object.keys(S.err).length) return render();
    go("declaration");
  },
  declNext(){
    syncDecl(); S.err = {};
    const c = catBy(S.cat);
    if (c.vehicle) {
      if (!S.rc.trim()) S.err.rc = t("required");
      if (!futureDate(S.insurance)) S.err.insurance = t("dateBad");
      if (c.fitness && !futureDate(S.fitnessDate)) S.err.fitnessDate = t("dateBad");
    }
    if (!S.declOk) S.err.decl = t("tick");
    if (Object.keys(S.err).length) return render();
    repo.consent("PartnerDeclaration", "Granted", VER);
    go("consent");
  },
  async partnerFinish(){
    S.phonePublic = $("#phonePub").checked;
    if (!(await repo.savePartner())) return toast(t("fail"));
    if (S.phonePublic) { S.lastChange = new Date().toISOString(); repo.consent("PublicDirectoryPhone", "Granted", VER); }
    enterDirectory();
  },
  async live(){
    try {
      const p = await getPosition(); S.userLoc = { lat:p.lat, lng:p.lng }; S.locMsg = t("liveOn");
      await loadPartners();
    } catch(e){ S.locMsg = t("liveFail"); render(); }
  },
  account(){ go("account"); },
  async remove(){
    if (!confirm(t("removeAsk"))) return;
    if (!(await repo.remove())) return toast(t("fail"));
    S.phonePublic = false; S.lastChange = new Date().toISOString();
    repo.consent("PublicDirectoryPhone", "Withdrawn", VER);
    toast(t("removed")); go("directory"); loadPartners();
  },
  signOut(){ api.clear(); store.del("kaamo.role"); S.role = null; S.partners = []; go("role"); },
};

function syncPartner(){
  const v = id => { const e = document.getElementById(id); return e ? e.value : null; };
  const set = (k, id) => { const x = v(id); if (x !== null) S[k] = x; };
  set("name","name"); set("village","village"); set("district","district"); set("rate","rate");
}
function syncDecl(){
  const v = id => { const e = document.getElementById(id); return e ? e.value.trim() : null; };
  const r = v("rc"); if (r !== null) S.rc = r;
  const i = v("insurance"); if (i !== null) S.insurance = i;
  const f = v("fitnessDate"); if (f !== null) S.fitnessDate = f;
  const d = document.getElementById("declOk"); if (d) S.declOk = d.checked;
}

/* ---------------- render + events ---------------- */
function render(){
  const keep = document.activeElement && document.activeElement.id;
  const caret = keep && document.activeElement.selectionStart;
  $("#app").innerHTML = views[S.screen]();
  document.title = APP_NAME;
  if (keep) { const e = document.getElementById(keep); if (e) { e.focus(); try { if (caret != null) e.setSelectionRange(caret, caret); } catch(_){} } }
}

document.addEventListener("click", e => {
  const el = e.target.closest("[data-lang],[data-act],[data-role],[data-cat],[data-rm],[data-filter],[data-call],[data-report],[data-nl],[data-close]");
  if (!el) return;
  if (el.dataset.close && (el.classList.contains("scrim") ? e.target === el : true)) { if (el.dataset.close) return closeSheet(); }
  if (el.dataset.lang) return setLang(el.dataset.lang);
  if (el.dataset.act) return actions[el.dataset.act] && actions[el.dataset.act]();
  if (el.dataset.role) { S.role = el.dataset.role; S.otpStage = false; return go("auth"); }
  if (el.dataset.cat) { syncPartner(); S.cat = el.dataset.cat; S.subs = []; return render(); }
  if (el.dataset.rm) { syncPartner(); S.areas.splice(+el.dataset.rm, 1); return render(); }
  if (el.dataset.filter) { S.filter = el.dataset.filter; return render(); }
  if (el.dataset.nl) { syncDecl(); S.noticeLang = el.dataset.nl; return render(); }
  const p = S.partners.find(x => x.id === (el.dataset.call || el.dataset.report));
  if (p && el.dataset.call) return precallSheet(p);
  if (p && el.dataset.report) return reportSheet(p);
});

document.addEventListener("change", async e => {
  const el = e.target;
  if (el.dataset && el.dataset.sub) { syncPartner(); const s = el.dataset.sub; S.subs = el.checked ? [...S.subs, s] : S.subs.filter(x => x !== s); return; }
  if (el.name === "cev") { S.cev = el.value; return; }
  if (el.id === "phoneAcc") {
    const v = el.checked;
    if (await repo.setPhone(v)) { S.phonePublic = v; S.lastChange = new Date().toISOString(); repo.consent("PublicDirectoryPhone", v ? "Granted" : "Withdrawn", VER); toast(t("saved")); render(); }
    else { el.checked = !v; toast(t("fail")); }
  }
});

document.addEventListener("input", e => {
  if (e.target.id === "q") { S.q = e.target.value; render(); }
});

/* ---------------- boot ---------------- */
api.load();
document.documentElement.lang = S.lang;
render();
setTimeout(() => {
  if (api.signedIn && S.role && !DEMO) { enterDirectory(); } else go("role");
}, 1200);
})();
