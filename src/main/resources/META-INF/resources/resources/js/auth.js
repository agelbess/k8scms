(function () {
    const Constructor = function () {

        const canAccess = function (database, collection, method) {
            let permissions = $.cms.context.resources.userPermissions;
            for (let permission of permissions) {
                let tokens = permission.split(':');
                if (
                    (!database || database.match(tokens[0])) &&
                    (!collection || collection.match(tokens[1])) &&
                    (!method || method.match(tokens[2]))) {
                    return true;
                }
            }
        }

        const hide = function (eE, database, collection, method) {
            if (!canAccess(database, collection, method)) {
                eE.hide();
            }
        }

        const remove = function (eE, database, collection, method) {
            if (!canAccess(database, collection, method)) {
                eE.remove();
            }
        }

        const ca = function (database, collection, method) {
            if (typeof database === 'string') {
                if (!canAccess(database, collection, method)) {
                    $(this).hide();
                }
            } else {
                // shift the arguments
                let model = database;
                let method = collection;
                if (!canAccess(model.database, model.collection, method)) {
                    $(this).hide();
                }
            }
            return $(this);
        }

        // register in $
        // can also be invoked as (model, method)
        $.fn.ca = ca;

        return {
            canAccess: canAccess
        }
    }

    !$.cms ? $.cms = {} : null;

    jQuery.extend($.cms, {
        auth: Constructor()
    });
})();
