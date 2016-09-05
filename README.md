hudson-results
==============
This is a utility for producing an HTML Summary of our Hudson platform build results.

To run:

    mvn clean jaxb2:xjc install
    mvn exec:java -Dexec.mainClass=org.fusesource.hudsonresults.SummarizeBuildResults -Dexec.args="/mnt/hudson/jobs .*6[-\.]1.*platform"

The first argument is the location of Hudson's jobs directory.  The second is a regular expression for project directories that we want to report on.





