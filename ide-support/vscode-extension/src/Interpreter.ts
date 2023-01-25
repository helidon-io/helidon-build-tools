/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { QuickPickItemExt } from "./common";
import { GeneratorData } from "./GeneratorCommand";
import { Expression } from "./Expression";
import { ContextValueKind } from "./Context";

export class Interpreter {

    private archetype: any;
    private newElements: any[] = [];
    private generatorData: GeneratorData;

    constructor(archetype: any, generatorData: GeneratorData) {
        this.archetype = archetype;
        this.generatorData = generatorData;
    }

    public process(element: any): any[] {
        this.newElements = [];
        this.visit(element);
        return this.newElements;
    }

    private visit(element: any) {
        switch (element.kind) {
            case 'step':
                this.processStep(element);
                break;
            case 'inputs':
                this.processInputs(element);
                break;
            case 'list':
                this.processList(element);
                break;
            case 'enum':
                this.processList(element);
                break;
            case 'boolean':
                this.processList(element);
                break;
            case 'text':
                this.processText(element);
                break;
            case 'call':
                this.processMethod(element);
                break;
            case 'presets':
                this.processPreset(element);
                break;
            case 'variables':
                this.processVariables(element);
                break;
            default:
                break;
        }
    }

    private processInputs(element: any) {
        if (element.children) {
            for (const child of element.children) {
                child._scope = this.generatorData.context.newScope(child);
                const contextValue = this.generatorData.context.getValue(child._scope.id);
                // eslint-disable-next-line eqeqeq
                if (contextValue == null || child.kind !== 'boolean' || contextValue.value === true) {
                    this.visit(child);
                }
            }
        }
    }

    private processStep(element: any) {
        if (element.children) {
            for (const child of element.children) {
                this.visit(child);
            }
        }
    }

    private processList(element: any) {
        const options: QuickPickItemExt[] = [];
        const defaultValue: any[] = this.getDefaultValue(element);
        const contextValue = this.generatorData.context.getValue(element._scope.id);
        // eslint-disable-next-line eqeqeq
        if (contextValue == null) {
            this.generatorData.context.setValue(element._scope.id, defaultValue, ContextValueKind.DEFAULT);
        }
        const selectedOptions: QuickPickItemExt[] = [];
        if (element.kind === 'boolean') {
            options.push(
                {label: 'yes', children: element.children, value: 'true'},
                {label: 'no', value: 'false'});
        } else {
            options.push(...element.children.filter((child: any) => child.kind === 'option').map((o: any) => {
                return {
                    label: o.name,
                    children: o.children,
                    value: o.value,
                    description: o.description
                }
            }));
        }
        // eslint-disable-next-line eqeqeq
        if (contextValue != null) {
            selectedOptions.push(
                ...options.filter(option => {
                    if (Array.isArray(contextValue?.value)) {
                        return contextValue?.value.includes(option.value);
                    } else {
                        if (element.kind === 'boolean') {
                            // eslint-disable-next-line eqeqeq
                            return (option.value?.toLowerCase() === 'true') == contextValue?.value;
                        }
                        return option.value === contextValue?.value;
                    }
                })
            );
        } else if (defaultValue.length > 0) {
            selectedOptions.push(
                ...options.filter(option =>
                    defaultValue.includes(element.kind === 'boolean' ? (option.value?.toLowerCase() === 'true') : option.value!))
            );
        }
        const optional = element.optional ? element.optional : false;
        const result = {
            title: element.name,
            placeholder: (element.name ?? "") + ` (optional - ${optional}). Press 'Enter' to confirm.`,
            items: options,
            selectedItems: selectedOptions,
            kind: element.kind,
            id: element.id,
            path: element._scope.id,
            _scope: element._scope,
            _skip: false
        };

        const defaultContextValueKind = ContextValueKind.DEFAULT;
        // eslint-disable-next-line eqeqeq
        if (contextValue == null || contextValue.kind === defaultContextValueKind) {
            if (options && options.length <= 1) {
                result._skip = true;
            }

        } else {
            result._skip = true;
        }
        this.newElements.push(result);
    }

