(function () {
    const Constructor = function () {
        // only html dependencies, safe to add here
        let snackbarE;
        const init = function () {
            console.debug('init log');
            let rootE = $('#cms-log');
            snackbarE = $('<div>').addClass('mdl-js-snackbar mdl-snackbar')
                .mdl()
                .appendTo(rootE);
            $('<div>').addClass('mdl-snackbar__text').appendTo(snackbarE);
            $('<button>').addClass('mdl-snackbar__action').appendTo(snackbarE);
        }

        const info = function(message) {
            snackbarE[0].MaterialSnackbar.showSnackbar({"message": message});
            console.log(message);
        }

        const warn = function(message) {
            snackbarE[0].MaterialSnackbar.showSnackbar({"message": message});
            console.warn(message);
        }

        const error = function(message) {
            snackbarE[0].MaterialSnackbar.showSnackbar({"message": message});
            console.error(message);
            alert(message);
        }


        return {
            init: init,
            info: info,
            warn: warn,
            error: error
        }
    }

    !$.cms ? $.cms = {} : null;

    jQuery.extend($.cms, {
        log: Constructor()
    });
})();
