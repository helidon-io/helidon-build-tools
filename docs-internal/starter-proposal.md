# Helidon Starter Proposal

## Background

This project started from the idea of adding "features" (e.g. tracing or Open API) to the CLI in the form of options to the `init`
command as well as `add-feature` and `remove-feature` commands. 

A feature is straightforward from a user perspective: a meaningful name that can be selected along with a description
of what it is and when/why it should be used. 

At the implementation level, however, things are much more complex. Ultimately, a feature must be represented in an internal 
data model as some metadata plus one or more of the following:

1. Build file fragments, e.g. pom.xml dependencies 
1. Configuration file fragments
1. README file fragments
1. Java source file fragments, e.g. Main.java
1. Complete source files 

Relationships _across_ features must also be modelled somehow, e.g.
 
1. `A allows B..M`
1. `D requires E`
1. `C and F implies G`
1. `X precedes Y`

In addition to producing and maintaining this data, the use of it implies some form of _merge_. At project creation, a base 
archetype must be merged with feature content, and the result must be both correct and properly formatted. A good templating
mechanism should be sufficient for this (perhaps with some post-processing to format), but to add or remove features from
a previously created project requires an actual merge. 

These are significant requirements when the content is entirely under our control; however, when a user has modified the source 
and wants to add or remove features the correctness requirement becomes an extremely high bar, far more of a science project. We 
deferred the [first attempt at supporting features](https://github.com/tomas-langer/helidon/blob/e47f1fe27c75d6fe2120fffdd4d97a166e5c8e99/docs-internal/features/features.md)
because of the latter realization; solving the update problem requires some simplifying assumptions.  

Further, the scope of this project has been expanded in light of the recognition that we must augment/replace our current 
documentation-only Quickstart experience with a simpler and more interactive one for new users. The important additions are:

1. A web UI that guides creation and initial use of a new project. 
1. A REST service, both for the web UI and to enable third party UIs (e.g. IDEs).   
       

## Non Goals

1. Feature add/remove. Given the high likelihood of introducing errors, direct modification of an existing project will not be 
   supported. As an admittedly poor substitute, we can (eventually) follow the Micronaut `feature diff` model and rely on 
   knowledgeable users to apply and merge. (Note that in the CLI we may be able to do better if the project is a git repo, 
   since we can run `git apply --check` to see if the merge would succeed and then apply it if so, all under some new flag.)
   
2. SEO. Ultimately we need users to be able to find relevant parts of `helidon.io` via expected search patterns (e.g. 
   "get started with helidon", "what is helidon", etc.) but this is a complex task to do properly, and may require significant
   changes to the site implementation (i.e. so that it can support non-anchor URLs). We may be able to do some very simple
   optimizations to support searches that target the landing page.

## Goals

1. Ensure that a potential user can try out Helidon with a minimum of steps.
1. Enhance our "Getting Started" page with a _guided_ experience ("wizard") that provides help and exposes complexity only as needed.
1. Provide single-click shortcuts (e.g. "Hello World") in the wizard for common cases.    
1. Expose the archetype data model via a service so that IDE vendors can use it to add project creation support.
1. Ensure that the CLI `init` command matches the wizard as much as is reasonable.

## Stretch Goals
 
1. Minimal SEO to support searches like "what is helidon" and "how do I try helidon" etc. that resolve to the landing page.
1. Provide wizard pathways tailored for users with different backgrounds, e.g. Spring Boot, Java EE, Helidon expert, etc.


## User Stories

1. As a developer who has only heard of Helidon, I want to easily find a page on the web so I can try it out without reading a lot of docs.
1. As a developer who has found the wizard, I don’t want to be told I need anything more than a JDK to try it since I’m not sure I want to use Helidon. 
1. As a first time wizard user, I need clear, helpful guidance through the choices so I’m not overwhelmed by things I don’t understand yet.
1. As a wizard user, I want to be able to easily navigate back to earlier choices so I can change my mind.
1. As a wizard user, I need to be confident that selecting among all of the choices presented to me will result in a working project.
1. As a wizard user, I want very simple instructions on how to build, run and use the created project so I don’t have to learn anything else.
1. As a Spring Boot developer, I want the wizard to help me try out comparable features so I can understand how to migrate and get started.
1. As a Java EE developer, I want the wizard to help me try out comparable features so I can understand how to migrate and get started.
1. As an experienced wizard user, I want to turn off the extra help information so I can create a new project with the features I want very quickly.
1. As a wizard user, I want the created project README to contain links for further reading when I’m ready so I don’t have to hunt it down myself.
1. As a wizard user, I want to see the rendered README once the project is generated, so I don’t have to read markup.
1. As a developer who has never used Maven, I want the created project README to have some basic help on getting started using it so I don’t have to learn that too.
1. As a new Helidon user, I want the created project README to provide information on installing and using the CLI.
1. As a Helidon CLI user, I want the `init` command experience to be as close to the web wizard experience as possible so it is familiar.
1. As an IDE developer, I want an easily understood REST service for creating new Helidon projects so that I can provide a great user experience.

See [Laird's doc](https://docs.google.com/document/d/1G4IEycA9PDkodNI1d3-Zuj5WoA4oiIn2vKqwt7XAvIo/edit#heading=h.ui9koosmi9gl) for
a deeper set of user stories and goals.

           
## User Experience

This section describes the proposed user experience embedded in commentary at this level, like so:

> { _user experience here_ }

The flow begins when a user reaches the landing page at `helidon.io`, and assumes that the wizard is an integral part of the 
existing single page app. The wizard will be developed initially as a separate `start.helidon.io` site, but must then be 
integrated into the current app to provide a simplified and more seamless experience.

Let's go...
                
> Our user reaches the `helidon.io` landing page where she sees a _prominent_ **Try Me** button.

The current `Getting Started` link in the header is changed to `Try Me` and has the same action as the button; leaving this link
here provides easy access when not on the landing page.

> She clicks the **Try Me** button.

This starts the wizard, which like any other "slide" in the current app just immediately replaces the page content. 

The wizard is composed of steps, each of which contains one or more related choices along with information to help choose and
simple navigation to go back to a previous step or cancel. See [here](https://vuetifyjs.com/en/components/steppers/) for an
example "stepper" UI component with a progress bar naming the current and previous steps. To accomodate multiple steps, we will
use a [vertical stepper](https://vuetifyjs.com/en/components/steppers/#vertical) with a dynamic rather than a fixed list of steps.  

> She is presented with the choice of SE or MP, with help in the form of both code and overview bullet points. She sees 
> `Try SE` and `Learn More` buttons under SE, and `Try MP` and `Learn More` under MP. The first progress element is shown naming
> this step (e.g. `Helidon SE or MP`).

The help content here is just like the current `Getting Started` page. 

The `Learn More` buttons exit the wizard and lead to documentation as they do today. Alternatively, they could simply open a new
tab and not exit the wizard. TBD.

The `Try SE` and `Try MP` buttons replace the `GET STARTED` buttons, and lead to the next step of the wizard rather than to 
documentation as they do today.

> She clicks the `Try MP` button, and proceeds to the next step. A new `Application` progress element is added naming this 
> step. She is asked to select from among a small list of base applications (e.g. `Hello World`, `JPA with Hibernate & UCP`, etc.)
> via radio buttons. Each app has associated help that describes what it does.
 
SE and MP may have different sets of base applications.

Steps are nodes in a directed acyclic graph which is optimized to reach termination quickly. It is a variant of a 
[decision tree](https://en.wikipedia.org/wiki/Decision_tree), where each decision leads to another or to termination and
the creation of the starter project.

The user follows a path through a series of choices, where each choice contains help and previous choices dictate which choices 
are presented next. At any point the user can navigate back to a previous step via the progress bar or to another part of 
`helidon.io` to cancel.

Some steps will activate a `Try It` button since enough choices have been made to do so. Some steps will support further
customization, and on those a `Customize It` button will activate. Some steps will activate both.

When a base application supports features, steps associated with these are activated. At each step, the user will have both
the `Try It` and `Customize It` buttons enabled to either stop or go on to the next feature.
 
Some features require selection of a single implementation among the available choices, some may require one or more related
components to be selected. Some combinations of features may require automatic inclusion of "hidden features" (e.g. for
integration modules such as `helidon-tracing-jersey`).

Docker and Kubernetes support would be modeled and exposed as features.
                                   
> She selects `Hello World` and two buttons are activated: `Try It` or `Customize It`.

Any application _can_ be customized further to provide a name, package name, groupId and artifactId, but it is not necessary to
do so.

> She selects `Customize It` just to see what happens, and proceeds to a step where she can type in values for the required
> properties. Each field is pre-filled with a default, but the first keystroke in a field clears the default and enters that key.
> The `Customize It` button is deactivated (or removed). She doesn't want to bother doing more, so just clicks `Try It`.

Here she has reached a terminal step. She could have reached the same state by selecting `Try It` on the previous step, or using
the progress bar here to go back and do so; either of these paths would simply use default values.
                                                                  
We're now ready to generate the project. We:

1. Generate the project and create a zip file. (In the future, we can also support creating a GitHub repo.)
2. Render the generated README.md file.

> She sees the zip file download begin, along with short instructions (specific to her OS) to unpack and cd into the directory. 
> The rendered README is also displayed, which contains further instructions and documentation. 


## Deliverables

#### Archetype Data Model Updates

TODO

#### Archetype Engine Updates

TODO
 
#### Project Generator Service

TODO

#### Wizard UI: `start.helidon.io`

TODO

#### Wizard Integration: `helidon.io` 

TODO

#### CLI Updates

TODO