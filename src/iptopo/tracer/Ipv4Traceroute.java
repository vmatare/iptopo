package iptopo.tracer;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ipv4Traceroute extends Tracer {

	private final static String IPV4_REGEX = "([1-9][0-9]{0,2})(?:\\.)" +
		"([0-9]{1,3})(?:\\.)([0-9]{1,3})(?:\\.)([1-9][0-9]{0,2})";

	private String[] program;
	private Process traceProc;
	private Pattern linePattern, ipv4Pattern;
		
	public Ipv4Traceroute(String root, TracerSettings settings) throws RuntimeException, UnknownHostException {
		super(root, settings);
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add("/usr/bin/traceroute");
		cmd.add("-n");
		if (settings.getProto() != null) {
			switch (settings.getProto()) {
			case ICMP:
				cmd.add("-I");
				break;
			case TCP:
				cmd.add("-T");
				break;
			case UDP:
				// UDP is default
				break;
			}
			if (settings.getPort() > 0)
				cmd.add("-p " + Integer.toString(settings.getPort()));
		}
		if (settings.getStartTTL() > 0)
			cmd.add("-f " + Integer.toString(settings.getStartTTL()));
		if (settings.getMaxTTL() > 0)
			cmd.add("-m " + Integer.toString(settings.getMaxTTL()));
		else settings.setMaxTTL(30); // set it to traceroute's default for completeness
		cmd.add(settings.getTarget().getHostAddress());
		program = new String[cmd.size()];
		for (int i = 0; i < cmd.size(); i++)
			program[i] = cmd.get(i);
		linePattern = Pattern.compile("^\\s*[0-9]+\\s+.*");
		ipv4Pattern = Pattern.compile(IPV4_REGEX);
	}
	
	
	
	/**
	 * runs the external traceroute program with the settings that
	 * have been specified in the constructor
	 * 
	 * @throws RuntimeException
	 */
	public void trace() {
		BufferedReader stdout, stderr;
		String line, errLine;
		String errorOutput = "";
		Matcher lineMatcher, ipMatcher;
		
		try {
			ProcessBuilder pb = new ProcessBuilder(program);
			traceProc = pb.start();
			stdout = new BufferedReader(new InputStreamReader(traceProc.getInputStream()));
			stderr = new BufferedReader(new InputStreamReader(traceProc.getErrorStream()));
			errLine = "";
			line = "";
			do  {
				if (stderr.ready()) {
					errLine = stderr.readLine();
					errorOutput += errLine + "\n";
				}
				else {
					errLine = null;
				}
				if ((line = stdout.readLine()) != null) {
					lineMatcher = linePattern.matcher(line);
					ipMatcher = ipv4Pattern.matcher(line);
					if (lineMatcher.find()) {
						if (ipMatcher.find()) {
							addHop(ipMatcher.group());
						}
						else {
							addHop("0");
						}
					}
				}
			} while (!Thread.interrupted() && (errLine != null || line != null));
			if (Thread.interrupted()) traceProc.destroy();
			traceProc.waitFor();

			if (traceProc.exitValue() != 0) {
				System.err.println("Error: " + Arrays.toString(program) + " returned " + traceProc.exitValue());
				System.err.print(errorOutput);
			}
		} catch (InterruptedException e) {
			;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	}

