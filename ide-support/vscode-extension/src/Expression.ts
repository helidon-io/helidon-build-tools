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

/**
 * Expression.
 */
export class Expression {

    private tokens: Token[];

    /**
     * Create a new expression.
     * @param tokens tokens
     */
    constructor (tokens: Token[]) {
        this.tokens = tokens
    }

    /**
     * Evaluate the expression.
     * @param resolver variable resolver
     * @return {*}
     */
    eval (resolver: any) {
        const stack = []
        for (let i = 0; i < this.tokens.length; i++) {
            const token = this.tokens[i]
            let value
            if (token.kind === TokenKind.OPERATOR) {
                let result
                const operand1 = stack.pop()
                if (token.value === Operator.NOT) {
                    result = !operand1.asBoolean()
                } else {
                    const operand2 = stack.pop()
                    switch (token.value) {
                        case Operator.OR:
                            result = operand2.asBoolean() || operand1.asBoolean()
                            break
                        case Operator.AND:
                            result = operand2.asBoolean() && operand1.asBoolean()
                            break
                        case Operator.EQUAL:
                            result = operand2.value === operand1.value
                            break
                        case Operator.NOT_EQUAL:
                            result = operand2.value !== operand1.value
                            break
                        case Operator.CONTAINS:
                            result = operand2.asArray().includes(operand1.asString())
                            break
                        default:
                            throw new Error('Unsupported operator:' + token.value)
                    }
                }
                value = TypedValue.of(result)
            } else if (token.kind === TokenKind.LITERAL) {
                value = token.value
            } else if (token.kind === TokenKind.VARIABLE) {
                const resolvedValue = resolver(token.value)
                if (resolvedValue == null) {
                    throw new Error('Unresolved variable: ' + token.value)
                }
                value = TypedValue.of(resolvedValue)
            } else {
                throw new Error('Unsupported token: ' + token.kind)
            }
            stack.push(value)
        }
        return stack.pop().value
    }

    /**
     * Create the expression from the given 'raw' data.
     * @param data data
     * @return {Expression}
     */
    public static create (data: any) : Expression {
        const tokens = []
        if (!Array.isArray(data)) {
            throw new TypeError('Invalid expression data: ' + JSON.stringify(data))
        }
        for (let i = 0; i < data.length; i++) {
            const token = data[i]
            if (!Object.getOwnPropertyDescriptor(token, 'kind') || !Object.getOwnPropertyDescriptor(token, 'value')) {
                throw new Error('Invalid token: ' + JSON.stringify(token))
            }
            switch (token.kind) {
                case 'literal':
                    tokens.push(new Token(TokenKind.LITERAL, TypedValue.of(token.value)))
                    break
                case 'operator':
                    tokens.push(new Token(TokenKind.OPERATOR, Operator.of(token.value)))
                    break
                case 'variable':
                    tokens.push(new Token(TokenKind.VARIABLE, token.value))
                    break
                default:
                    throw new Error('Unsupported token:' + token.kind)
            }
        }
        return new Expression(tokens)
    }
}

/**
 * Token.
 */
class Token {

    kind: TokenKind;
    value: any;

    /**
     * Create a new token.
     * @param kind kind
     * @param value value
     */
    constructor (kind: TokenKind, value: any) {
        this.kind = kind
        this.value = value
    }
}

/**
 * Token kind.
 */
class TokenKind {
    /**
     * Literal.
     * @type {TokenKind}
     */
    static LITERAL = new TokenKind('literal')

    /**
     * Operator.
     * @type {TokenKind}
     */
    static OPERATOR = new TokenKind('operator')

    /**
     * Variable.
     * @type {TokenKind}
     */
    static VARIABLE = new TokenKind('variable')

    private name: string;

    /**
     * Create a new token kind.
     * @param name name
     */
    constructor (name: string) {
        this.name = name
    }

    /**
     * Get a token kind.
     * @param value value
     * @return {TokenKind}
     */
    static of (value: string) {
        switch (value) {
            case 'literal':
                return TokenKind.LITERAL
            case 'operator':
                return TokenKind.OPERATOR
            case 'variable':
                return TokenKind.VARIABLE
        }
    }
}

/**
 * Operator kind.
 */
class Operator {
    /**
     * Equal operator.
     * @type {Operator}
     */
    static EQUAL = new Operator('==')

    /**
     * Not equal operator.
     * @type {Operator}
     */
    static NOT_EQUAL = new Operator('!=')

