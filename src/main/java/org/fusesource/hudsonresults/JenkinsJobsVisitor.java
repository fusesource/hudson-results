package org.fusesource.hudsonresults;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Created by kearls on 4/11/14.
 */
public class JenkinsJobsVisitor<T> extends SimpleFileVisitor<T> {
    Map<String, Set<String>> axes = new HashMap<>();      // Maps labels (i.e. rhel or windows) to jdks used with that label.
    Set<String> testSuiteNames = new HashSet<>();         // name of all test suites we're interested in.
    Set<String> jdks = new HashSet<>();                   // All jdks found
    Set<String> labels = new HashSet<>();                  // All labels found.  Not sure if we need jdks and labels; maybe we just need axes?
    private String directoryMatchExpression;

    public JenkinsJobsVisitor(String directoryMatchExpression) {
        this.directoryMatchExpression = directoryMatchExpression;
    }

    @Override
    public FileVisitResult visitFile(T file, BasicFileAttributes attributes) {
        return FileVisitResult.CONTINUE;
    }

    @Override
    /**
     * // Looks like: /home/jenkins/jobs/smx4-specs-2.3.0.redhat-6-1-x-stable-platform/configurations/axis-jdk/jdk6/axis-label/rhel ; watch out for
     // /home/jenkins/jobs/smx4-specs-2.3.0.redhat-6-1-x-stable-platform/configurations/axis-jdk/jdk6/axis-label
     */
    public FileVisitResult preVisitDirectory(T dir, BasicFileAttributes attrs) throws IOException {
        Path directoryPath = (Path) dir;
        File directory = directoryPath.toFile();
        String fileName = directory.getAbsolutePath();
        if ((fileName.contains("axis-jdk") && fileName.contains("axis-label")) && !fileName.endsWith("axis-label") ) {
            List<String> parts = Arrays.asList(fileName.split("/"));
            int jdkIndex = parts.indexOf("axis-jdk") + 1;
            int labelIndex = parts.indexOf("axis-label") + 1;
            String label = parts.get(labelIndex);
            String jdk = parts.get(jdkIndex);
            String testSuiteName = parts.get(jdkIndex - 3);

            testSuiteNames.add(testSuiteName);
            if (!testSuiteName.matches(directoryMatchExpression)) {
                return FileVisitResult.CONTINUE;
            }
            jdks.add(jdk);
            labels.add(label);

            if (!axes.containsKey(label)) {
                Set<String> jdksForLabel = new HashSet<>();
                jdksForLabel.add(jdk);
                axes.put(label, jdksForLabel);
            } else {
                Set<String> jdksForLabel = axes.get(label);
                jdksForLabel.add(jdk);
            }
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(T dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    public Set<String> getTestSuiteNames() {
        return testSuiteNames;
    }

    public Set<String> getJdks() {
        return jdks;
    }

    public Set<String> getLabels() {
        return labels;
    }

    public  Map<String, Set<String>> getAxes() {
        return axes;
    }
}

