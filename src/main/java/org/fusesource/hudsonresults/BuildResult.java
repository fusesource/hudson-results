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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BuildResult {
	private String name;
	private String runDate;
	private String result;		// from MatrixRunType.getResult..I think this includes SUCCESS, FAILURE.
	private Integer testsRun;
	private Integer failedTests;
    private Integer duration;
	private String jdk;
	private String platform;	// i.e. Ubuntu, Windows, AIX
    private Integer buildNumber;

    private static final Locale currentLocale =  Locale.getDefault();

	/**
	 * 
	 * @param name
	 * @param runDate
	 * @param jdk
	 * @param platform
	 * @param result
	 * @param testsRun
	 * @param testsFailed
	 */
	public BuildResult(String name, String runDate, String jdk, String platform, String result, int testsRun, int testsFailed, int duration, int buildNumber) {
		this.name = name;
		this.runDate = runDate;
		this.jdk = jdk;
		this.platform = platform;
		this.result = result;
		this.testsRun = testsRun;
		this.failedTests = testsFailed;
        this.duration = duration;
        this.buildNumber = buildNumber;
	}
	
	
	/**
	 *
	 */
	@Override
	public String toString() {
		String s = name + "," + runDate + ", " + jdk + ", " + platform + ", " + getResult() + ", Tests run, " + testsRun
                + ", Failed ," + failedTests + " ,duration, " + getFormattedDuration() + " buildNumber " + buildNumber;
		return s;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getRunDate() {
		return runDate;
	}
	public void setRunDate(String runDate) {
		this.runDate = runDate;
	}
	public String getResult() {
        if (result.equals("SUCCESS")) {
            return result.toLowerCase();
        } else {
		    return result;
        }
	}
	public void setResult(String result) {
		this.result = result;
	}
	public Integer getTestsRun() {
		return testsRun;
	}
	public void setTestsRun(Integer testsRun) {
		this.testsRun = testsRun;
	}
	public Integer getFailedTests() {
		return failedTests;
	}
	public void setFailedTests(Integer failedTests) {
		this.failedTests = failedTests;
	}
	public String getJdk() {
		return jdk;
	}
	public void setJdk(String jdk) {
		this.jdk = jdk;
	}
	public String getPlatform() {
		return platform;
	}
	public void setPlatform(String platform) {
		this.platform = platform;
	}

    public Integer getDuration() {
        return this.duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(Integer buildNumber) {
        this.buildNumber = buildNumber;
    }

    /**
     *
     * @return
     */
    public String getFormattedDuration() {
        int hours = (int) (duration / (60 * 60 * 1000));
        int minutes = (int) (duration / (60 * 1000)) % 60;
        int seconds = (int) (duration / 1000) % 60;
        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }


    /**
     * Run dates look like:  "2013-09-07_00-07-19";  This method returns a shorter formatted
     * date like "Dec 2"
     *
     * @return Formatted date: "SEP 07"
     */
    public String getFormattedRunDate() {
		try {
			int firstDash = runDate.indexOf("-");
			int secondDash = runDate.indexOf("-", firstDash + 1);
			String month = runDate.substring(firstDash + 1, firstDash + 3);
			String day = runDate.substring(secondDash + 1, secondDash + 3);

			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.MONTH, Integer.parseInt(month) - 1);
			cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(day));

			SimpleDateFormat formatter = new SimpleDateFormat("MMM d", currentLocale);
			String result = formatter.format(cal.getTime());
			return result;
		}catch (Exception e) {
			System.out.println(">>>>> Exception " + e.getMessage() + " formatting runDate [" + runDate + "]");
			return " ";
		}
    }
}
