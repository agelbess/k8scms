(function () {
    const Constructor = function () {

        const start = function () {
            console.debug('start index');
        }

        return {
            start: start
        }
    }

    !$.cms ? $.cms = {} : null;

    jQuery.extend($.cms, {
        index: Constructor()
    });
})();
