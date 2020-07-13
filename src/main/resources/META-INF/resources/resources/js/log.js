/*
 * MIT License
 * Copyright (c) 2020 Alexandros Gelbessis
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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

        const info = function (message) {
            snackbarE[0].MaterialSnackbar.showSnackbar({"message": message});
            console.log(message);
        }

        const warn = function (message) {
            snackbarE[0].MaterialSnackbar.showSnackbar({"message": message});
            console.warn(message);
        }

        const error = function (message) {
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
