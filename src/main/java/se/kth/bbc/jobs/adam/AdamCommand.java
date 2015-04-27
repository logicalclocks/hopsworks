package se.kth.bbc.jobs.adam;

/**
 *
 * @author stig
 */
public enum AdamCommand {
//ADAM ACTIONS

  COMPARE("Compare two ADAM files based on read name.",
          new AdamArgument[]{
            new AdamArgument("INPUT1", "The first ADAM file to compare.", true),
            new AdamArgument("INPUT2", "The second ADAM file to compare.", true)},
          new AdamOption[]{
            new AdamOption("comparisons",
                    "Comma-separated list of comparisons to run.", false, false),
            new AdamOption("list_comparisons",
                    "If specified, lists all the comparisons that are available.",
                    false, true),
            new AdamOption("output",
                    "Directory to generate the comparison output files (default: output to STDOUT).",
                    true, false, true),
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true),
            new AdamOption("recurse1",
                    "Optional regex; if specified, INPUT1 is recursively searched for matching this pattern, whose contents are loaded and merged prior to running the comparison",
                    false, false),
            new AdamOption("recurse2",
                    "Optional regex; if specified, INPUT2 is recursively searched for matching this pattern, whose contents are loaded and merged prior to running the comparison",
                    false, false)}),
  FINDREADS(
          "Find reads that match particular individual or comparative criteria.",
          new AdamArgument[]{
            new AdamArgument("INPUT1", "The first ADAM file to compare.", true),
            new AdamArgument("INPUT2", "The second ADAM file to compare.", true),
            new AdamArgument("FILTER", "Filter to run.", false)},
          new AdamOption[]{
            new AdamOption("file",
                    "File name to write the matching read names to.", true,
                    false, true),
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true),
            new AdamOption("recurse1",
                    "Optional regex; if specified, INPUT1 is recursively searched for matching this pattern, whose contents are loaded and merged prior to running the comparison",
                    false, false),
            new AdamOption("recurse2",
                    "Optional regex; if specified, INPUT2 is recursively searched for matching this pattern, whose contents are loaded and merged prior to running the comparison",
                    false, false)}),
  DEPTH("Calculate the depth from a given ADAM file, at each variant in a VCF.",
          new AdamArgument[]{
            new AdamArgument("ADAM", "The Read file to use to calculate depths.",
                    true),
            new AdamArgument("VCF",
                    "The VCF containing the sites at which to calculate depths.",
                    true)},
          new AdamOption[]{
            new AdamOption("cartesian", "Use a cartesian join, then filter.",
                    false, true),
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true)}),
  COUNT_KMERS("Counts the k-mers/q-mers from a read dataset.",
          new AdamArgument[]{
            new AdamArgument("INPUT",
                    "The ADAM, BAM or SAM file to count kmers from.", true),
            new AdamArgument("OUTPUT", "Location for storing k-mer counts.",
                    true, true),
            new AdamArgument("KMER_LENGTH", "Length of k-mers.", false)},
          new AdamOption[]{
            new AdamOption("countQmers", "Counts q-mers instead of k-mers.",
                    false, true),
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true),
            new AdamOption("printHistogram", "Prints a histogram of counts.",
                    false, true),
            new AdamOption("repartition",
                    "Set the number of partitions to map data to", false, false)}),
  TRANSFORM(
          "Convert SAM/BAM to ADAM format and optionally perform read pre-processing transformations.",
          new AdamArgument[]{
            new AdamArgument("INPUT",
                    "The ADAM, BAM or SAM file to apply the transforms to.",
                    true),
            new AdamArgument("OUTPUT",
                    "Location to write the transformed data in ADAM/Parquet format.",
                    true, true)},
          new AdamOption[]{
            new AdamOption("coalesce",
                    "Set the number of partitions written to the ADAM output directory.",
                    false, false),
            new AdamOption("dump_observations",
                    "Local path to dump BQSR observations to. Outputs CSV format.",
                    true, false, true),
            new AdamOption("known_indels",
                    "VCF file including locations of known INDELs. If none is provided, default consensus model will be used.",
                    true, false),
            new AdamOption("known_snps",
                    "Sites-only VCF giving location of known SNPs.", true, false),
            new AdamOption("log_odds_threshold",
                    "The log-odds threshold for accepting a realignment. Default value is 5.0.",
                    false, false),
            new AdamOption("mark_duplicate_reads", "Mark duplicate reads.",
                    false, true),
            new AdamOption("max_consensus_number",
                    "The maximum number of consensus to try realigning a target region to. Default value is 30.",
                    false, false),
            new AdamOption("max_indel_size",
                    "The maximum length of an INDEL to realign to. Default value is 500.",
                    false, false),
            new AdamOption("max_target_size",
                    "The maximum length of a target region to attempt realigning. Default length is 3000.",
                    false, false),
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true),
            new AdamOption("qualityBasedTrim",
                    "Trims reads based on quality scores of prefix/suffixes across read group.",
                    false, true),
            new AdamOption("qualityThreshold",
                    "Phred scaled quality threshold used for trimming. If omitted, Phred 20 is used.",
                    false, false),
            new AdamOption("realign_indels",
                    "Locally realign indels present in reads.", false, true),
            new AdamOption("recalibrate_base_qualities",
                    "Recalibrate the base quality scores (ILLUMINA only)", false,
                    true),
            new AdamOption("repartition",
                    "Set the number of partitions to map data to", false, false),
            new AdamOption("sort_fastq_output",
                    "Sets whether to sort the FASTQ output, if saving as FASTQ. False by default. Ignored if not saving as FASTQ.",
                    false, true),
            new AdamOption("sort_reads",
                    "Sort the reads by referenceId and read position", false,
                    true),
            new AdamOption("trimBeforeBQSR",
                    "Performs quality based trim before running BQSR. Default is to run quality based trim after BQSR.",
                    false, true),
            new AdamOption("trimFromEnd", "Trim to be applied to end of read.",
                    false, false),
            new AdamOption("trimFromStart",
                    "Trim to be applied to start of read.", false, false),
            new AdamOption("trimReadGroup",
                    "Read group to be trimmed. If omitted, all reads are trimmed.",
                    false, false),
            new AdamOption("trimReads",
                    "Apply a fixed trim to the prefix and suffix of all reads/reads in a specific read group.",
                    false, true)}),
  ADAM2FASTQ("Convert BAM to FASTQ files.",
          new AdamArgument[]{
            new AdamArgument("INPUT",
                    "The ADAM, BAM or SAM file to load as input.", true),
            new AdamArgument("OUTPUT",
                    "The ADAM, BAM or SAM file to save as output.", true, true),
            new AdamArgument("OUTPUT",
                    "When writing FASTQ data, all second-in-pair reads will go here, if this argument is provided.",
                    true, true, false)},
          new AdamOption[]{
            new AdamOption("no-projection",
                    "Disable projection on records. No great reason to do this, but useful for testing/comparison.",
                    false, true),
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("persist-level", "Persist() intermediate RDDs", false,
                    false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true),
            new AdamOption("repartition",
                    "Set the number of partitions to map data to", false, false),
            new AdamOption("validation",
                    "SAM tools validation level; when STRICT, checks that all reads are paired.",
                    false, false)}),
  PLUGIN("Executes an ADAMPlugin.",
          new AdamArgument[]{
            new AdamArgument("PLUGIN", "The ADAMPlugin to run.", true),
            new AdamArgument("INPUT", "The input location.", true)},
          new AdamOption[]{
            new AdamOption("access_control", "Class for access control.", false,
                    false),
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true),
            new AdamOption("plugin_args", "Arguments for the plugin", false,
                    false)}),
