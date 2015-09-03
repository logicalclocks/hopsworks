package se.kth.hopsworks.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class ThreadedStreams extends Thread {
        private static final Logger logger = LoggerFactory
                .getLogger(ThreadedStreams.class);

        InputStream inputStream;
        String adminPassword;
        OutputStream outputStream;
        PrintWriter printWriter;
        StringBuilder outputBuffer = new StringBuilder();
        private boolean sudoIsRequested = false;

        /**
         * A simple constructor for when the sudo command is not necessary. This
         * constructor will just run the command you provide, without running sudo
         * before the command, and without expecting a password.
         *
         * @param inputStream
         * @param streamType
         */
        ThreadedStreams(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        /**
         * Use this constructor when you want to invoke the 'sudo' command. The
         * outputStream must not be null. If it is, you'll regret it. :)
         *
         * TODO this currently hangs if the admin password given for the sudo
         * command is wrong.
         *
         * @param inputStream
         * @param streamType
         * @param outputStream
         * @param adminPassword
         */
        ThreadedStreams(InputStream inputStream, OutputStream outputStream,
                              String adminPassword) {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
            this.printWriter = new PrintWriter(outputStream);
            this.adminPassword = adminPassword;
            this.sudoIsRequested = true;
        }

        public void run() {
            // on mac os x 10.5.x, when i run a 'sudo' command, i need to write
            // the admin password out immediately; that's why this code is
            // here.
            if (sudoIsRequested) {
                // doSleep(500);
                printWriter.println(adminPassword);
                printWriter.flush();
            }

            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(
                        inputStream));
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    logger.debug("STDOUT/ERR: "     + line);
                    outputBuffer.append(line + "\n");
                }
            } catch (IOException ioe) {
                // TODO handle this better
                ioe.printStackTrace();
            } catch (Throwable t) {
                // TODO handle this better
                t.printStackTrace();
            } finally {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    // ignore this one
                }
            }
        }

        private void doSleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        public String getOutputBuffer() {
            String out = outputBuffer.toString();
            // clear StringBuilder once it's been read
            this.outputBuffer = new StringBuilder();
            return out;
        }

    }
