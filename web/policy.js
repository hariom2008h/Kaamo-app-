(function(){
  var l="hi"; try{l=localStorage.getItem("kaamo.lang")||"hi";}catch(e){}
  if(/[?&]lang=en/.test(location.search))l="en"; if(/[?&]lang=hi/.test(location.search))l="hi";
  function set(x){l=x;document.body.className="lang-"+x;document.documentElement.lang=x;try{localStorage.setItem("kaamo.lang",x);}catch(e){}
    document.querySelectorAll("[data-l]").forEach(function(b){b.classList.toggle("on",b.dataset.l===x);});}
  document.querySelectorAll("[data-l]").forEach(function(b){b.onclick=function(){set(b.dataset.l);};});
  set(l);
})();
