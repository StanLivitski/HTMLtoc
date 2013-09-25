<!--
 |  This file is part of HTMLtoc.
 |  Copyright © 2013 Konstantin Livitski
 |
 |  HTMLtoc is free software: you can redistribute it and/or modify
 |  it under the terms of the GNU Affero General Public License as published by
 |  the Free Software Foundation, either version 3 of the License, or
 |  (at your option) any later version.
 |
 |  This program is distributed in the hope that it will be useful,
 |  but WITHOUT ANY WARRANTY; without even the implied warranty of
 |  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 |  GNU Affero General Public License for more details.
 |
 |  You should have received a copy of the GNU Affero General Public License
 |  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 -->

<a name="sec-about"> </a>
About HTMLtoc
=============

HTMLtoc is a tool that adds a table of contents (TOC) to an
HTML document. You can use it to insert a TOC in a web page, online
documentation, etc. HTMLtoc can be run from the command line, a script, or
a Java application.

<a name="sec-repo"> </a>
About this repository
=====================

This repository contains the project's sources, license information, and files
required to build the project and work with its source code. The top-level
entries of this repository are:

        src/           		HTMLtoc's source files
        LICENSE		        Document that describes the project's licensing
        					 terms
        NOTICE   	        A summary of license terms that apply to HTMLtoc 
        build.xml      		Configuration file for the tool (Ant) that builds
                       		 the HTMLtoc binary
        .classpath     		Eclipse configuration file for the project
        .project       		Eclipse configuration file for the project
        README.md			This document

<a name="sec-download"> </a>
Downloading the binary
======================

The compiled binary of HTMLtoc is available for download at:

 - <https://github.com/StanLivitski/HTMLtoc/wiki/Download>

<a name="sec-depends"> </a>
Dependencies
------------

