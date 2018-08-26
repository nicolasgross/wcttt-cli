# WCT³ Core

WCT³ (WIAI Course Timetabling Tool) is a software that strives to automate the 
timetabling process at the WIAI faculty of the University of Bamberg. It was 
developed by [Nicolas Gross](https://github.com/nicolasgross) as part of his 
bachelor thesis at the Software Technologies Research Group (SWT).

This is the Core module of WCT³. It comprises the implementations of the 
algorithms as well as a command line interface to be able to run them without 
using a GUI.


## Dependencies

- Oracle JDK 10
- maven
- libwcttt


## Build

1. Install libwcttt in the local maven repository
2. `cd <path-to-project-root>`
3. `mvn package` 
4. (Optional, installs WCT³ Core in the local maven repository which is required
to build WCT³ GUI)  
`mvn install`


## Run

`java -jar <path-to-project-root>/target/wcttt-core.jar`


## License

This software is released under the terms of the GPLv3.
