# WCT³-CLI

WCT³ (WIAI Course Timetabling Tool) is a software that strives to automate the 
timetabling process at the WIAI faculty of the University of Bamberg. It was 
developed by [Nicolas Gross](https://github.com/nicolasgross) as part of his 
bachelor thesis at the Software Technologies Research Group (SWT).

This is the CLI module of WCT³. It comprises a command line interface to be 
able to run the algorithms without using a GUI.


## Notice

Because of an unresolved issue in the JAXB framework, warning messages are
printed to stdout if a XML file is parsed/written. The corresponding issue on 
GitHub can be found [here](https://github.com/javaee/jaxb-v2/issues/1197).


## Run

### Requirements

- Oracle JDK or OpenJDK, version 11 or higher
- OpenJFX 11
- maven
- libwcttt

### Steps

1. Install libwcttt in the local maven repository
2. `cd PATH_TO_PROJECT_ROOT`
3. `mvn clean compile`
4. `mvn exec:java -Dexec.args="PATH_TO_SEMESTER_DATA"`


## License

This software is released under the terms of the GPLv3.
