/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
package org.eclipse.microprofile.openapi.models;

import java.util.List;
import java.util.Map;

public interface TopLevel extends Reference {

    public enum MyEnum {OFF, ON}

    public String getStuff();

    public void setStuff(String stuff);

    public MyEnum getState();

    public void setState(MyEnum state);

    public List<String> getNames();

    public void setNames(List<String> names);

    public Map<String, Stuff> getStuffMap();

    public void setStuffMap(Map<String, Stuff> map);

    public void setThing(String thing);

    public String getThing();


}
