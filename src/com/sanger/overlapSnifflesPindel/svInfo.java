package com.sanger.overlapSnifflesPindel;

import com.sanger.intervalTree.*;

public class svInfo {
	private String[] lineTokens = null;
	private String chrom = null;
	private Integer start = -1;
	private Integer end = -1;
	private Double sw = -1.0;
	Boolean found = null;
	
	public svInfo(String[] lineTokens, String chrom, Integer start, Integer end, double sw) {
		this.lineTokens = lineTokens;
		this.chrom = chrom;
		this.start = start;
		this.end = end;
		this.sw = sw;
	}
	public int getSearchWidth() {
		return sw.intValue();
	}
	public String getLine() {
		return String.join("\t", String.join("\t", lineTokens), Boolean.toString(found));
	}
	public int lowLeft() {
		return Math.max(start- sw.intValue(), 0);
	}
	public int highLeft() {
		return start+ sw.intValue();
	}
	public int lowRight() {
		return Math.max(end - sw.intValue(), 0);
	}
	public int highRight() {
		return end + sw.intValue();
	}
	public String getChrom() {
		return chrom;
	}
	public Integer getStart() {
		return start;
	}
	public Integer getEnd() {
		return end;
	}
	public Interval1D searchInterval() {
		return new Interval1D(start - sw.intValue(), start + sw.intValue());
	}
	public void setFound(boolean found) {
		this.found = found;
	}
	public boolean getFound() {
		return found;
	}
	public boolean fits(svInfo tmp) {
		return (Math.abs(start - tmp.start) <= sw) && (Math.abs(end - tmp.end) <= sw);
	}
}
