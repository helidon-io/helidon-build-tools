# Helidon `sitegen` Maven Plug-in

## AsciiDoc File Processing

### Background

GitHub renders AsciiDoc, but it does not process `include::` directives (although 
the claim is that this might be fixed in 2019). Many of 
our AsciiDoc files include from other places extensively.

To work around this, the `sitegen` plug-in provides two goals:

* `preprocess-adoc` -  "Pre-includes" included text specified by AsciiDoc `include::` 
directives into an `.adoc` file, adding
AsciiDoc comments to track where each snippet of included content is in the 
updated file and where it came from

* `naturalize-adoc` - Converts a preprocessed `.adoc` file back into natural form with 
conventional `include::` directives

### Developer Workflow

A developer who needs to edit an `.adoc` file containing includes might follow a workflow
like this:

1. Clone or pull the GitHub repository.
2. `cd` to a directory in the code containing a `README.adoc` that uses includes.
3. Run `mvn sitegen:naturalize-adoc` to convert the file to normal format.
4. Iteratively edit the file (or files it includes) and rebuild, perhaps using
an AsciiDoc viewer to see what the results look like.
5. When all changes are done:


    mvn sitegen:preprocess-adoc # to convert the format.
    git add README.adoc
    git commit -m ...
    git push

### Build Pipeline
The `preprocess-adoc` supports the optional `check` parameter. When set to true
the plug-in not only generates the preprocessed form (presumably to some other directory)
but also compares the generated file with the same-named file in the input directory.
If they differ, the plug-in fails the build because probably the developer changed
the contents of the `.adoc` file or some files it includes

