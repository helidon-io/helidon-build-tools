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

export interface GeneratorData {
    steps: any[],
    elements: any[],
    currentElementIndex: number,

}

export abstract class BaseCommand {

    readonly initialData: GeneratorData;

    protected constructor(initialData: GeneratorData) {
        this.initialData = initialData;
    }

    public undo(): GeneratorData {
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
            currentElementIndex: this.initialData.currentElementIndex + 1
        };
    }

}

export class OptionCommand extends BaseCommand {

    steps: any[];
    elements: any[];
    newElements: any[] = [];

    constructor(initialData: GeneratorData) {
        super(initialData);
        this.steps = [...this.initialData.steps];
        this.elements = [...this.initialData.elements];
    }

    public selectedOptions (newElements: any[]) : void {
        this.newElements = newElements;
    }

    execute(): GeneratorData {
        this.updateElements(this.initialData.currentElementIndex, this.newElements);
        return {
            steps: this.steps,
            elements: this.elements,
            currentElementIndex: this.initialData.currentElementIndex + 1
        };
    }

    private updateElements(currentElementIndex: number, newElements: any[]): void {
        let newInputs: any[] = [];
        for (let element of newElements) {
            if (element.type === 'step-element') {
                this.addStep(element, newInputs);
            } else {
                newInputs.push(element);
            }
        }
        if (newInputs.length > 0) {
            this.elements.splice(currentElementIndex+1, 0, ...newInputs);
        }
    }

    private addStep(step: any, inputList: any[]): void {
        if (step.children == null) {
            return;
        }
        let newInputs: any[] = [];
        for (let element of step.children) {
            if (element.type === 'step-element') {
                this.addStep(element, newInputs);
            } else {
                newInputs.push(element);
            }
        }
        inputList.push(...newInputs);
    }

}

