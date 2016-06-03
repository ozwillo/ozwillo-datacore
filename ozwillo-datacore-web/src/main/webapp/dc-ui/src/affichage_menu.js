$(document).ready(function() {
    var idTimeout;
    //it 's easier to work with a local variable than checking with jquery
    var hidden = true;
    var showDuration = 2000;
    $(".menubutton").hover(function() {
        if (hidden) {
            $('.ui.mymenu').transition('fade right');
            hidden = false;
        }
    }, function() {});

    $(".groupementmenu").hover(function() {
        clearTimeout(idTimeout);
    }, function() {
        clearTimeout(idTimeout);
        if (!hidden) {
            idTimeout = window.setTimeout(function() {
                $('.ui.mymenu ').transition('fade right');
                hidden = true;
            }, showDuration);
        }
    });
});