//CONVERSION OPERATIONS
  BAM2ADAM(
          "Single-node BAM to ADAM converter (Note: the 'transform' command can take SAM or BAM as input).",
          new AdamArgument[]{
            new AdamArgument("BAM", "The SAM or BAM file to convert.", true),
            new AdamArgument("ADAM", "Location to write ADAM data.", true, true)},
          new AdamOption[]{
            new AdamOption("num_threads",
                    "Number of threads/partitions to use (default=4).", false,
                    false),
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true),
            new AdamOption("queue_size", "Queue size (default = 10,000)", false,
                    false),
            new AdamOption("samtools_validation", "SAM tools validation level",
                    false, false)}),
  VCF2FLATGENOTYPE("Single-node VCF to flat-schema'd ADAM converter.",
          new AdamArgument[]{
            new AdamArgument("VCF", "The VCF file to convert.", true),
            new AdamArgument("ADAM", "Location to write ADAM data.", true, true)},
          new AdamOption[]{
            new AdamOption("num_threads",
                    "Number of threads/partitions to use (default=4).", false,
                    false),
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true),
            new AdamOption("queue_size", "Queue size (default = 10,000)", false,
                    false),
            new AdamOption("sample_block",
                    "The number of samples per parquet file", false, false),
            new AdamOption("samples", "Comma-separated set of samples to subset",
                    false, false),
            new AdamOption("samtools_validation", "SAM tools validation level",
                    false, false)}),
  VCF2ADAM("Convert a VCF file to the corresponding ADAM format.",
          new AdamArgument[]{
            new AdamArgument("VCF", "The VCF file to convert.", true),
            new AdamArgument("ADAM", "Location to write ADAM Variant data.",
                    true, true)},
          new AdamOption[]{
            new AdamOption("coalesce",
                    "Set the number of partitions written to the ADAM output directory.",
                    false, false),
            new AdamOption("dict", "Reference dictionary.", true, false),
            new AdamOption("onlyvariants",
                    "Output Variant objects instead of Genotypes.", false, true),
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true)}),
  ANNO2ADAM(
          "Convert a annotation file (in VCF format) to the corresponding ADAM format.",
          new AdamArgument[]{
            new AdamArgument("VCF", "The VCF file to convert.", true),
            new AdamArgument("ADAM",
                    "Location to write ADAM Variant annotations data.", true,
                    true)},
          new AdamOption[]{
            new AdamOption("current-db",
                    "Location of existing ADAM Variant annotations data.", true,
                    false),
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true)}),
  ADAM2VCF("Convert an ADAM variant to the VCF ADAM format.",
          new AdamArgument[]{
            new AdamArgument("ADAM", "The ADAM variant files to convert.", true),
            new AdamArgument("VCF", "Location to write VCF data.", true, true)},
          new AdamOption[]{
            new AdamOption("coalesce",
                    "Set the number of partitions written to the ADAM output directory.",
                    false, false),
            new AdamOption("dict", "Reference dictionary.", true, false),
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true)}),
  FASTA2ADAM(
          "Converts a text FASTA sequence file into an ADAMNucleotideContig Parquet file which represents assembled sequences.",
          new AdamArgument[]{
            new AdamArgument("FASTA", "The FASTA file to convert.", true),
            new AdamArgument("ADAM", "Location to write ADAM data.", true, true)},
          new AdamOption[]{
            new AdamOption("fragment_length",
                    "Sets maximum fragment length. Default value is 10,000. Values greater than 1e9 should be avoided.",
                    false, false),
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true),
            new AdamOption("reads",
                    "Maps contig IDs to match contig IDs of reads.", false,
                    false),
            new AdamOption("verbose",
                    "Prints enhanced debugging info, including contents of seq dict.",
                    false, true)}),
  READS2REF(
          "Convert an ADAM read-oriented file to an ADAM reference-oriented file.",
          new AdamArgument[]{
            new AdamArgument("ADAMREADS", "ADAM read-oriented data.", true),
            new AdamArgument("DIR",
                    "Location to create reference-oriented ADAM data.", true,
                    true)},
          new AdamOption[]{
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true)}),
  MPILEUP("output the samtool mpileup text from ADAM reference-oriented data.",
          new AdamArgument[]{
            new AdamArgument("ADAMREADS", "ADAM read-oriented data.", true)},
          new AdamOption[]{
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true)}),
  FEATURES2ADAM(
          "Convert a file with sequence features into corresponding ADAM format.",
          new AdamArgument[]{
            new AdamArgument("FEATURES",
                    "The features file to convert (e.g., .bed, .gff).", true),
            new AdamArgument("ADAM", "Location to write ADAM features data.",
                    true, true)},
          new AdamOption[]{
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true)}),
  //Does not seem to work properly
  /*
   * WIGFIX2BED("Locally convert a wigFix file to BED format.",
   * new AdamArgument[]{},
   * new AdamOption[]{}),
   */
