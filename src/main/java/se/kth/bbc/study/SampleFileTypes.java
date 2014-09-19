/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

/**
 *
 * @author roshan
 */
public enum SampleFileTypes {
    
  FASTQ("fastq"),
  FASTA("fasta"),
  BAM("bam"),
  SAM("sam"),
  VCF("vcf");
  
  
  String type;
   
  SampleFileTypes(String type){
      this.type = type;
  }
  
  public String getFileType() {
      return this.type;
  }
    
}
