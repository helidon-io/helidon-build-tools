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

function validateEnum(selectedValues: string[], enumElement: any): any[] {
    const errors: any[] = [];
    if ((selectedValues == null || selectedValues.length === 0) && !enumElement.optional) {
        errors.push({elementId: enumElement.id, message: "You should choose some element."});
    }
    return errors;
}

function validateList(selectedValues: string[], list: any): any[] {
    const errors: any[] = [];
    if ((selectedValues == null || selectedValues.length === 0) && !list.optional) {
        errors.push({elementId: list.id, message: "At least one element must be selected."});
    }
    return errors;
}

function validateText(value: string, text: any): any[] {
    const errors: any[] = [];
    if ((value === null || value === '') && !text.optional) {
        errors.push({elementId: text.id, message: "Value cannot be empty."});
    }
    return errors;
}

function validateQuickPick(value: any, element: any): any[] {
    let errors: any[] = [];
    if (element.type === 'enum-element') {
        errors = validateEnum(value, element);
    }
    if (element.type === 'list-element') {
        errors = validateList(value, element);
    }
    return errors;
}

export { validateList, validateText, validateEnum, validateQuickPick }