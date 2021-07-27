/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.build.archetype.engine.v2.descriptor;

import java.util.Objects;

public class Select {

    private boolean required = false;
    private boolean multiple = false;
    private final String choice;

    Select(boolean required,
           boolean multiple,
           String choice) {
        this.required = required;
        this.multiple = multiple;
        this.choice = choice;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public String choice() {
        return choice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Select select = (Select) o;
        return required == select.required
                && multiple == select.multiple
                && choice.equals(select.choice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), required, multiple, choice);
    }

    @Override
    public String toString() {
        return "Select{"
                + "required=" + isRequired()
                + ", multiple=" + isMultiple()
                + ", choice=" + choice()
                + '}';
    }

}
