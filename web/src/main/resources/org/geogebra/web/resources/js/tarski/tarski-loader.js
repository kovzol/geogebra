Tarski().then(function(Module) {
 var numcells = 50000000;
 var timeout = 5;
 TARSKIINIT = Module.cwrap("TARSKIINIT", 'void', ['number', 'number']);
 TARSKIEVAL = Module.cwrap("TARSKIEVAL", "string", ["string"]);
 TARSKIEND = Module.cwrap("TARSKIEND", "void", []);
 TARSKIINIT(numcells, timeout);
});
