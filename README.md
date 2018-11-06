SCViewer
========

SCViewer is composed of a set of eclipse plugins to display VCD and transaction streams 
created by the SystemC VCD trace implementation and the SystemC Verification Library (SCV).
For further description of the SCV please refer to 
http://www.accellera.org/activities/committees/systemc-verification.

> If you encounter issue when running on Linux please try running as `SWT_GTK3=0 scviewer` as there exist issues wiht GTK3.

The viewer has the following features
- support of VCD files (compressed and uncompressed)
 - real numbers
 - showing vectors and real numbers as analog (step-wise & continuous)
 - various value representations of bit vectors
- support of SCV transaction recordings in various formats
 - text log files (compressed and uncompressed)
 - sqlite based 
 - visualization of transaction relations

To build the plugins the Eclipse SDK or PDE can be used. In both cases the Groovy
eclipse plugin (http://groovy.codehaus.org/Eclipse+Plugin or Market) has to be
installed.

TODO
====
- add more tests
- move to feature based product to allow automatic updates
- improve graphics
- catch-up e3 plugin to functionality of e4 product
- add calculated traces
