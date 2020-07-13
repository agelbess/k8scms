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

        // action = { label: "the button name", action: the_function }
        const create = function (title, contentE, actions) {
            let dialog;

            let dialogE = $('<dialog>').addClass('mdl-dialog');
            $('<h3>').addClass('mdl-dialog__title').text(title).appendTo(dialogE);
            contentE.addClass('mdl-dialog__content').appendTo(dialogE);
            let actionsE = $('<div>').addClass('mdl-dialog__actions').appendTo(dialogE);

            const addAction = function (action) {
                let buttonE = $('<button type="button">')
                    .addClass('mdl-button')
                    .text(action.label)
                    .on('click', action.action)
                    .appendTo(actionsE);
                if (action.keyup) {
                    dialogE.keyup((e) => {
                        let code = e.key; // recommended to use e.key, it's normalized across devices and languages
                        if (code === action.keyup.key) {
                            action.action();
                        }
                    });
                }
            }

            const show = function () {
                $('#cms-dialog').empty().append(dialogE);
                dialogPolyfill.registerDialog(dialogE[0]);
                dialogE[0].showModal();
            }

            const close = function () {
                dialogE[0].close();
            }

            addAction({
                label: 'close',
                action: close
            });

            for (let action of actions) {
                addAction(action);
            }

            return {
                show: show,
                close: close
            };
        }

        /*
        let data = {
            message: 'Button color changed.',
            timeout: 2000,
            actionHandler: handler,
            actionText: 'Undo'
        };
         */
        const confirm = function (data) {
            $('.mdl-snackbar')[0].MaterialSnackbar.showSnackbar(data);
        }

        return {
            create: create,
            confirm: confirm
        }
    }

    !$.cms ? $.cms = {} : null;

    jQuery.extend($.cms, {
        dialog: Constructor()
    });
})();
