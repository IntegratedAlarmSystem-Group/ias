# IAS build with Waf

During the development of IAS v12 we found that scala is not supporting ant anymore for this reason we hd to 
to find an alternative building tool.

Scala native build tools did not seem to us a good choice because they might change without
notice and would require a continuous maintenance effort. 
On top of that, the IAS is not a scala project but multi-language as it supports also python and java.

For this reason we decided to go for onother build tool and selected waf.

Here we describe how to install and use Waf in for the IAS. 

# Waf installation

1. Download the binaries of Waf from the website and install them in `/opt/waf`.
At the first execution waf unpack itself producing the python executables into `/opt/waf/.waf-...`
1. Ensure to have the waf executable `/opt/waf/waf` in the `$PATH` for example setting an alias.

# Waf build procedure

Waf is used to build the entire IAS or to build one of the module.
For building the whole IAS, you have to run waf from the IAS source root folder (normally called `ias`).
To run the build of a module, Waf must be run from the top folder of the module like `ias/BasicTypes`.

The scripts to customize the build are called `wscript`. 
The IAS provide a build system, composed of python classes in the `ias/WafBuildTools` module.
Waf accesses the python sources in this module using an absolute path by executing `conf.load` 
like for example 
```python
conf.load('IasWafBuildTools', tooldir='WafBuildTools/src/main/python/')
``` 

The top level `wscript` delegates the build of each module (by means of waf recurse).
The build of each module puts all the produced files in the build folder of the module itself.
So for example jar files produced by compiling scala/java sources will all be saved in `build/lib`
like `ias/BasicTypes/build`.




  