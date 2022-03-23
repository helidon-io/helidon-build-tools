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

export const STEPS = {
  type: "step-element",
  id: "root",
  children: [
    {
      label: "Helidon Flavor",
      type: "step-element",
      id: "hf1",
      children: [
        {
          label: "Select a flavor",
          type: "enum-element",
          name: "flavor",
          id: "f1",
          defaultValue: "MP",
          options: [
            {
              label: "Helidon SE",
              value: "SE",
              id: "hse1",
              children: [
                {
                  label: "Application Type",
                  type: "step-element",
                  id: "se-at-1",
                  children: [
                    {
                      label: "Select archetype",
                      type: "enum-element",
                      name: "base",
                      id: "sea1",
                      defaultValue: "quickstart",
                      options: [
                        {
                          label: "Bare Helidon SE project suitable to start from scratch",
                          value: "bare",
                          id: "se-bare",
                          children: [
                            {
                              label: "Media Support",
                              type: "step-element",
                              id: "ms-seb-1",
                              children: [
                                {
                                  type: "boolean-element",
                                  label: "Media type support",
                                  id: "mts-se-1",
                                  name: "media-support",
                                  defaultValue: "false",
                                  prompt: "Do you want to configure media type support (e.g. JSON, XML)?",
                                  children: [
                                    {
                                      label: "Select formats",
                                      type: "list-element",
                                      id: "list-ms-1",
                                      name: "list-media-types",
                                      defaultValue: "json,xml",
                                      options: [
                                        {
                                          label: "JSON",
                                          value: "json",
                                          help: "Do you want to use JSON?",
                                          id: "ms-json-se-1",
                                          children: [
                                            {
                                              label: "Select JSON provider",
                                              type: "enum-element",
                                              name: "json-provider",
                                              id: "json-provider-se-1",
                                              options: [
                                                {
                                                  label: "Jackson",
                                                  value: "jackson",
                                                  id: "json-jackson-se-1",
                                                },
                                                {
                                                  label: "JSON-B",
                                                  value: "jsonb",
                                                  id: "json-jsonb-se-1",
                                                },
                                                {
                                                  label: "JSON-P",
                                                  value: "jsonp",
                                                  id: "json-jsonp-se-1",
                                                }
                                              ]
                                            },
                                          ]
                                        },
                                        {
                                          label: "XML",
                                          value: "xml",
                                          help: "Do you want to use XML?",
                                          id: "ms-xml-se-1",
                                          children: [
                                            {
                                              label: "Select XML provider",
                                              type: "enum-element",
                                              name: "xml-provider",
                                              id: "xml-provider-se-1",
                                              options: [
                                                {
                                                  label: "JAX-P",
                                                  value: "jaxp",
                                                  id: "xml-jaxp-se-1",
                                                },
                                                {
                                                  label: "JAX-B",
                                                  value: "jaxb",
                                                  id: "xml-jaxb-se-1",
                                                }
                                              ]
                                            }
                                          ]
                                        }
                                      ]
                                    }
                                  ]
                                }
                              ]
                            },
                            {
                              label: "Tracing",
                              type: "step-element",
                              id: "tr-s-1",
                              children: [
                                {
                                  type: "boolean-element",
                                  label: "Tracing support",
                                  id: "bl3",
                                  name: "tracing",
                                  prompt: "Do you want tracing support?",
                                  children: [
                                    {
                                      label: "Select a tracing provider",
                                      type: "enum-element",
                                      name: "provider",
                                      id: "tp-e-1",
                                      options: [
                                        {
                                          label: "Zipkin",
                                          value: "zipkin",
                                          id: "zipkin-1",
                                        },
                                        {
                                          label: "Jaeger",
                                          value: "jaeger",
                                          id: "jaeger-1",
                                        }
                                      ]
                                    },
                                    {
                                      label: "Service name",
                                      type: "text-element",
                                      id: "tr-sn-1",
                                      name: "service-name",
                                      defaultValue: "myservice"
                                    },
                                  ]
                                }
                              ]
                            }
                          ]
                        },
                        {
                          label: "Sample Helidon SE project that includes multiple REST operations",
                          value: "quickstart",
                          id: "se-quickstart",
                        },
                        {
                          label: "Helidon SE application that uses the dbclient API with an in-memory H2 database",
                          value: "database",
                          id: "se-database",
                        }
                      ]
                    }
                  ]
                },
              ]
            },
            {
              label: "Helidon MP",
              value: "MP",
              id: "hmp1",
              children: [
                {
                  label: "Application Type",
                  type: "step-element",
                  id: "mp-at-1",
                  children: [
                    {
                      label: "Select archetype",
                      type: "enum-element",
                      name: "base",
                      id: "mpa1",
                      defaultValue: "bare",
                      options: [
                        {
                          label: "Bare Helidon MP project suitable to start from scratch",
                          value: "bare",
                          id: "mp-bare",
                        },
                        {
                          label: "Sample Helidon MP project that includes multiple REST operations",
                          value: "quickstart",
                          id: "mp-quickstart",
                        },
                        {
                          label: "Helidon MP application that uses JPA with an in-memory H2 database",
                          value: "database",
                          id: "mp-database",
                        },
                      ]
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    },
    {
      label: "Customize Project",
      type: "step-element",
      id: "cp2",
      children: [
        {
          label: "Select a build system",
          type: "enum-element",
          name: "build-system",
          id: "cp-bs",
          defaultValue: "maven",
          options: [
            {
              label: "Apache Maven",
              value: "maven",
              id: "am-1",
            }
          ]
        },
        {
          label: "Project groupId",
          type: "text-element",
          id: "cp-pg-1",
          name: "groupId",
          defaultValue: "com.examples"
        },
        {
          label: "Project artifactId",
          type: "text-element",
          id: "cp-pa-1",
          name: "artifactId",
          optional: true,
          defaultValue: "myproject"
        },
        {
          label: "Project version",
          type: "text-element",
          id: "cp-pv-1",
          name: "version",
          optional: true,
          defaultValue: "1.0-SNAPSHOT"
        },
        {
          label: "Java package name",
          type: "text-element",
          id: "cp-jpn-1",
          name: "package",
          optional: true,
          defaultValue: "com.example.myproject"
        },
      ]
    }
  ]
};


