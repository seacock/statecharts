package org.yakindu.sct.standalone.cmdln.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
/**
 * 
 * @author Johannes Dicks - Initial contribution and API
 *
 */
public class CmdLineUtil {
	private static final String PARAM_HELP = "h";
	/**
	 * root package so we will configure logging for all subpackages
	 */
	public static final String LOGGER_ID = "org.yakindu.sct.standalone";
	private static final Logger logger = Logger.getLogger(LOGGER_ID);
	private static final String DEFAULT_LOG_LEVEL = "INFO";
	private static final String LOG_LEVEL_DEVELOPMENT = "DEV";

	public static final String PARAM_LOG = "logLvl";
	public static final String PARAM_LOG_FILE = "logFile";

	public  void configureCustomLogging(CommandLine line) {
		// get log lvl
		String logLevel = null;
		if (line != null && line.hasOption(PARAM_LOG))
			logLevel = line.getOptionValue(PARAM_LOG);
		else
			logLevel = DEFAULT_LOG_LEVEL;
	
		Level level = null;
		PatternLayout layout = new PatternLayout("%5p | %d | %F | %L | %m%n");
		if (logLevel.equals(LOG_LEVEL_DEVELOPMENT)) {
			level = Level.ALL;
			logger.removeAllAppenders();
			logger.addAppender(new ConsoleAppender(layout));
		} else {
			level = Level.toLevel(logLevel);
		}
		initFileAppenderIfConfigured(line, logger, layout);
		logger.setLevel(level);
		logger.info("Logging mode: " + level);
	}

	private  void initFileAppenderIfConfigured(CommandLine line, Logger root, PatternLayout layout) {
		if (line != null && line.hasOption(PARAM_LOG_FILE)) {
			try {
				root.addAppender(new RollingFileAppender(layout, line.getOptionValue(PARAM_LOG_FILE), true));
			} catch (IOException e) {
				System.err.println("Error creating log4j.FileAppender ("+e.getMessage()+")");
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("static-access")
	public  void initOptions(Options options) {
		options.addOption(OptionBuilder.withArgName("logLevel").hasArg()
				.withDescription("Log level:  OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE and ALL").create(PARAM_LOG));
		options.addOption(OptionBuilder.withArgName("logFile").hasArg()
				.withDescription("An absolute path to the log file").create(PARAM_LOG_FILE));
		options.addOption(OptionBuilder.withArgName("help").hasArg(false).isRequired(false)
				.withDescription("Shows help content.").create(PARAM_HELP));
	}

	public  String readInput() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			return br.readLine();
		} catch (IOException e) {
			System.out.println("Error while reading input.");
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// silently...
			}
		}
		return null;
	}

	public  void printHelp(Options options, String app) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printUsage(new PrintWriter(System.out), 80, app, options);
		formatter.printHelp(app, options);
	}

	public  CommandLine parseCmdLine(String[] args, String app, Options options) {
		CommandLine cmd = null;
		try {
			CommandLineParser parser = new BasicParser();
			cmd = parser.parse(options, args);
			configureCustomLogging(cmd);
		} catch (MissingOptionException e) {
			System.err.println(e.getMessage());
			help(options, app);
		} catch (UnrecognizedOptionException e) {
			System.err.println(e.getMessage());
			help(options, app);
		} catch (ParseException e) {
			e.printStackTrace();
			help(options, app);
		}
		return cmd;
	}

	public  void help(Options cmdOptions, String app) {
		printHelp(cmdOptions, app);
	}

	public  boolean containsHelp(String[] args) {
		for (String string : args) {
			if(string.equals("-h"))
				return true;
		}
		return false;
	}
}
