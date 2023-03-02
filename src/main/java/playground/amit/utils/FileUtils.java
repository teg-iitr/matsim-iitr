/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.amit.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.mixedTraffic.patnaIndia.input.joint.JointCalibrationControler;

import java.io.File;

/**
 * I think, after introduction of URL and for uniformity, pass absolute path.
 * Because, relative paths are converted to new uri and then url using new File(" ").getAbsoluteFile() rather than
 * new File(" ").getCanonicalFile(); which eventually contains (../..) in the file path. see toURL() of {@link java.io.File}.
 *
 * Created by amit on 26/09/16.
 */


public final class FileUtils {

    public static final Logger LOGGER = LogManager.getLogger(FileUtils.class);

    public static final String RUNS_SVN = System.getProperty("user.name").equalsIgnoreCase("amit") ? "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/project_data/" : "../../runs-svn/";
    public static final String SHARED_SVN = System.getProperty("user.name").equalsIgnoreCase("amit") ? "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/project_data/" : "../../shared-svn/";
    public static final String GNU_SCRIPT_DIR = "../agarwalamit/src/main/resources/gnuplot/";

    public static final String SVN_PROJECT_DATA_DRIVE = System.getProperty("user.name").equalsIgnoreCase("Amit") ? "C:/Users/Amit/Documents/svn-repos/shared/data/project_data/" : "C:/Users/Nidhi/Documents/svn-repos/shared/data/project_data/";


    /*
    * To delete all intermediate iteration except first and last.
    */
    public static void deleteIntermediateIterations(final String outputDir, final int firstIteration, final int lastIteration) {
        for (int index =firstIteration+1; index <lastIteration; index ++){
            String dirToDel = outputDir+"/ITERS/it."+index;
            if (! new File(dirToDel).exists()) continue;
            LogManager.getLogger(JointCalibrationControler.class).info("Deleting the directory "+dirToDel);
            IOUtils.deleteDirectoryRecursively(new File(dirToDel).toPath());
        }
    }

    public static String getLocalGDrivePath(){
        String userName = System.getProperty("user.name");
        if(userName.equalsIgnoreCase("amit")){
            return "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/";
        }
        else if (userName.equalsIgnoreCase("Nidhi")){
            return "C:/Users/Nidhi/Documents/iitr_gmail_drive/";

        }
        else{
            throw new RuntimeException("No Google Drive Folder is defined for user "+userName);
        }
    }
}
