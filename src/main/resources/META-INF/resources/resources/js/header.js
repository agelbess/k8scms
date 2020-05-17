/*
 * MIT License
 *
 * Copyright (c) 2020 Alexandros Gelbessis
 *
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
 *
 */

(function () {
    const Constructor = function () {

        let navE;
        let drawerE;

        const logout = async function () {
            await $.cms.api.logout();
            $(window.location.reload());
        }

        const init = function () {
            console.debug('init header');
            let properties = $.cms.context.resources.properties
            {
                let headerE = $('#cms-header-header');
                let rowE = $('<div>').addClass('mdl-layout__header-row').appendTo(headerE);
                $('<img id="cms-header-header-logo" src="resources/image/favicon.ico">').addClass('mdl-layout-title').appendTo(rowE);
                $('<span id="cms-header-header-title-fixed">K8sCMS</span>').addClass('mdl-layout-title').appendTo(rowE);
                $('<span id="cms-header-header-title"></span>').addClass('mdl-layout-title').appendTo(rowE);
                $('<div>').addClass('mdl-layout-spacer').appendTo(rowE);
                navE = $('<nav>').addClass('mdl-navigation mdl-layout--large-screen-only').appendTo(rowE);
                navE.append(createLink('home', '#INDEX'));
                navE.append(createLink('upload', '#UPLOAD'));
                navE.append($('<span>').text($.cms.context.resources.user.name));
                navE.append(createLink('logout', '#', logout));
            }

            {
                let drawerContainerE = $('#cms-header-drawer');
                drawerE = $('<nav>').addClass('mdl-navigation').appendTo(drawerContainerE);
                appendDrawer('logout', '#', logout);
                $('<h4>')
                    .text('pages')
                    .addClass('cms-header-drawer-title')
                    .appendTo(drawerE);
                appendDrawer('home', '#INDEX');
                appendDrawer('upload', '#UPLOAD');
                $('<hr>').appendTo(drawerE);
                $('<h4>')
                    .text('collections')
                    .addClass('cms-header-drawer-title')
                    .appendTo(drawerE);
            }
        }

        const createLink = function (title, href, action) {
            let linkE = $('<a>').addClass('mdl-navigation__link').attr('href', href).append(title);
            if (action) {
                linkE.on('click', action);
            }
            return linkE;
        }

        const appendDrawer = function(title, href, action) {
            drawerE.append(createLink(title, href, action));
        }

        return {
            init: init,
            appendDrawer: appendDrawer,
        }
    }

    !$.cms ? $.cms = {} : null;

    jQuery.extend($.cms, {
        header: Constructor()
    });
})();
