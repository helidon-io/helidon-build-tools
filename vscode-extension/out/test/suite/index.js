"use strict";
/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */
Object.defineProperty(exports, "__esModule", {value: true});
exports.run = void 0;
const path = require("path");
const Mocha = require("mocha");
const glob = require("glob");

function run() {
    // Create the mocha test
    const mocha = new Mocha({
        ui: 'tdd',
    });
    mocha.useColors(true);
    const testsRoot = path.resolve(__dirname, '..');
    return new Promise((c, e) => {
        glob('**/**.test.js', {cwd: testsRoot}, (err, files) => {
            if (err) {
                return e(err);
            }
            // Add files to the test suite
            files.forEach(f => mocha.addFile(path.resolve(testsRoot, f)));
            try {
                // Run the mocha test
                mocha.run(failures => {
                    if (failures > 0) {
                        e(new Error(`${failures} tests failed.`));
                    } else {
                        c();
                    }
                });
            } catch (err) {
                e(err);
            }
        });
    });
}

exports.run = run;
//# sourceMappingURL=index.js.map