    /**
     * And operator.
     * @type {Operator}
     */
    static AND = new Operator('&&')

    /**
     * Or operator.
     * @type {Operator}
     */
    static OR = new Operator('||')

    /**
     * Contains operator.
     * @type {Operator}
     */
    static CONTAINS = new Operator('contains')

    /**
     * Not operator.
     * @type {Operator}
     */
    static NOT = new Operator('!')

    private name: string;

    constructor (name: string) {
        this.name = name
    }

    /**
     * Get the operator.
     * @param value string representation
     * @return {Operator}
     */
    static of (value: string) {
        switch (value) {
            case '&&':
                return Operator.AND
            case '||':
                return Operator.OR
            case '!':
                return Operator.NOT
            case '==':
                return Operator.EQUAL
            case '!=':
                return Operator.NOT_EQUAL
            case 'contains':
                return Operator.CONTAINS
            default:
                throw new Error('Unsupported operator: ' + value)
        }
    }
}

/**
 * Value.
 */
class Value {

    readonly value: any;

    /**
     * Create a new value.
     * @param value value
     */
    constructor (value: any) {
        this.value = value
    }

    /**
     * Get this value as the given type.
     * @param kind expected type
     * @return {*}
     */
    as (kind: ValueKind) {
        const type = typeof (this.value)
        switch (kind) {
            case ValueKind.BOOLEAN:
                if (type === 'boolean') {
                    return this.value
                }
                if (type === 'string') {
                    switch (this.value.trim().toLowerCase()) {
                        case 'y':
                        case 'yes':
                        case 'true':
                            return true
                        case 'n':
                        case 'no':
                        case 'false':
                            return false
                        default:
                            throw new Error(`${this.value} is not a boolean`)
                    }
                }
                break
            case ValueKind.STRING:
                if (type === 'string') {
                    return this.value
                }
                if (type === 'boolean') {
                    return this.value ? 'true' : 'false'
                }
                break
            case ValueKind.ARRAY:
                if (Array.isArray(this.value)) {
                    return this.value
                }
                if (type === 'string') {
                    return this.value.split(' ').filter((s: string) => s.length > 0)
                }
                break
            default:
                throw new Error(`Unsupported value kind: ${kind}`)
        }
        throw new Error(`Cannot get a ${type} as a ${kind.name}`)
    }

    /**
     * Get this value as a boolean.
     * @return {boolean}
     */
    asBoolean () {
        return this.as(ValueKind.BOOLEAN)
    }

    /**
     * Get this value as a string.
     * @return {string}
     */
    asString () {
        return this.as(ValueKind.STRING)
    }

    /**
     * Get this value as an array.
     * @return {[string]}
     */
    asArray () {
        return this.as(ValueKind.ARRAY)
    }
}

/**
 * Typed value.
 */
class TypedValue extends Value {

    private readonly kind: ValueKind;

    /**
     * Create a new value.
     * @param kind literal kind
     * @param value value
     */
    constructor (kind: ValueKind, value: any) {
        super(value)
        this.kind = kind
    }

    /**
     * Get this value as the given type.
     * @param kind expected type
     * @return {*}
     */
    as (kind: ValueKind): any {
        if (this.kind !== kind) {
            throw new Error(`Cannot get a value of type: ${this.kind.name} as ${kind.name}`)
        }
        return this.value;
    }

    /**
     * Create a new value.
     * @param value value
     * @return {TypedValue}
     */
    static of (value: any) {
        const valueType = typeof (value)
        if (valueType === 'boolean') {
            return new TypedValue(ValueKind.BOOLEAN, value)
        }
        if (valueType === 'string') {
            return new TypedValue(ValueKind.STRING, value)
        }
        if (Array.isArray(value)) {
            return new TypedValue(ValueKind.ARRAY, value)
        }
        throw new Error('Unsupported value type: ' + valueType)
    }
}

/**
 * Value kind.
 */
class ValueKind {
    /**
     * Boolean.
     * @type {ValueKind}
     */
    static BOOLEAN = new ValueKind('boolean')

    /**
     * String.
     * @type {ValueKind}
     */
    static STRING = new ValueKind('string')

    /**
     * Array.
     * @type {ValueKind}
     */
    static ARRAY = new ValueKind('array')

    name: string;

    /**
     * Create a new value kind.
     * @param name name
     */
    constructor (name: string) {
        this.name = name
    }
}

