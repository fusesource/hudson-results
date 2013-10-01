/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.hudsonresults;

import generated.hudson.build.ActionsType;
import generated.hudson.build.HudsonTasksJunitTestResultActionType;
import generated.hudson.build.MatrixRunType;
import org.apache.commons.io.FileUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Create a summary of result of the Platform builds on Hudson in a single html page
 * 
 * @author Kevin Earls
 *
 */
public class SummarizeBuildResults {
    private static final String passedTdOpenTag = "<td style=\"background-color: #2E8B57;\">";
    private static final String failedTestsTdOpenTag = "<td style=\"background-color: #ffd700;\">";
    private static final String failedBuildTdOpenTag =  "<td style=\"background-color: #dc143c;\">";
    private static final String tdCloseTag = "</td>";
    public static final String NEW_LINE = "\n";
    private static String hudsonJobsRootName ="/mnt/hudson/jobs";

    // RE to select which test directories we want results from.
    private static final String ACCEPT_STRING_RH_6_1 = ".*6[-\\.]1.*platform";

    // Root of URL to link back to test results
    private static final String REPORT_URL_ROOT="http://ci.fusesource.com/hudson/job/";

    /**
     *
     */
    private static Unmarshaller unmarshaller = null;
	static {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(MatrixRunType.class);
			unmarshaller = jaxbContext.createUnmarshaller();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Use JAXB to create a MatrixRunType object from the file
	 * 
	 * @param buildFileName Name of the build.xml file with results for a particular test run
     * @return JAXB representation of the test result
	 * @throws JAXBException
	 */
	private MatrixRunType getTestSuiteFromFile(String buildFileName) throws JAXBException {
		File buildResultsFile = new File(buildFileName);
		StreamSource source = new StreamSource(buildResultsFile);
		JAXBElement<MatrixRunType> root = unmarshaller.unmarshal(source, MatrixRunType.class); 
		return root.getValue(); 
	}


	/**
	 * Return the latest build in a given build directory
	 * 
	 * @param targetDirectory Something like: cxf-2.6.0.fuse-7-1-x-stable-platform/configurations/axis-jdk/jdk6/axis-label/ubuntu/builds/
	 * @return cxf-2.6.0.fuse-7-1-x-stable-platform/configurations/axis-jdk/jdk6/axis-label/ubuntu/builds/2012-11-02_21-09-35
	 */
    private File getLatestBuildDirectory(File targetDirectory) throws IOException {
        if (targetDirectory != null && targetDirectory.listFiles() != null) {
            List<File> fud = Arrays.asList(targetDirectory.listFiles());

            // Remove symlinks, which are just numbered.  We want to be able to use the date.
            List<File> contents = new ArrayList<>();
            for (File f : fud) {
                if (!FileUtils.isSymlink(f)) {
                    contents.add(f);
                }
            }

            Collections.sort(contents, new BuildDirectoryComparator());
            Collections.reverse(contents);

            if (!contents.isEmpty()) {
                File lastBuildDirectory = contents.get(0);
                String latestBuildFileName = lastBuildDirectory.getAbsolutePath() + "/build.xml";
                File lastBuildFile = new File(latestBuildFileName);
                if (lastBuildFile.exists()) {
                    return lastBuildDirectory;
                } else if (contents.size() > 1) {
                    System.out.println(">>>> Couldn't find [" + latestBuildFileName + "] using " + contents.get(2));
                    return contents.get(1);
                }
            }
        }

        return null;
    }


	/**
	 * Return a list of platform test result directories matching the directoryMatchExpression.
     * For example, to get all 6.1 platform results, use ".*6[-\\.]1.*platform"
	 * 
	 * @param hudsonJobsRoot the root of the Hudson jobs directory, i.e. /mnt/hudson/jobs
     * @param directoryMatchExpression regular expression for selecting target directories
	 */
	private List<File> getPlatformDirectories(File hudsonJobsRoot, String directoryMatchExpression) {
		PlatformDirectoryFilter pdf = new PlatformDirectoryFilter(directoryMatchExpression);
        File[] files = hudsonJobsRoot.listFiles(pdf);
		List<File> directories = Arrays.asList(files);
        Collections.sort(directories);
		
		return directories;
	}

    /**
     * Write the report to a file.
     *
     * @param writer FileWriter set to desired output file
     *               @param allResults Map of the results from desired tests
     * @throws JAXBException
     * @throws IOException
     */
    public void createHTMLSummary(FileWriter writer, Map<String, List<BuildResult>> allResults) throws JAXBException, IOException {
        writer.write("<html>" + NEW_LINE);
        writer.write("<body>" + NEW_LINE);
        String style = "<style><!--\n" +
                "table { border-collapse: collapse; font-family: Futura, Arial, sans-serif; } caption { font-size: larger; margin: 1em auto; } th, td { padding: .65em; } th, thead { background: #000; color: #fff; border: 1px solid #000; } td { border: 1px solid #777; }\n" +
                "--></style>";
        writer.write(style + NEW_LINE);
        writer.write("<table border=\"1\">"  + NEW_LINE);
        writer.write("<caption>JBoss Fuse 6.1 Platform Test Results as of " + new Date().toString() + "</caption>" + NEW_LINE);
        printHtmlHeaders(writer);

        // Write one row for each project
        List<String> projectNames = new ArrayList<>(allResults.keySet());
        Collections.sort(projectNames);
        for (String projectName : projectNames) {
            writer.write("    <tr>");
            List<BuildResult> buildResults = allResults.get(projectName);
            Collections.reverse(buildResults);
            Collections.sort(buildResults, new BuildResultComparator());

            writer.write("<td>" + projectName + "</td>");
            for (PLATFORM platform : PLATFORM.values()) {
                for (JDK jdk : JDK.values()) {
                    for (BuildResult br: buildResults) {
                        if (platform.equals(br.getPlatform()) && jdk.equals(br.getJdk()) && !(jdk.equals(JDK.jdk6) && platform.equals(PLATFORM.rhel))) {
                            String linkToResultsPage = REPORT_URL_ROOT + projectName + "/" + br.getBuildNumber() + "/" + "jdk=" + jdk + ",label=" + platform + "/";

                            String testResult = "<a href=\"" + linkToResultsPage + "\">" + br.getFailedTests() + "/" + br.getTestsRun() + "</a>"
                                    + "<br/><small>(" + br.getFormattedDuration() + ")</small>";    // TODO this needs to be smaller.
                            if (br.getResult().equalsIgnoreCase("success")) {
                                writer.write(passedTdOpenTag + testResult + tdCloseTag);
                            } else if (br.getTestsRun().equals(0)) {
                                writer.write(failedBuildTdOpenTag + testResult + tdCloseTag);
                            } else {
                                writer.write(failedTestsTdOpenTag + testResult + tdCloseTag);
                            }
                        }
                    }

                }
            }
            writer.write("</tr> " + NEW_LINE);
        }

        writer.write("<table>" + NEW_LINE);
        writer.write("<br/>" + NEW_LINE);
        writer.write("<p>Red cells indicate build failures, yellow cells indicate builds with test failures, green cells indicate successful builds.  ");
        writer.write("Cell results N/M show N test failures out of M tests run</p>" + NEW_LINE);
        writer.write("<p></p>" + NEW_LINE);
        writer.write("</body>" + NEW_LINE);
        writer.write("</html>" + NEW_LINE);
        writer.close();
    }

    /**
     *
     * @return A FileWriter set to wherever we want to write the report
     * @throws IOException
     */
    private FileWriter getResultFileWriter() throws IOException {
        File resultsDir = new File("results");
        resultsDir.mkdir();
        String resultsFileName = "results/results.html";
        return new FileWriter(resultsFileName);
    }

    /**
     * Print the headers for the HTML summary
     */
    private void printHtmlHeaders(FileWriter writer) throws IOException {
        // Print headers
        writer.write("<thead>");
        writer.write("<tr>");
        writer.write("<td>Platform</td>");
        for (PLATFORM platform : PLATFORM.values()) {
            for (JDK jdk : JDK.values()) {
                if (!(jdk.equals(JDK.jdk6) && platform.equals(PLATFORM.rhel))) {
                    writer.write("<td>" + platform + " " + jdk + "</td>");
                }
            }
        }
        writer.write("</tr>");
        writer.write("</thead>" + NEW_LINE);
    }

    /**
     * @param hudsonJobsRoot the root of the Hudson jobs directory, i.e. /mnt/hudson/jobs
     * @param directoryMatchExpression regular expression to select only tests we want
     * Map of all test results
     */
    private Map<String, List<BuildResult>> getAllResults(File hudsonJobsRoot, String directoryMatchExpression) throws IOException{
        Map<String, List<BuildResult>> allResults = new HashMap<>();
        List<File>platformDirectories = getPlatformDirectories(hudsonJobsRoot, directoryMatchExpression);
        for (File platformDirectory : platformDirectories) {
            for (PLATFORM platform : PLATFORM.values()) {
                for (JDK jdk : JDK.values()) {
                    String targetDirectoryName = platformDirectory.getAbsolutePath() + "/configurations/axis-jdk/" + jdk + "/axis-label/" + platform + "/builds/";
                    File latestBuildDirectory = getLatestBuildDirectory(new File(targetDirectoryName));
                    if (latestBuildDirectory != null) {
                        try {
                            String buildDateTime = latestBuildDirectory.getName(); 	// directory name of the build is date time in the format 2012-11-02_21-09-35
                            String latestBuildFileName = latestBuildDirectory.getAbsolutePath() + "/build.xml";
                            MatrixRunType mrt = getTestSuiteFromFile(latestBuildFileName);  // Gets FileNotFoundException...can we get second oldest here?
                            ActionsType actions = mrt.getActions();
                            HudsonTasksJunitTestResultActionType junitResults = actions.getHudsonTasksJunitTestResultAction();

                            BuildResult buildResult;
                            if (junitResults != null) {
                                buildResult = new BuildResult(platformDirectory.getName(),  buildDateTime, jdk, platform,
                                        mrt.getResult(), junitResults.getTotalCount(), junitResults.getFailCount(), mrt.getDuration(), mrt.getNumber());
                            } else {
                                buildResult = new BuildResult(platformDirectory.getName(),  buildDateTime, jdk, platform, mrt.getResult(), 0, 0, 0, mrt.getNumber());
                            }
                            // TODO need to store by platformDirectory.getName() (which is projectname) jdk, platform
                            List<BuildResult> platformResults = allResults.get(platformDirectory.getName());
                            if (platformResults == null) {
                                platformResults = new ArrayList<>();
                                allResults.put(platformDirectory.getName(), platformResults);
                            }
                            platformResults.add(buildResult);
                        } catch(Exception e) {
                            e.printStackTrace();
                            System.err.println("************ Exception " + e.getMessage() + " on " + latestBuildDirectory.getAbsolutePath());
                        }

                    }
                }

            }
        }
        return allResults;
    }


	/**
	 * @param args optional command line args
	 * @throws JAXBException 
	 */
	public static void main(String[] args) throws JAXBException, IOException {
        hudsonJobsRootName="/mnt/hudson/jobs";
        String directoryMatchExpression = ACCEPT_STRING_RH_6_1;

		if (args.length > 0) {
            hudsonJobsRootName = args[0];
            if (args.length > 1) {
                directoryMatchExpression = args[1];

            }
		} 

		System.out.println("Starting at " + hudsonJobsRootName + " matchings on [" + directoryMatchExpression + "]");
		SummarizeBuildResults me = new SummarizeBuildResults();
		File theRoot = new File(hudsonJobsRootName);
        File hudsonJobsRoot = new File(hudsonJobsRootName);
        FileWriter writer = me.getResultFileWriter();
        Map<String, List<BuildResult>> allResults = me.getAllResults(hudsonJobsRoot, directoryMatchExpression);
        me.createHTMLSummary(writer, allResults);
	}
}


/**
 * Filter to select directories which contain desired results
 *
 * @author kearls
 *
 */
class PlatformDirectoryFilter implements FileFilter {
    String matchRegularExpression = "";

    public PlatformDirectoryFilter(String target) {
        this.matchRegularExpression = target;
    }

    @Override
	public boolean accept(File pathname) {
		String name = pathname.getName();
        return name.matches(matchRegularExpression);
	}
}


/**
 * Comparator to help find the newest subdirectory in the builds directory
 * @author kearls
 *
 */
class BuildDirectoryComparator implements Comparator<File> {
    @Override
	public int compare(File first, File second) {
		if (first.lastModified() > second.lastModified()) {
			return 1;
		} else {
			return -1;
		}
	}
}


/**
 * Comparator to sort a list of builds results on name, platform, and jdk
 * @author kearls
 *
 */
class BuildResultComparator implements Comparator<BuildResult> {
	@Override
	public int compare(BuildResult b1, BuildResult b2) {
		int nameValue = b1.getName().compareTo(b2.getName());
		if (nameValue != 0) {
			return nameValue;
		} else {
			int platformValue = b1.getPlatform().compareTo(b2.getPlatform());
			if (platformValue != 0) {
				return platformValue;
			} else {
				return b1.getJdk().compareTo(b2.getJdk());
			}
		}
	}
	
}