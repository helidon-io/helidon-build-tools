/*
 * Copyright (c) 2020, 2020 Oracle and/or its affiliates.
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
 *
 */
package io.smallrye.openapi.api.models;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.TopLevel;

public class TopLevelImpl implements TopLevel {

    private String stuff;
    private TopLevel.MyEnum state;
    private List<String> names;
    private Map<String, Stuff> stuffMap;

    public String getStuff() {
        return stuff;
    }

    public void setStuff(String stuff) {
        this.stuff = stuff;
    }

    public MyEnum getState() {
        return state;
    }

    public void setState(MyEnum state) {
        this.state = state;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public Map<String, Stuff> getStuffMap() {
        return stuffMap;
    }

    public void setStuffMap(Map<String, Stuff> map) {
        this.stuffMap = map;
    }
}
