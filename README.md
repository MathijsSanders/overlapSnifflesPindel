# Validate PacBio-detected small deletion in Illumina WGS data

Determine whether small deletions (< 1kb) detected in PacBio data is present in matched Illumina WGS data (Pindel output and raw aligned reads).

## How do I run it?

First, align PacBio sequencing data with NGMLR (https://github.com/philres/ngmlr), call SVs using Sniffles (https://github.com/fritzsedlazeck/Sniffles), annotate Sniffles output with AnnotSV (https://github.com/lgmgeo/AnnotSV) and, possibly, annotate and filter final Sniffles results with annotateSniffles (https://github.com/MathijsSanders/annotateSniffles).

Next, provide the filtered Sniffles output, Pindel output (raw VCF or GZIP VCF) and the Illumina WGS BAM file, tweak the paramters to your preference and observe whether the small deletions detected in PacBio data is also present in matched Illumina WGS data.

### The recommended way

The pre-compiled JAR file is included with the repository, but in case the package needs to be recompiled, please run:

```bash
mvn package clean
```

The following command adds a single column to the annotated Sniffles file.

- Found in Pindel: true or false. Indicated whether the small deletion in present in the Pindel output or is detectable in the raw Illumina WGS reads. 

```bash
java -Xmx5G -jar overlapSnifflesPindel.jar --input-filtered-sniffles input_filtered_sniffles_file --input-pindel pindel_vcf --illumina-bam illumina_WGS_bam --output-file output_file --minimum-width minimum_window_width --overlap-fraction fraction_for_determining_window_size --maximum-del-size max_size_deletion --extract-width padding_window_illumina_bam --threads threads --help --version
```

- --input-filtered-sniffles*: Input filtered and annotated Sniffles file.
- --input-pindel*: Input Pindel VCF file. Can be GZIP'ed.
- --illumina-bam*: Input Illumina WGS BAM file.
- --output-file*: Output file.
- --minimum-width: Minimum window size for detecting fuzzy breakpoints (default: 20nt).
- --overlap-fraction: Fraction used for calculating window size for fuzzy breakpoint detection (default: 0.05).
- --maximum-del-size: Maximum size deletions for including in analysis (default: 1000nt).
- --extract-width: How many bases should be padded to the read extraction window Illumina WGS data (default: 50nt).
- --threads: Number of threads (default: 1).    
- --help, -help: Get usage information.
- --version, -version: Get current version.
- \* Required.

*Dependencies*
- Maven version 3+ (For compiling only).
- Java JDK 11+