To run the binary distribution of HTMLtoc, you need the following software
installed on your machine:

   - A **Java runtime**, also known as JRE, Standard Edition (SE), version 6 or
   later. To check whether, and what version of JRE you have installed, run

        java -version 

   from the command line. When you install a [Java SDK](#sec-building), JRE is
   installed automatically.

   - A binary distribution of the [StAXform][] library. You can download the
   current binary at <https://github.com/StanLivitski/StAXForm/wiki/Download>.
   You should have the StAXform JAR on your CLASSPATH when running HTMLtoc.

<a name="sec-use"> </a>
Using HTMLtoc
=============

HTMLtoc is an XML transformer that reads an XHTML document, looks up certain
processing instructions (PIs) in it, and replaces those PIs along with the
fragment enclosed within them (TOC placeholder) with a generated table of
contents (TOC). The rest of the document is copied to the output unchanged,
except that the elements referenced by the TOC may have anchors assigned to
them.

HTMLtoc assembles the TOC from the text of certain elements of the source
document called the *outline elements*. You can choose which elements will be
used to build the outline. For example, you can request HTMLtoc to generate the
TOC from the text of HTML headings. Note that HTMLtoc processes only those
outline elements that follow the TOC placeholder.

HTMLtoc has a simple command line interface that allows you to run it
interactively or from a script. It also defines a class that extends
`javax.xml.transform.Transformer`. You can instantiate that class and use it
in a Java application.

<a name="sec-source-doc"> </a>
Preparing a source document
---------------------------

Before processing an HTML document with HTMLtoc, you have to make sure that
it complies with XML specification. In other words, HTMLtoc accepts only XHTML
documents.

HTMLtoc will start building a document's TOC when it encounters an XML fragment
surrounded by `name.livitski.tools.html.toc` processing instructions or an
empty `name.livitski.tools.html.toc` processing instruction. For example, XML
fragment

      <?name.livitski.tools.html.toc version="1.0" outline="h2" linetags="li"
        blocktags="ul.toc" ?>
        This text will be replaced with the document's TOC
      <?name.livitski.tools.html.toc /?>

activates HTMLtoc and configures it to extract the text of subsequent `<h2>`
elements. It sets up HTMLtoc to place the extracted text of each
such element into an item of the unordered list that will replace the above
fragment in the document. The list element `<ul>` will have a `class="toc"`
attribute generated.  

HTMLtoc distinguishes between an opening processing instruction (PI) with
XML-like attributes, and a closing PI containing a slash character ('/'). The
current version of HTMLtoc accepts only one pair of such processing
instructions. If there is no need to place text or any other content between
the opening and closing PIs, you may replace them with a single PI that
contains HTMLtoc's attributes followed by a slash. The above example may
then be rewritten as:

      <?name.livitski.tools.html.toc version="1.0" outline="h2" linetags="li"
        blocktags="ul.toc" /?>

The syntax of the opening PI's contents is the same as the attributes portion
of a regular XML tag. The table below explains the attributes that apply to
the opening PI: 

<table border="1" cellspacing="0" cellpadding="6" width="90%">
<tr>
<th width="30%">Attribute</th>
<th>Contents</th>
<th width="3ex">Required?</th>
</tr>
<tr>
<td><code>version</code></td>
<td>Literal string "<code>1.0</code>"</td>
<td>yes</td>
</tr>
<tr>
<td><code>outline</code></td>
<td>A comma-separated list of source HTML elements that generate TOC entries.
A tag's position on the list determines its level in the TOC's structure. The
element names must be unique and cannot be empty.
</td>
<td>no</td>
</tr>
<tr>
<td><code>blocktags</code></td>
<td>A comma-separated list of generated HTML tags that will group TOC entries
at each level of the outline. The first item on that list defines the element
that will contain the entire TOC. The list may have fewer elements than
the outline, and some elements may be empty. Missing or empty elements will
result in block wrappers not generated for the respective level of the TOC.
Tag names on the list may be followed by a dot '<code>.</code>' and a CSS class
name to apply to generated HTML elements.</td>
<td>no</td>
</tr>
<tr>
<td><code>linetags</code></td>
<td>A comma-separated list of generated HTML tags that will contain individual
TOC entries at each level of the outline. This list may have fewer items
than the outline, and some items may be empty. Missing or empty items
will be replaced with default line wrappers, currently defined as plain
<code>&lt;div&gt;</code> elements. Non-empty items may be followed by a
dot '<code>.</code>' and a CSS class name to apply to generated HTML elements.
</td>
<td>no</td>
</tr>
</table>

Thus, to prepare an HTML document for processing by HTMLtoc, you must:

 - Make sure it complies with XML syntax requirements.
 - Determine the location of the TOC that will be generated.
 - Place an XML fragment surrounded by `name.livitski.tools.html.toc`
 processing instructions or an empty `name.livitski.tools.html.toc` PI at
 that location.
 - Add attributes from the above table to configure HTMLtoc.

<a name="sec-output"> </a>
HTMLtoc output
--------------

HTMLtoc generates a multi-level table of contents and inserts it into the
resulting document. In a general case, each level of the table is enclosed
into a block element, such as `<div>`, `<ol>`, or `<ul>`, that contains items
on that level of the source document's outline. For example, with the following
PI:

      <?name.livitski.tools.html.toc version="1.0" outline="h2" linetags="li"
        blocktags="ul.toc" /?>

each chapter of the document that begins with an `<h2>` heading will generate
a list item:

>      <ul class="toc">
>      <li> <!-- heading from chapter 1 --> </li>
>	   <li> <!-- heading from chapter 2 --> </li>
	    ...
>      </ul>

_(Note that HTML comments included here won't be present in the generated TOC)_

If the document contains subsections at the lower outline levels, they are
grouped in blocks as explained above and the blocks are included in
higher-level blocks. For example, when HTMLtoc is configured like this:

      <?name.livitski.tools.html.toc version="1.0" outline="h2,h3"
      	linetags="div,li" blocktags="header,ul" /?>

and the source document has the following structure:

>      <h1>My document</h1>
>      <h2>Preface</h2>
>      <h2>Chapter 1</h2>
>      <h3>Section 1.1</h3>
>      <h3>Section 1.2</h3>
>      <h2>Chapter 2</h2>
>      <h3>Section 2.1</h3>
>      <h3>Section 2.2</h3>
>      <h2>Summary</h2>

then the generated TOC will have this structure:

>      <header>
>      <div>Preface</div>
>      <div>Chapter 1</div>
>      <ul>
>      <li>Section 1.1</li>
>      <li>Section 1.2</li>
>      </ul>
>      <div>Chapter 2</div>
>      <ul>
>      <li>Section 2.1</li>
>      <li>Section 2.2</li>
>      </ul>
>      <div>Summary</div>
>      </header>

In addition, the actual TOC added to the document will contain hyperlinks
from each entry to the respective heading element. For target elements with
an `id` attribute, the value of that attribute is used as anchor. For target
elements without an id, anchor values are generated by HTMLtoc. An excerpt
from such actual TOC may look like this:

>      <div>
>      <a href="#toc000002">User's guide</a>
>      </div>
>      <ul class="toc1">
>      <li>
>      <a href="#toc000003">Prerequisites</a>
>      </li>
>      <li>
>      <a href="#toc000004">Getting started</a>
>      </li>
      ...
>      </ul>  

and the target elements will have anchors assigned:

>      <h2 id="toc000002">
>      <a name="toc000002"> </a>
>      User's guide</h2>
	  ...
>      <h3 id="toc000003">
>      <a name="toc000003"> </a>Prerequisites</h3>
	  ...
>      <h3 id="toc000004">
>      <a name="toc000004"> </a>Getting started</h3>


<a name="sec-cmdline"> </a>
Command-line interface
----------------------

To run HTMLtoc from the command line or a script, add `html-toc.jar` and its
[dependencies](#sec-depends) to the CLASSPATH and run the class named
`name.livitski.tools.html.toc.ProcessFile`. The class expects exactly one
argument - the location of the source HTML file.

       java -cp html-toc.jar:staxform.jar name.livitski.tools.html.toc.ProcessFile source.html

The resulting document is streamed to the standard output.

If the document you process has a different encoding than the system default,
you should set the `name.livitski.tools.html.toc.encoding` system property to
the name of the document's encoding. That will ensure the correct
transformation of the document. Note that HTMLtoc cannot change the document's
encoding when run from the command line. You may be able to read and write the
document using different encodings when you use the Java API described
[below](#sec-api).
 
<a name="sec-api"> </a>
HTMLtoc API
-----------

To run HTMLtoc from a Java application, first make sure that both
`html-toc.jar` and its [dependencies](#sec-depends) are either on the CLASSPATH
or otherwise reachable to the context class loader.

Then you can create an instance of `name.livitski.tools.html.toc.Transformer`
class, a subclass of `javax.xml.transform.Transformer`. The constructor of
`name.livitski.tools.html.toc.Transformer` takes no arguments:

>     import name.livitski.tools.html.toc.Transformer;
     ...
>     Transformer htmltoc = new Transformer();

Then you can use the new object as a [TrAX][] transformer instance:

>     StreamSource source;
>     StreamResult result;
>	  // initialize source and result
     ... 
>     htmltoc.transform(source,result);
     
Note, however, that some settings and options of a [TrAX][] transformer may
not be implemented in [StAXform][] yet, and therefore will not work in HTMLtoc
either. If you want to use those options, please consider
[contributing to StAXform](https://github.com/StanLivitski/StAXForm/blob/master/README.md#sec-contact).

<a name="sec-building"> </a>
Building HTMLtoc
=================

To build the project's binary from this repository, you need:

   - A **Java SDK**, also known as JDK, Standard Edition (SE), version 6 or
   later, available from OpenJDK <http://openjdk.java.net/> or Oracle
   <http://www.oracle.com/technetwork/java/javase/downloads/index.html>.

   Even though a Java runtime may already be installed on your machine
   (check that by running `java --version`), the build will fail if you
   don't have a complete JDK (check that by running `javac`).

   - **Apache Ant** version 1.7.1 or newer, available from the Apache Software
   Foundation <http://ant.apache.org/>.

To build HTMLtoc, go to the directory containing its working copy. Make sure
that any libraries listed as project's [dependencies](#sec-depends) are copied
(or linked) to the `lib` subdirectory. Then run `ant` without arguments.

The result is a file named `html-toc.jar` in the same directory. 

<a name="sec-contact"> </a>
Contacting the project's team
=============================

You can send a message to the project's team via the
[Contact page](http://www.livitski.com/contact) at <http://www.livitski.com/>
or via *GitHub*. We will be glad to hear from you!

   [StAXform]: https://github.com/StanLivitski/StAXForm
   [TrAX]: http://xml.apache.org/xalan-j/trax.html
   