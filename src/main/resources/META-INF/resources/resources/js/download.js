(function () {
    const Constructor = function () {

        let xlsx = function (data, model) {
            const filename = `${model.database}_${model.collection}.xlsx`;
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
                    c: model.fields.length,
                    r: data.length
                }
            };

            for (let ir = 0; ir < data.length; ir++) {
                for (let ic = 0; ic < model.fields.length; ic++) {
                    let c = model.fields[ic];
                    let v = data[ir][c.name];
                    if (ir === 0) {
                        let cell = {
                            t: 's',
                            v: c.name,
                        };
                        let cell_ref = XLSX.utils.encode_cell({
                            c: ic,
                            r: ir
                        });
                        worksheet[cell_ref] = cell;
                    }
                    let t;
                    if (v !== null && v !== undefined) {
                        if (c.type) {
                            // cell type
                            switch (c.type) {
                                case null:
                                case undefined:
                                case 'string':
                                case 'oid':
                                case 'date':
                                case 'boolean':
                                case 'email':
                                case 'phone':
                                case 'secret2':
                                    // no type
                                    // not change
                                    break;
                                case 'json':
                                    t = 's';
                                    if (v instanceof Object) {
                                        v = JSON.stringify(v);
                                    }
                                    break;
                                case 'integer':
                                case 'decimal':
                                    t = 'n';
                                    // no change
                                    break;
                                case 'secret1':
                                    v = undefined;
                                    // no type
                                    // no change
                                    break;
                                default:
                                    $.cms.log.error(`invalid field type '${c.type}'`);
                            }
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
            xlsx: xlsx
        }
    }

    !$.cms ? $.cms = {} : null;

    jQuery.extend($.cms, {
        download: Constructor()
    });
})();
