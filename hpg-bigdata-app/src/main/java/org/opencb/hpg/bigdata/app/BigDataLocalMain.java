package org.opencb.hpg.bigdata.app;

import com.beust.jcommander.ParameterException;
import org.opencb.hpg.bigdata.app.cli.hadoop.CliOptionsParser;
import org.opencb.hpg.bigdata.app.cli.CommandExecutor;
import org.opencb.hpg.bigdata.app.cli.hadoop.ConvertCommandExecutor;

/**
 * Created by hpccoll1 on 18/05/15.
 */
public class BigDataLocalMain {

    public static void main(String[] args) {
        CliOptionsParser cliOptionsParser = new CliOptionsParser();

        if (args == null || args.length == 0) {
            cliOptionsParser.printUsage();
        }

        try {
            cliOptionsParser.parse(args);
        } catch(ParameterException e) {
            System.out.println(e.getMessage());
            cliOptionsParser.printUsage();
            System.exit(-1);
        }


        String parsedCommand = cliOptionsParser.getCommand();
        if (parsedCommand == null || parsedCommand.isEmpty()) {
            if (cliOptionsParser.getGeneralOptions().help) {
                cliOptionsParser.printUsage();
                System.exit(0);
            }
            if (cliOptionsParser.getGeneralOptions().version) {
                BigDataMain.printVersion();
            }
        } else {
            CommandExecutor commandExecutor = null;
            switch (parsedCommand) {
                case "convert":
                    if (cliOptionsParser.getConvertCommandOptions().commonOptions.help) {
                        cliOptionsParser.printUsage();
                    } else {
                        commandExecutor = new ConvertCommandExecutor(cliOptionsParser.getConvertCommandOptions());
                    }
                    break;
                default:
                    break;
            }

            if (commandExecutor != null) {
                try {
                    commandExecutor.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
    }
}
