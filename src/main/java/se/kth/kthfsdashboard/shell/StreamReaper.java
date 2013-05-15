/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.shell;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

class StreamReaper  {
   private static final Logger logger = Logger.getLogger(StreamReaper.class.getName());
   private InputStream inStream;
   private File outputFile;

   public StreamReaper(InputStream inStream, File outputFile) {
      this.inStream = inStream;
      this.outputFile = outputFile;
   }

   public void doWork() throws Exception {
      BufferedReader bufInStream = null;
      BufferedWriter bufOutStream = null;
      try {
         bufInStream = new BufferedReader(new InputStreamReader(inStream));
         FileWriter fileStream = new FileWriter(outputFile);
         bufOutStream = new BufferedWriter(fileStream);

         String line;
         while (true) {
            try {
               line = bufInStream.readLine();
            } catch (IOException ex) {
               logger.log(Level.WARNING, "stream break, assume pipe is broken", ex);
               break;
            }
            if (line == null) {
               break;
            }
            bufOutStream.write(line);
            bufOutStream.newLine();
            /*
             * some overhead here, fork a thread to flush periodically if it's a
             * problem.
             */
            bufOutStream.flush();
         }
         bufOutStream.flush();
      } finally {
         if (bufInStream != null) {
            try {
               bufInStream.close();
            } catch (IOException e) {
               logger.log(Level.WARNING, "falied to close input stream", e);
            }
         }

         if (bufOutStream != null) {
            try {
               bufOutStream.close();
            } catch (IOException e) {
               logger.log(Level.WARNING, "falied to close output stream: " + outputFile, e);
            }
         }
      }
   }

   public void onStart() {
      logger.log(Level.INFO, "start dumping: " + outputFile);
   }

   public void onException(Throwable t) {
      logger.log(Level.WARNING, "falied to dump the stream: " + outputFile);
   }

   public void onFinish() {
      logger.log(Level.INFO, "finish dumping: " + outputFile);
   }
}
