// Mirrors the default row seeded in supabase/migrations/003. Used when the backend is not configured (demo)
// or the config cannot be fetched.
window.KAAMO_DEFAULT_CFG = {
  categories:[
    {code:"plumber",label_en:"Plumber",label_hi:"प्लंबर",enabled:true,vehicle:false,fitness:false,cev:false,subs:[{en:"Pipe fitting",hi:"पाइप फिटिंग"},{en:"Leak repair",hi:"लीकेज मरम्मत"},{en:"Bathroom fitting",hi:"बाथरूम फिटिंग"},{en:"Water tank",hi:"पानी की टंकी"}]},
    {code:"electrician",label_en:"Electrician",label_hi:"इलेक्ट्रीशियन",enabled:true,vehicle:false,fitness:false,cev:false,subs:[{en:"House wiring",hi:"घर की वायरिंग"},{en:"Fan / light repair",hi:"पंखा / लाइट मरम्मत"},{en:"Inverter / UPS",hi:"इन्वर्टर / UPS"}]},
    {code:"construction",label_en:"Construction workers",label_hi:"निर्माण मज़दूर",enabled:true,vehicle:false,fitness:false,cev:false,subs:[{en:"Mason (mistri)",hi:"राजमिस्त्री"},{en:"Labour",hi:"मज़दूर"},{en:"Painter",hi:"पेंटर"},{en:"Carpenter",hi:"बढ़ई"},{en:"Tiles",hi:"टाइल्स"}]},
    {code:"borewell",label_en:"Borewell",label_hi:"बोरवेल",enabled:true,vehicle:true,fitness:true,cev:false,subs:[{en:"Borewell drilling",hi:"बोरवेल ड्रिलिंग"},{en:"Motor / pump repair",hi:"मोटर / पंप मरम्मत"},{en:"Flushing",hi:"फ्लशिंग"}]},
    {code:"jcb",label_en:"JCB / Earthmover",label_hi:"JCB / अर्थमूवर",enabled:true,vehicle:true,fitness:true,cev:true,subs:[{en:"Excavation",hi:"खुदाई"},{en:"Loading",hi:"लोडिंग"},{en:"Demolition",hi:"तोड़फोड़"},{en:"Leveling",hi:"समतलीकरण"}]},
    {code:"tractor",label_en:"Tractor",label_hi:"ट्रैक्टर",enabled:true,vehicle:true,fitness:false,cev:false,subs:[{en:"Trolley",hi:"ट्रॉली"},{en:"Cultivator",hi:"कल्टीवेटर"},{en:"Water Tanker",hi:"पानी का टैंकर"},{en:"Thresher",hi:"थ्रेशर"}]},
    {code:"transport",label_en:"Transport / Trailer",label_hi:"ट्रांसपोर्ट / ट्रेलर",enabled:true,vehicle:true,fitness:true,cev:false,subs:[]},
  ],
  fields:{village:{enabled:true,required:true},district:{enabled:true,required:true},areas:{enabled:true,required:true},rate:{enabled:true,required:false},gps:{enabled:true,required:false},rc:{enabled:true,required:true},insurance:{enabled:true,required:true},fitness:{enabled:true,required:true}},
  custom:[], policy:{version:"v1.0_2026",summary_en:"",summary_hi:""}, registration:{invite_only:false},
};
