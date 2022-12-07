/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

export class Context {

    scope: ContextScope;
    scopes: Map<string, ContextScope>
    values: Map<string, ContextValue>;
    variables: Map<string, any>;

    constructor() {
        this.scope = new ContextScope(null, null);
        this.scopes = new Map();
        this.values = new Map();
        this.variables = new Map();
    }

    public static clone(context: Context): Context {
        let result = new Context();
        result.values = new Map(context.values);
        result.variables = new Map(context.variables);
        result.scope = {
            global: context.scope.global,
            children: new Map(context.scope.children),
            parent: context.scope.parent,
            id: context.scope.id,
            clear: context.scope.clear
        };
        result.scopes = new Map(context.scopes);
        return result;
    }

    /**
     * Get a value.
     * @param id {string}
     * @return {*|null}
     */
    public getValue(id: string) {
        const entry = this.values.get(id)
        if (entry !== undefined) {
            return entry;
        }
        return null;
    }

    /**
     * Set a value.
     * @param id {string}
     * @param value {*}
     * @param kind {ContextValueKind}
     * @return {*}
     */
    public setValue(id: string, value: any, kind: ContextValueKind) {
        const valueObject = new ContextValue(value, kind);
        this.values.set(id, valueObject);
        return valueObject;
    }

    public lookup(query: string): any {
        const id = this.query(query);
        let value = this.variables.get(id);
        if (value != null) {
            return value;
        }
        value = this.values.get(id)
        if (value != null) {
            return value.value;
        }
        return null;
    }

    /**
     * Resolve a query.
     * @param query {string}
     * @return {string}
     */
    private query(query: string): string {
        let scope = this.scope.global ? '' : this.scope.id;
        if (scope === null) {
            scope = '';
        }
        let key: string;
        if (query.startsWith('~')) {
            key = query.substring(1);
        } else {
            let offset = 0;
            let level = 0;
            while (query.startsWith('..', offset)) {
                offset += 2;
                level++;
            }
            if (offset > 0) {
                query = query.substring(offset);
            } else {
                level = 0;
            }
            let index;
            for (index = scope.length - 1; index >= 0 && level > 0; index--) {
                if (scope.charAt(index) === '.') {
                    level--;
                }
            }
            if (index > 0) {
                key = scope.substring(0, index + 1) + '.' + query;
            } else {
                key = query;
            }
        }
        return key;
    }

    /**
     * Create a new scope.
     * @param node {*}
     * @return {ContextScope}
     */
    public newScope(node: any): ContextScope {
        const scope = new ContextScope(this.scope, node);
        this.scopes.set(scope.id!, scope);
        return scope;
    }

    /**
     * Push a scope.
     * @param scope {ContextScope}
     */
    public pushScope(scope: ContextScope) {
        this.scope = scope;
    }

    /**
     * Pop the current scope.
     */
    public popScope() {
        if (this.scope.parent === null) {
            throw new Error('No such element');
        }
        this.scope = this.scope.parent!;
    }
}

/**
 * Context scope.
 */
export class ContextScope {

    global: boolean;
    children: Map<any, ContextScope>;
    parent: ContextScope | null;
    id: string | null;

    /**
     * Create a new context scope.
     * @param parent {ContextScope|null}
     * @param node {*|null}
     */
    constructor(parent: ContextScope | null, node: any) {
        this.global = node != null ? node.global : true
        this.children = new Map()
        if (parent !== null) {
            this.parent = parent
            if (node.global) {
                this.id = node.id
            } else {
                this.id = ContextScope.path(this.parent, node.id)
            }
            this.parent.children.set(this.id, this)
        } else {
            this.parent = null
            this.id = null
        }
    }

    public clear() {
        this.children.clear();
    }

    /**
     * Make a path.
     * @param parent {*}
     * @param id {string}
     * @return {string}
     */
    static path(parent: ContextScope, id: string) {
        return parent.global ? id : parent.id + '.' + id
    }
}

/**
 * Context value.
 */
class ContextValue {

    value: any;
    kind: ContextValueKind;

    /**
     * Create a new context value.
     * @param value {*}
     * @param kind {ContextValueKind}
     */
    constructor(value: any, kind: ContextValueKind) {
        this.value = value
        this.kind = kind
    }
}

/**
 * Context value kind.
 */
export enum ContextValueKind {

    PRESET = "preset",
    DEFAULT = "default",
    USER = "user"
}
