package com.sanger.overlapSnifflesPindel;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import java.util.zip.*;
import java.util.function.*;
import java.nio.charset.Charset;
import java.nio.file.*;

import com.sanger.intervalTree.*;

import htsjdk.samtools.*;

import static java.lang.System.*;

public class overlapSnifflesPindelCore {
	private String header = null;
	public overlapSnifflesPindelCore(File input_sniffles, File input_pindel, File bam, String oFile, Integer min_window, Double oFrac, Integer max_indel, Integer ew, Integer threads) {
		try {
			out.println("Retrieving Pindel results");
			var pindelMap = retrievePindelResults(input_pindel);
			out.println("Retrieving SVs from filtered Sniffles file");
			var snifflesList = retrieveSnifflesResults(input_sniffles, oFrac, min_window, max_indel);
			out.println("Find overlaps");
			findOverlap(snifflesList, pindelMap, threads);
			out.println("Find supplementary alignment evidence");
			findSupplementary(snifflesList, bam, ew, threads);
			out.println("Write results");
			writeResults(oFile, snifflesList);
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
	}
	private void writeResults(String output, ArrayList<svInfo> sl) throws IOException {
		var data = new ArrayList<String>(Arrays.asList(header));
		data.addAll(sl.stream().map(i -> i.getLine()).collect(Collectors.toCollection(ArrayList::new)));
		Files.write(Paths.get(output), data, Charset.defaultCharset());
	}
	private HashMap<String, IntervalST<svInfo>> retrievePindelResults(File pindel) throws IOException {
		if(isGzipped(pindel))
			return GZIPFiles.lines(pindel.toPath()).filter(s -> !s.startsWith("#")).filter(s -> s.contains("PC=D")).map(s -> s.split("\t", 6)).map(i -> new svInfo(i, i[0], Integer.parseInt(i[1]), Integer.parseInt(i[1]) + i[3].length() - i[4].length(), -1.0)).collect(Collectors.groupingBy(svInfo::getChrom, HashMap::new, new svCollector()));
		return Files.lines(pindel.toPath()).filter(s -> !s.startsWith("#")).filter(s -> s.contains("PC=D")).map(s -> s.split("\t", 6)).map(i -> new svInfo(i, i[0], Integer.parseInt(i[1]), Integer.parseInt(i[1]) + i[3].length() - i[4].length(), -1.0)).collect(Collectors.groupingBy(svInfo::getChrom, HashMap::new, new svCollector()));
	}
	private ArrayList<svInfo> retrieveSnifflesResults(File input_sniffles, double oFrac, int minWin, int maxWin) throws IOException {
		Supplier<Stream<String>> streamSupplier = () -> {try{return Files.lines(input_sniffles.toPath());}catch(IOException e){e.printStackTrace();} return null;};
		header = String.join("\t", streamSupplier.get().findFirst().get(), "Found in Pindel");
		return streamSupplier.get().skip(1).map(i -> i.split("\t")).filter(i -> i[5].equals("DEL")).filter(i -> Math.abs(Integer.parseInt(i[4])) <= maxWin).map(i -> new svInfo(i, i[1], Integer.parseInt(i[2]), Integer.parseInt(i[3]), Math.min(Math.max(Math.abs(Double.parseDouble(i[4])) * oFrac, minWin), maxWin))).collect(Collectors.toCollection(ArrayList::new));
	}
	private void findOverlap(ArrayList<svInfo> sl, HashMap<String, IntervalST<svInfo>> pm, int threads) {
		var forkJoinPool = new ForkJoinPool(threads);
		try {
			forkJoinPool.submit(() -> sl.parallelStream().forEach(i -> overlap(i, pm))).get();
		} catch(InterruptedException | ExecutionException e) {
			e.printStackTrace();
			System.exit(-2);
		}
	}
	private void overlap(svInfo current, HashMap<String, IntervalST<svInfo>> pm) {
		var chromTree = pm.get(current.getChrom());
		var query = chromTree.searchAllList(current.searchInterval());
		if(query.size() > 0) {
			var svs = query.stream().map(i -> chromTree.get(i)).collect(Collectors.toCollection(ArrayList::new));
			current.setFound(svs.stream().filter(i -> current.fits(i)).count() > 0);
		} else
			current.setFound(false);
	}
	private void findSupplementary(ArrayList<svInfo> sl, File bam, int ew, int threads) {
		var forkJoinPool = new ForkJoinPool(threads);
		try {
			forkJoinPool.submit(() -> sl.parallelStream().filter(i -> !i.getFound()).forEach(i -> i.setFound(supplementary(i, bam, ew)))).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			System.exit(-3);
		}
	}
	private boolean supplementary(svInfo sv, File bam, int ew) {
		var inputSam = SamReaderFactory.make().enable(SamReaderFactory.Option.DONT_MEMORY_MAP_INDEX).validationStringency(ValidationStringency.LENIENT).samRecordFactory(DefaultSAMRecordFactory.getInstance()).open(bam);
		SAMRecordIterator it = null;
		SAMRecord current = null;
		try {
			it = inputSam.query(sv.getChrom(), sv.getStart() - ew, sv.getEnd() + ew, false);
			while(it.hasNext()) {
				current = it.next();
				if(hasSupplementary(current) && fitSupplementary(sv, current)) {
					inputSam.close();
					return true;
				}
			}
			inputSam.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-4);
		}
		return false;
	}
	private boolean hasSupplementary(SAMRecord read) {
		var sa = read.getStringAttribute("SA");
		return !(sa == null || sa.equals(""));
	}
	private boolean fitSupplementary(svInfo sv, SAMRecord read) {
		var searchList = new ArrayList<search>(100);
		var sa = read.getStringAttribute("SA");
		boolean fitLeft = false, fitRight = false;
		searchList.add(new search(read.getReferenceName(), read.getAlignmentStart(), read.getAlignmentEnd()));
		Arrays.stream(sa.split(";")).map(i -> i.split(",")).forEach(i -> searchList.add(new search(i[0], Integer.parseInt(i[1]), Integer.parseInt(i[1]) + TextCigarCodec.decode(i[3]).getReferenceLength())));
		for(final search el : searchList) {
			if(Math.abs(sv.getStart() - el.start) <= sv.getSearchWidth() || Math.abs(sv.getStart() - el.end) <= sv.getSearchWidth())
				fitLeft = true;
			else if(Math.abs(sv.getEnd() - el.start) <= sv.getSearchWidth() || Math.abs(sv.getEnd() - el.end) <= sv.getSearchWidth())
				fitRight = true;
			if(fitLeft && fitRight)
				return true;
		}
		return false;
	}
	private boolean isGzipped(File db) {
		var magic = 0;
		try {
			var raf = new RandomAccessFile(db, "r");
			magic = raf.read() & 0xff | ((raf.read() << 8) & 0xff00);
			raf.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-9);
		}
		return magic == GZIPInputStream.GZIP_MAGIC;
	}
}

class search {
	public String chr = null;
	public Integer start = -1;
	public Integer end = -1;
	public search(String chr, Integer start, Integer end) {
		this.chr = chr;
		this.start = start;
		this.end = end;
	}
}
