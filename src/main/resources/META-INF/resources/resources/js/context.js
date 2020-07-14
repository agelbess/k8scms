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
        const resources = {};

        const init = async function () {
            console.debug('init context');
            resources.user = await $.cms.api.getUser();
            resources.properties = await $.cms.api.getProperties();
            resources.models = await $.cms.api.getModels();
            resources.userPermissions = resources.user.rolesRelation ? resources.user.rolesRelation.map(role => role.permissions) : [];
            resources.user.permissions ? resources.userPermissions = resources.userPermissions.concat(resources.user.permissions) : null;
            if (resources.userPermissions.filter(p => p.cluster === '.*' && p.database === '.*' && p.collection === '.*' && p.method === '.*').length > 0) {
                $.cms.log.warn('you are a super user, do not use .*:.*:.*:.* in prod');
            }
            $('#cms-title').text(resources.properties.projectName + ' throfilou');
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
