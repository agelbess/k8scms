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

        const ajaxError = function (jqXHR, textStatus, errorThrown) {
            let message = errorThrown + (jqXHR.responseText ? `: ${jqXHR.responseText}` : '');
            console.error(message);
            alert(message);
            if (jqXHR.status === 401) {
                $(window.location.reload());
            }
            $.cms.utils.loading(true);
        }

        const sleep = function (ms) {
            return new Promise(resolve => setTimeout(resolve, ms));
        }

        const getIdFields = function (model) {
            return model.fields.filter((c) => c.id);
        }

        const findFieldByName = function (fields, name) {
            return fields.filter(c => c.name === name).pop();
        }

        const getFieldIcon = function (field) {
            let iconE;
            switch (field.type) {
                case 'oid':
                    iconE = $('<span>').addClass('material-icons').text('perm_identity');
                    break;
                case 'date':
                    iconE = $('<span>').addClass('material-icons').text('calendar_today');
                    break;
                case 'integer':
                    iconE = $('<span>').addClass('material-icons').text('exposure_plus_1');
                    break;
                case 'decimal':
                    iconE = $('<span>').addClass('material-icons').text('exposure_plus_2');
                    break;
                case 'boolean':
                    iconE = $('<span>').addClass('material-icons').text('check');
                    break;
                case 'json':
                    iconE = $('<span>').addClass('material-icons').text('code');
                    break;
                case 'email':
                    iconE = $('<span>').addClass('material-icons').text('email');
                    break;
                case 'phone':
                    iconE = $('<span>').addClass('material-icons').text('phone');
                    break;
                case 'secret1':
                    iconE = $('<span>').addClass('material-icons').text('enhanced_encryption');
                    break;
                case 'secret2':
                    iconE = $('<span>').addClass('material-icons').text('enhanced_encryption');
                    break;
                case 'cron':
                    iconE = $('<span>').addClass('material-icons').text('access_alarm');
                    break;
                case 'geoJson':
                    iconE = $('<span>').addClass('material-icons').text('location_on');
                    break;
                default:
                    iconE = $('<span>').addClass('material-icons').text('text_format');
            }
            let spanE = $('<span>').append(iconE);
            if (field.regex) {
                spanE.append($('<span>').addClass('material-icons').text('spellcheck'))
            }
            if (field.relation) {
                spanE.append($('<span>').addClass('material-icons').text('extension'))
            }
            return spanE;
        }
        // reads cookies
        const getCookie = function (name) {
            var c = document.cookie.split('; ');
            var cookies = {};
            for (var i = c.length - 1; i >= 0; i--) {
                var C = c[i].split('=');
                cookies[C[0]] = C[1];
            }
            return cookies[name];
        }

        // writes cookies
        const setCookie = function (name, value, path, expires) {
            var date = new Date();
            date.setTime(date.getTime() + expires);
            document.cookie = name + '=' + value + ';expires='
                + date.toUTCString() + ';path=' + path;
        }

        const equal = function (o1, o2) {
            if (o1 instanceof Object && o2 instanceof Object) {
                if (Object.keys(o1).length !== Object.keys(o2).length) {
                    return false;
                }
                for (p in o1) {
                    if (!equal(o1[p], o2[p])) {
                        return false;
                    }
                }
            } else if (o1 instanceof Array && o2 instanceof Array) {
                for (let i = 0; i < o1.length; i++) {
                    if (!equal(o1[i])) {
                        return false;
                    }
                }
            } else {
                if (o1 !== o2) {
                    return false;
                }
            }
            return true;
        }

        const getHashParams = function () {
            let params = {};
            let hash = window.location.hash.split('#')[1];
            if (hash) {
                hash.split('&')
                    .map(s => s.split("="))
                    .forEach(a => {
                        params[a[0]] = a[1];
                    });
            }
            return params;
        }

        const getHashParam = function (name) {
            return getHashParams()[name];
        }

        const evalHash = function (map) {
            let hash = '';
            for (let k in map) {
                hash += (hash ? '&' : '') + (map[k] !== undefined ? k + '=' + map[k] : k);
            }
            return hash;
        }

        const setHashParam = function (key, value) {
            let params = getHashParams();
            params[key] = value;
            window.location.hash = evalHash(params);
        }

        const setHashParams = function (params) {
            window.location.hash = evalHash(params);
        }

        const removeHashParam = function (key) {
            let params = getHashParams();
            delete params[key];
            window.location.hash = evalHash(params);
        }

        function dynamicJson(key, json, expand, comma) {
            let jsonE = $('<div>').addClass('cms-json');
            if (key !== null) {
                jsonE.append($('<span>').addClass('cms-json-key').text(`"${key}": `))
            }
            let copyToClipboardE = $('<span>').addClass('material-icons cms-json-copy').text('file_copy')
                .on('click', () => {
                    copyToClipboard(JSON.stringify(json, undefined, 2));
                    $.cms.log.info('json copied to clipboard');
                });
            if (Array.isArray(json)) {
                jsonE.append($('<span>').text('['));
                jsonE.append(copyToClipboardE);
                if (!expand) {
                    let moreE = $('<span>').addClass('material-icons').text('expand_more')
                        .add($('<span>').text(json.length))
                        .addClass('cms-json-expand')
                        .on('click', () => jsonE.replaceWith(dynamicJson(key, json, 100)))
                        .appendTo(jsonE);
                } else {
                    let lessE = $('<span>').addClass('material-icons').text('expand_less')
                        .add($('<span>').text(json.length))
                        .addClass('cms-json-expand')
                        .on('click', () => jsonE.replaceWith(dynamicJson(key, json, 0)))
                        .appendTo(jsonE);
                    for (let i in json) {
                        let value = json[i];
                        if (value !== null) {
                            let containerE = $('<div>').appendTo(jsonE);
                            let valueE = dynamicJson(null, value, expand - 1, i < json.length - 1).appendTo(containerE);
                        }
                    }
                }
                jsonE.append($('<span>').text(']'));
            } else if (json instanceof Object) {
                jsonE.append($('<span>').text('{'));
                jsonE.append(copyToClipboardE);
                if (!expand) {
                    let moreE = $('<span>').addClass('material-icons').text('expand_more')
                        .add($('<span>').text(Object.keys(json).length).appendTo(jsonE))
                        .addClass('cms-json-expand')
                        .on('click', () => jsonE.replaceWith(dynamicJson(key, json, 100)))
                        .appendTo(jsonE);
                } else {
                    let lessE = $('<span>').addClass('material-icons').text('expand_less')
                        .add($('<span>').text(Object.keys(json).length).appendTo(jsonE))
                        .addClass('cms-json-expand')
                        .on('click', () => jsonE.replaceWith(dynamicJson(key, json, 0)))
                        .appendTo(jsonE);
                    let keys = Object.keys(json);
                    for (let i = 0; i < keys.length; i++) {
                        let key = keys[i];
                        let containerE = $('<div>').appendTo(jsonE);
                        if (json[key] !== undefined) {
                            let valueE = dynamicJson(key, json[key], expand - 1, i < keys.length - 1).appendTo(containerE);
                        }
                    }
                }
                jsonE.append($('<span>').text('}'));
            } else if (typeof json === 'string') {
                jsonE.append($('<span>').addClass('cms-json-string').text(`"${json}"`));
            } else if (typeof json === 'number') {
                jsonE.append($('<span>').addClass('cms-json-number').text(json));
            } else if (typeof json === 'boolean') {
                jsonE.append($('<span>').addClass('cms-json-boolean').text(json));
            }
            if (comma) {
                jsonE.append($('<span>').text(','));
            }
            return jsonE;
        }

        let loadingId;
        const loading = function (done) {
            clearTimeout(loadingId);
            if (done) {
                $('#cms-loading').hide();
            } else {
                loadingId = setTimeout(() => {
                    $('#cms-loading').show();
                }, 100)
            }
        }

        const copyToClipboard = function (text) {
            const el = document.createElement('textarea');
            el.value = text;
            document.body.appendChild(el);
            el.select();
            document.execCommand('copy');
            document.body.removeChild(el);
        }

        const filterObject = function (o, keys) {
            let result = {};
            for (let key of keys) {
                if (o[key] !== undefined) {
                    result[key] = o[key];
                }
            }
            return result;
        }

        const replaceAll = function(target, replaceS, withS) {
            while (target.includes(replaceS)) {
                target = target.replace(replaceS, withS);
            }
            return target;
        }

        const mdl = function () {
            setTimeout(() => componentHandler.upgradeElement($(this)[0]));
            return $(this);
        }
        // use it when changing a value of a textfield. e.g. element.mdlNot().mdl()
        const mdlNot = function () {
            componentHandler.downgradeElements($(this)[0]);
            return $(this);
        }
        // register in $
        $.fn.mdl = mdl;
        $.fn.mdlNot = mdlNot;

        return {
            ajaxError: ajaxError,
            sleep: sleep,
            getIdFields: getIdFields,
            findFieldByName: findFieldByName,
            getFieldIcon: getFieldIcon,
            getCookie: getCookie,
            setCookie: setCookie,
            equal: equal,
            getHashParams: getHashParams,
            evalHash: evalHash,
            getHashParam: getHashParam,
            setHashParam: setHashParam,
            setHashParams: setHashParams,
            removeHashParam: removeHashParam,
            dynamicJson: dynamicJson,
            loading: loading,
            copyToClipboard: copyToClipboard,
            filterObject: filterObject,
            replaceAll: replaceAll,
            mdl: mdl,
            mdlNot: mdlNot
        }
    }

    !$.cms ? $.cms = {} : null;

    jQuery.extend($.cms, {
        utils: Constructor()
    });
})();
