/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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

import { Context } from "./Context";

export interface GeneratorData {
    steps: any[],
    elements: any[],
    currentElementIndex: number,
    context: Context;
}

export class GeneratorDataAPI {

    public static convertProjectDataElements(generatorData: GeneratorData): Map<string, string> {
        const result: Map<string, string> = new Map();
        for (let element of generatorData.elements) {
            if (element._skip === true) {
                continue;
            }
            let value: string | null = null;
            if (element.kind === 'enum') {
                if (element.selectedValues.length > 0) {
                    value = element.selectedValues[0];
                }
            } else if (element.kind === 'boolean') {
                if (element.selectedValues.length > 0) {
                    value = element.selectedValues[0];
                } else {
                    value = 'false';
                }
            } else if (element.kind === 'list') {
                value = element.selectedValues.join(",")
            } else if (element.kind === 'text') {
                value = element.value;
            }
            if (value != null) {
                result.set(element._scope.id, value);
            }
        }
        return result;
    }
}

export abstract class BaseCommand {

    readonly initialData: GeneratorData;

    protected constructor(initialData: GeneratorData) {
        this.initialData = initialData;
    }

    public undo(): GeneratorData {
        this.initialData.context.scope.clear();
        return this.initialData;
    }

    public abstract execute(): GeneratorData;
}

export class TextCommand extends BaseCommand {

    constructor(initialData: GeneratorData) {
        super(initialData);
    }

    execute(): GeneratorData {
        return {
            steps: this.initialData.steps,
            elements: this.initialData.elements,
            currentElementIndex: this.initialData.currentElementIndex + 1,
            context: this.initialData.context
        };
    }

}

export class OptionCommand extends BaseCommand {

    steps: any[];
    elements: any[];
    newElements: any[] = [];
    context: Context = new Context();

    constructor(initialData: GeneratorData) {
        super({
            steps: initialData.steps,
            elements: initialData.elements,
            currentElementIndex: initialData.currentElementIndex,
            context: Context.clone(initialData.context)
        });
        this.steps = [...this.initialData.steps];
        this.elements = [...this.initialData.elements];
        this.context = Context.clone(initialData.context);
    }

    public selectedOptionsChildren(newElements: any[]): void {
        this.newElements = newElements;
    }

    public setContext(newContext: Context) {
        this.context = newContext;
    }

    execute(): GeneratorData {
        this.updateElements(this.initialData.currentElementIndex, this.newElements);
        return {
            steps: this.steps,
            elements: this.elements,
            currentElementIndex: this.initialData.currentElementIndex + 1,
            context: this.context
        };
    }

    private updateElements(currentElementIndex: number, newElements: any[]): void {
        if (newElements.length > 0) {
            this.elements.splice(currentElementIndex + 1, 0, ...newElements);
        }
    }
}

