package de.d3v0.peb;

import de.d3v0.peb.common.IOProperties.FileHandlerProperties;
import de.d3v0.peb.common.Properties;
import de.d3v0.peb.common.dbhelper.SqliteProperties;
import de.d3v0.peb.common.sourceproperties.SourceProperties;
import de.d3v0.peb.common.IOProperties.FtpHandlerProperties;
import de.d3v0.peb.controller.MainHandler;
import org.apache.commons.cli.*;

import java.util.Hashtable;


public class Main {

    public static void main(String[] args)
    {
        Options options = new Options();
        Option workingDirOpt = new Option("w", "workingDirectory", true, "local path to working directory");
        workingDirOpt.setRequired(true);
        options.addOption(workingDirOpt);

        Option createNewOpt = new Option("c", "createNew", false, "create totally fresh Backup; discard probably existing backup at target location");
        options.addOption(createNewOpt);

        Option sampleOpt = new Option("e", "exampleConfig", false, "create an Example Config in WorkingDir");
        options.addOption(sampleOpt);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try
        {
            cmd = parser.parse(options, args);

        } catch (ParseException e)
        {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }

        if (cmd.hasOption(sampleOpt.getLongOpt()))
        {
            CreateSampleConfig();
            return;
        }

        String workingDir = cmd.getOptionValue(workingDirOpt.getLongOpt());
        boolean createNew = cmd.hasOption(createNewOpt.getLongOpt());

        Run(workingDir, createNew);
    }

    private static void Run(String wokringDir, boolean createNew) {
        try
        {
            MainHandler handle = new MainHandler(wokringDir, createNew);
            handle.performBackup();
        }
        catch(Exception e)
        {
            System.out.println("ouch");
        }
    }

    private static void CreateSampleConfig()
    {
        Properties props = new Properties();
        props.dbProperties = new SqliteProperties();
        props.sourceProperties = new SourceProperties[1];
        props.sourceProperties[0] = new SourceProperties();
        props.sourceProperties[0].IOProperties = new FileHandlerProperties();
        props.sourceProperties[0].IOProperties.BasePath = "/home/user/";
        props.targetProperties= new FtpHandlerProperties();
        props.targetProperties.BasePath = "/backup";
        ((FtpHandlerProperties)props.targetProperties).HostName = "backup.ftp.local";
        ((FtpHandlerProperties)props.targetProperties).Password = "password";
        ((FtpHandlerProperties)props.targetProperties).UserName = "username";
        props.workingDir = "/home/user/workingDir";

        try
        {
            Properties.SaveConfig(props);
        }
        catch(Exception e)
        {
            props.workingDir = "";
        }
    }

}