//PRINT
  PRINT("Print an ADAM formatted file.",
          new AdamArgument[]{
            new AdamArgument("FILE(S)", "One or more files to print.", true)},
          new AdamOption[]{
            new AdamOption("o", "Output to a (local) file.", true, false, true),
            new AdamOption("pretty", "Display raw, pretty-formatted JSON.",
                    false, true),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true)}),
  PRINT_GENES(
          "Load a GTF file containing gene annotations and print the corresponding gene models.",
          new AdamArgument[]{
            new AdamArgument("GTF", "GTF file with gene model data.", true)},
          new AdamOption[]{
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true)}),
  FLAGSTAT(
          "Print statistics on reads in an ADAM file (similar to samtools flagstat).",
          new AdamArgument[]{
            new AdamArgument("INPUT", "The ADAM data to return stats for.", true)},
          new AdamOption[]{
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true)}),
  //TODO: check what happens here
  VIZ("Generates images from sections of the genome.",
          new AdamArgument[]{
            new AdamArgument("INPUT", "The ADAM Records file to view.", true),
            new AdamArgument("REFNAME", "The reference to view.", true)},
          new AdamOption[]{
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("port",
                    "The port to bind to for visualization. The default is 8080.",
                    false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true)}),
  PRINT_TAGS("Prints the values and counts of all tags in a set of records.",
          new AdamArgument[]{
            new AdamArgument("INPUT", "The ADAM file to scan for tags", true)},
          new AdamOption[]{
            new AdamOption("count",
                    "Comma-separated list of tag names; for each tag listed, we print the distinct values and their counts.",
                    false, false),
            new AdamOption("list",
                    "When value is set to <N>, also lists the first N attribute fields for ADAMRecords in the input.",
                    false, false),
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true)}),
  LISTDICT("Print the contents of an ADAM sequence dictionary.",
          new AdamArgument[]{
            new AdamArgument("INPUT", "The ADAM sequence dictionary to print.",
                    true)},
          new AdamOption[]{
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true)}),
  SUMMARIZE_GENOTYPES(
          "Print statistics of genotypes and variants in an ADAM file.",
          new AdamArgument[]{
            new AdamArgument("ADAM",
                    "The ADAM genotypes file to print stats for.", true)},
          new AdamOption[]{
            new AdamOption("format",
                    "Format: one of human, csv. Default: human.", false, false),
            new AdamOption("out", "Write output to the given file.", true, false,
                    true),
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true)}),
  ALLELECOUNT("Calculate Allele frequencies.",
          new AdamArgument[]{
            new AdamArgument("ADAM", "The ADAM Variant file.", true)},
          new AdamOption[]{
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true)}),
  /*
   * BUILDINFO("Display build information (use this for bug reports).",
   * new AdamArgument[]{},
   * new AdamOption[]{}),
   */
  VIEW("View certain reads from an alignment-record file.",
          new AdamArgument[]{
            new AdamArgument("INPUT", "The ADAM, BAM or SAM file to view.", true),
            new AdamArgument("OUTPUT", "Location to write output data.", true,
                    true)},
          new AdamOption[]{
            new AdamOption("F",
                    "Restrict to reads that match none of the bits in <N>.",
                    false, false),
            new AdamOption("G",
                    "Restrict to reads that mismatch at least one of the bits in <N>.",
                    false, false),
            new AdamOption("c",
                    "Print count of matching records, instead of the records themselves.",
                    false, true),
            new AdamOption("f",
                    "Restrict to reads that match all of the bits in <N>.",
                    false, false),
            new AdamOption("g",
                    "Restrict to reads that match any of the bits in <N>.",
                    false, false),
            new AdamOption("parquet_block_size",
                    "Parquet block size (default = 128mb)", false, false),
            new AdamOption("parquet_compression_codec",
                    "Parquet compression codec", false, false),
            new AdamOption("parquet_disable_dictionary",
                    "Disable dictionary encoding", false, true),
            new AdamOption("parquet_logging_level",
                    "Parquet logging level (default = severe)", false, false),
            new AdamOption("parquet_page_size",
                    "Parquet page size (default = 1mb)", false, false),
            new AdamOption("print_metrics",
                    "Print metrics to the log on completion", false, true)});

  private final String description;
  private final AdamArgument[] arguments;
  private final AdamOption[] options;

  AdamCommand(String description, AdamArgument[] arguments, AdamOption[] options) {
    this.description = description;
    this.arguments = arguments;
    this.options = options;
  }

  public String getDescription() {
    return this.description;
  }

  public AdamArgument[] getArguments() {
    return arguments;
  }

  public AdamOption[] getOptions() {
    return options;
  }

  public String getCommand() {
    switch (this) {
      case COMPARE:
        return "compare";
      case FINDREADS:
        return "findreads";
      case DEPTH:
        return "depth";
      case COUNT_KMERS:
        return "count_kmers";
      case TRANSFORM:
        return "transform";
      case ADAM2FASTQ:
        return "adam2fastq";
      case PLUGIN:
        return "plugin";
      case BAM2ADAM:
        return "bam2adam";
      case VCF2FLATGENOTYPE:
        return "vcf2flatgenotype";
      case VCF2ADAM:
        return "vcf2adam";
      case ANNO2ADAM:
        return "anno2adam";
      case ADAM2VCF:
        return "adam2vcf";
      case FASTA2ADAM:
        return "fasta2adam";
      case READS2REF:
        return "reads2ref";
      case MPILEUP:
        return "mpileup";
      case FEATURES2ADAM:
        return "features2adam";
      /*
       * case WIGFIX2BED:
       * return "wigfix2bed";
       */
      case PRINT:
        return "print";
      case PRINT_GENES:
        return "print_genes";
      case FLAGSTAT:
        return "flagstat";
      case VIZ:
        return "viz";
      case PRINT_TAGS:
        return "print_tags";
      case LISTDICT:
        return "listdict";
      case SUMMARIZE_GENOTYPES:
        return "summarize_genotypes";
      case ALLELECOUNT:
        return "allelecount";
      case VIEW:
        return "view";
      default:
        throw new IllegalStateException("Impossible enum value.");
    }
  }

  @Override
  public String toString() {
    StringBuilder retval = new StringBuilder();
    retval.append(getCommand()).append('\n');
    retval.append("Arguments:").append('\n');
    retval.append(arguments).append('\n');
    retval.append("Options:").append('\n');
    retval.append(options);
    return retval.toString();
  }

  public static AdamCommand getFromCommand(String command) {
    switch (command) {
      case "compare":
        return COMPARE;
      case "findreads":
        return FINDREADS;
      case "depth":
        return DEPTH;
      case "count_kmers":
        return COUNT_KMERS;
      case "transform":
        return TRANSFORM;
      case "adam2fastq":
        return ADAM2FASTQ;
      case "plugin":
        return PLUGIN;
      case "bam2adam":
        return BAM2ADAM;
      case "vcf2flatgenotype":
        return VCF2FLATGENOTYPE;
      case "vcf2adam":
        return VCF2ADAM;
      case "anno2adam":
        return ANNO2ADAM;
      case "adam2vcf":
        return ADAM2VCF;
      case "fasta2adam":
        return FASTA2ADAM;
      case "reads2ref":
        return READS2REF;
      case "mpileup":
        return MPILEUP;
      case "features2adam":
        return FEATURES2ADAM;
      case "print":
        return PRINT;
      case "print_genes":
        return PRINT_GENES;
      case "flagstat":
        return FLAGSTAT;
      case "viz":
        return VIZ;
      case "print_tags":
        return PRINT_TAGS;
      case "listdict":
        return LISTDICT;
      case "summarize_genotypes":
        return SUMMARIZE_GENOTYPES;
      case "allelecount":
        return ALLELECOUNT;
      case "view":
        return VIEW;
      default:
        throw new IllegalArgumentException("Argument " + command
                + " does not represent a valid ADAM command.");
    }
  }

}
