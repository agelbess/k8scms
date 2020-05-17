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
