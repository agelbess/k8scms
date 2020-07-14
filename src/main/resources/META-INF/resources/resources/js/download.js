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

        let xlsx = function (data, model) {
            const filename = `${model.cluster}_${model.database}_${model.collection}.xlsx`;
            const sheetName = model.collection;
            let workbook = {
                SheetNames: [],
                Sheets: {},
            };
            let worksheet = {};
            let range = {
                s: {
                    c: 0,
                    r: 0
                },
                e: {
                    c: model.fields.length - 1,
                    r: data.length
                }
            };

            let filteredFields = model.fields.filter(f => !f.relation && !f.virtual);
            for (let ir = 0; ir < data.length; ir++) {
                for (let ic = 0; ic < filteredFields.length; ic++) {
                    let f = filteredFields[ic];
                    let v = data[ir][f.name];
                    if (ir === 0) {
                        let cell = {
                            t: 's',
                            v: f.name,
                        };
                        let cell_ref = XLSX.utils.encode_cell({
                            c: ic,
                            r: ir
                        });
                        worksheet[cell_ref] = cell;
                    }
                    let t;
                    if (v !== null && v !== undefined) {
                        if (f.type) {
                            // cell type
                            switch (f.type) {
                                case null:
                                case undefined:
                                case 'string':
                                case 'oid':
                                case 'date':
                                case 'boolean':
                                case 'email':
                                case 'phone':
                                case 'cron':
                                case 'geoJson':
                                case 'secret2':
                                    // no type
                                    // not change
                                    break;
                                case 'json':
                                case 'array':
                                    t = 's';
                                    if (v instanceof Object) {
                                        v = JSON.stringify(v);
                                    }
                                    break;
                                case 'integer':
                                case 'decimal':
                                    if (!isNaN(v)) {
                                        t = 'n';
                                    }
                                    // no change
                                    break;
                                default:
                                    $.cms.log.error(`invalid field type '${f.type}'`);
                            }
                        } else if (f.encryption === 'secret1') {
                            v = undefined;
                        } else {
                            // no type
                            // no change to data
                        }

                        let cell = {
                            v: v,
                            t: t
                        };
                        let cell_ref = XLSX.utils.encode_cell({
                            c: ic,
                            r: ir + 1
                            // the first is the header row
                        });
                        worksheet[cell_ref] = cell;
                    }
                }
            }
            worksheet['!ref'] = XLSX.utils.encode_range(range);
            workbook.SheetNames.push(sheetName);
            workbook.Sheets[sheetName] = worksheet;
            let wopts = {
                bookType: 'xlsx',
                bookSST: false,
                type: 'binary',
                // https://github.com/SheetJS/js-xlsx/issues/126
                // cellDates : true,
            };
            let wbout = XLSX.write(workbook, wopts);
            let s2ab = function (s) {
                let buf = new ArrayBuffer(s.length);
                let view = new Uint8Array(buf);
                for (let i = 0; i != s.length; ++i)
                    view[i] = s.charCodeAt(i) & 0xFF;
                return buf;
            }
            // the saveAs call downloads a file on the local machine
            downloadBlob(new Blob([s2ab(wbout)], {
                type: 'application/octet-stream',
                // charset : '777'
            }), filename);
        }
        let json = function (data, model) {
            data.forEach(d => delete d._meta);
            const filename = `${model.cluster}_${model.database}_${model.collection}.json`;
            downloadBlob(new Blob([JSON.stringify(data, undefined, 2)], {
                type: 'application/octet-stream',
                // charset : '777'
            }), filename);

        }

        let downloadBlob = function (blob, fileName) {
            var a = window.document.createElement('a');
            a.href = window.URL.createObjectURL(blob);
            a.download = fileName;
            // Append anchor to body.
            document.body.appendChild(a);
            a.click();
            // Remove anchor from body
            document.body.removeChild(a);
        }

        return {
            xlsx: xlsx,
            json: json
        }
    }

    !$.cms ? $.cms = {} : null;

    jQuery.extend($.cms, {
        download: Constructor()
    });
})();
