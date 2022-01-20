package com.sanger.overlapSnifflesPindel;

import java.io.*;
import java.util.*;
import com.beust.jcommander.*;
import com.beust.jcommander.validators.PositiveInteger;

public class overlapSnifflesPindel {
	private static String versionNumber = "0.1";
	@Parameter
	private List<String> parameters = new ArrayList<String>();
	
	@Parameter(names = "--input-filtered-sniffles", description = "Input filtered Sniffles file.", required = true, converter = FileConverter.class, validateWith = FileValidator.class, order=0)
	public File input_sniffles = null;
	
	@Parameter(names = "--input-pindel", description = "Input Pindel file.", required = true, converter = FileConverter.class, validateWith=FileValidator.class, order=1)
	public File input_pindel = null;
	
	@Parameter(names = "--illumina-bam", description = "Input Illumina BAM (BWA-mem aligned).", required = true, converter = FileConverter.class, validateWith=FileValidator.class, order=2)
	public File bam = null;
	
	@Parameter(names = "--output-file", description = "Output file to store results.", required = true, order=3)
	public String output_file = null;
	
	@Parameter(names = "--minimum-width", description = "Minimum window width (default: 20).", validateWith = PositiveInteger.class, order=4)
	public Integer minimum_width = 20;
	
	@Parameter(names = "--overlap-fraction", description = "Key fraction for determining window size (default: 0.05).", validateWith = PositiveDoubleValidator.class, converter = PositiveDoubleConverter.class, order=5)
	public Double oFrac = 0.05;
	
	@Parameter(names = "--maximum-del-size", description = "Maximum size of deletion (default: 1000).", validateWith = PositiveInteger.class, order=6)
	public Integer max_del = 1000;
	
	@Parameter(names = "--extract-width", description = "Padding of extraction window (default: 50).", validateWith = PositiveInteger.class, order=7)
	public Integer ew = 50;
	
	@Parameter(names = "--threads", description = "Number of threads.", validateWith = PositiveInteger.class, order=8)
	public Integer threads = 1;
	
	@Parameter(names = {"--help","-help"}, help = true, description = "Get usage information", order=9)
	private boolean help;
	
	@Parameter(names = {"--version","-version"}, description = "Get current version", order=10)
	private Boolean version = null;
	
	public static void main(String[] args) {
		var osp  = new overlapSnifflesPindel();
		var jCommander = new JCommander(osp);
		jCommander.setProgramName("overlapSnifflesPindel.jar");
		JCommander.newBuilder().addObject(osp).build().parse(args);
		if(osp.version != null && osp.version) {
			System.out.println("Determine overlap in small indels between Sniffles and Pindel: " + versionNumber);
			System.exit(0);
		}
		else if(osp.help) {
			jCommander.usage();
			System.exit(0);
		} else  {
			var nThreads = Runtime.getRuntime().availableProcessors();
			if(osp.threads > nThreads)
				System.out.println("Warning: Number of threads exceeds number of available cores");
			new overlapSnifflesPindelCore(osp.input_sniffles, osp.input_pindel, osp.bam, osp.output_file, osp.minimum_width, osp.oFrac, osp.max_del, osp.ew, osp.threads);	
		}
	}
}
