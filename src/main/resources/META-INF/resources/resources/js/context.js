(function () {
    const Constructor = function () {
        const resources = {};

        const init = async function () {
            console.debug('init context');
            resources.user = await $.cms.api.getUser();
            resources.properties = await $.cms.api.getProperties();
            resources.models = await $.cms.api.getModels();
            resources.userPermissions = resources.user.rolesRelation ? resources.user.rolesRelation.map(role => role.permissions) : [];
            resources.user.permissions ? resources.userPermissions.push(resources.user.permissions) : null;
            if (resources.userPermissions.includes('.*:.*:.*')) {
                $.cms.log.warn('you are a super user, do not use .*:.*:.* in prod');
            }
            console.log('done init');
        }

        return {
            init: init,
            resources: resources,
        }
    }

    !$.cms ? $.cms = {} : null;

    jQuery.extend($.cms, {
        context: Constructor()
    });
})();