    private getDefaultValue(element: any): any[] {
        if (element.selectedValues) {
            return element.selectedValues;
        }
        // eslint-disable-next-line eqeqeq
        if (element.default != null) {
            if (element.kind === 'list') {
                return element.default
                    .map((e: string) => this.evaluateProps(e, (v: any) => this.generatorData.context.lookup(v)));
            } else {
                if (typeof (element.default) === 'string') {
                    return [this.evaluateProps(element.default, (v: any) => this.generatorData.context.lookup(v))];
                }
                return [element.default];
            }
        }
        return [];
    }

    private processText(element: any) {
        const optional = element.optional ? element.optional : false;
        // eslint-disable-next-line eqeqeq
        if (element.default != null) {
            element.default = this.evaluateProps(element.default, (v: any) => this.generatorData.context.lookup(v));
        }
        const defValPlaceholder = element.default ? ` default value: ${element.default};` : "";
        const result = {
            title: element.name,
            placeholder: (element.name ?? "") + defValPlaceholder + ` (optional - ${optional}). Press 'Enter' to confirm.`,
            value: element.default ? element.default : "",
            prompt: element.name,
            messageValidation: (value: string) => undefined,
            kind: element.kind,
            id: element.id,
            path: element._scope.id,
            _scope: element._scope,
            _skip: false
        };
        this.newElements.push(result);
    }

    private processMethod(element: any) {
        const methods = this.archetype.methods[element.method];
        for (const method of methods) {
            this.visit(method);
        }
    }

    private processPreset(element: any) {
        if (element.children) {
            for (const child of element.children) {
                this.generatorData.context.setValue(child.path, child.value, ContextValueKind.PRESET);
            }
        }
    }

    public setGeneratorData(generatorData: GeneratorData) {
        this.generatorData = generatorData;
    }

    private processVariables(element: any) {
        if (element.children) {
            for (const child of element.children) {
                const expressionResult = Expression.create(this.archetype.expressions[child.if])
                    .eval((val: string) => this.generatorData.context.lookup(val));
                if (expressionResult === true) {
                    this.generatorData.context.variables.set(child.path, child.value);
                }
            }
        }
    }

    /**
     * Evaluate properties within a string.
     * @param input {string}
     * @param resolver {Function}
     * @return {string|*}
     */
    private evaluateProps(input: string, resolver: any): string {
        let start = input.indexOf('${');
        let end = input.indexOf('}', start);
        let index = 0;
        let resolved = null;
        while (start >= 0 && end > 0) {
            if (resolved === null) {
                resolved = input.substring(index, start);
            } else {
                resolved += input.substring(index, start);
            }
            let propName = input.substring(start + 2, end);

            // search for transformation (name/regexp/replace)
            let matchStart = 0;
            do {
                matchStart = propName.indexOf('/', matchStart + 1);
            } while (matchStart > 0 && propName.charAt(matchStart - 1) === '\\')
            let matchEnd = matchStart;
            do {
                matchEnd = propName.indexOf('/', matchEnd + 1);
                // eslint-disable-next-line
            } while (matchStart > 0 && propName.charAt(matchStart - 1) === '\\')

            let regexp = null;
            let replace = null;
            if (matchStart > 0 && matchEnd > matchStart) {
                regexp = propName.substring(matchStart + 1, matchEnd);
                replace = propName.substring(matchEnd + 1);
                propName = propName.substring(0, matchStart);
            }

            let propValue = resolver(propName);
            // eslint-disable-next-line eqeqeq
            if (propValue == null) {
                propValue = '';
            } else if (regexp !== null && replace !== null) {
                propValue = propValue.replaceAll(regexp, replace);
            }

            resolved += propValue;
            index = end + 1;
            start = input.indexOf('${', index);
            end = input.indexOf('}', index);
        }
        if (resolved !== null) {
            return resolved + input.substring(index);
        }
        return input;
    }
}