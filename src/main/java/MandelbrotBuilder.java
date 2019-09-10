import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;


/**
 *
 */
class MandelbrotBuilder {

    private final double minCRe, minCIm, maxCRe, maxCIm;
    private final int maxIterations, xSize, ySize;
    private final double rePixSize, imPixSize; // Size of each pixel in re/im scale
    private int divisions;
    private int segmentXSize, segmentYSize;
    private byte[][] image;
    private static final String baseUrl = "mandelbrot";

    /**
     * @param minCRe min real value
     * @param minCIm min imaginary value
     * @param maxCRe max real value
     * @param maxCIm max imaginary value
     * @param maxIterations Max number of iterations to calculate
     * @param xSize x size in pixels
     * @param ySize y size in pixels
     */
    MandelbrotBuilder(double minCRe, double minCIm, double maxCRe, double maxCIm, int maxIterations, int xSize, int ySize) {
        this.minCRe = minCRe;
        this.minCIm = minCIm;
        this.maxCRe = maxCRe;
        this.maxCIm = maxCIm;
        this.maxIterations = maxIterations;
        this.xSize = xSize;
        this.ySize = ySize;
        this.rePixSize = (maxCRe - minCRe) / xSize;
        this.imPixSize = (maxCIm - minCIm) / ySize;
    }

    /**
     * Build a url from parameters
     * @param server name of server
     * @param baseUrl base of address
     * @param minCRe min real value
     * @param minCIm min imaginary value
     * @param maxCRe max real value
     * @param maxCIm max imaginary value
     * @param maxIterations Max number of iterations to calculate
     * @param xSize x size in pixels
     * @param ySize y size in pixels
     * @return url
     */
    private String getUrl(String server, String baseUrl, double minCRe, double minCIm, double maxCRe, double maxCIm, int xSize, int ySize, int maxIterations) {
        return server+"/"+baseUrl+"/"+minCRe+"/"+minCIm+"/"+maxCRe+"/"+maxCIm+"/"+xSize+"/"+ySize+"/"+maxIterations;
    }

    /**
     * @param col column of image segment to get (0 - divisions-1)
     * @param row row of image segment to get (0 - divisions-1)
     * @param sizeX x size of image segment to get
     * @param sizeY y size of image segment to get
     * @param divisions number of divisions
     * @return Callable with JobResult
     */
    private Callable<JobResult> getImageSegment(int col, int row, int sizeX, int sizeY, int divisions) {

        JobResult result = new JobResult();
        result.setCol(col);
        result.setRow(row);
        result.setSuccess(true);
        double minCRe, minCIm, maxCRe, maxCIm;

        minCRe = this.minCRe + col * sizeX * rePixSize;
        minCIm = this.minCIm + row * sizeY * imPixSize;
        maxCRe = minCRe + (sizeX - 1) * rePixSize;
        maxCIm = minCIm + (sizeY - 1) * imPixSize;

        return () -> {
            String server = ((ClientThreadFactory.ClientThread)Thread.currentThread()).getServer();
            try {
                URL url = new URL(getUrl(server, baseUrl, minCRe, minCIm, maxCRe, maxCIm, sizeX, sizeY, maxIterations));
                System.err.println("Reading: "+col+":"+row+" : " + url.toString());
                try (InputStream is = url.openStream()) {
                image[col+row*divisions] = IOUtils.toByteArray(is);
                } catch (IOException ex) {
                    System.err.println("IOException: "+ex);
                    ((ClientThreadFactory.ClientThread)Thread.currentThread()).removeAndSwitchServer();
                    result.setSuccess(false);
                }
            } catch (MalformedURLException ex) {
                System.err.println("Bad url: " + ex);
                result.setSuccess(false);
            }
            return result;
        };
    }

    /**
     * @param servers list of servers to use
     * @param divisions number of parts image should be divided into (divisions x divisions)
     */
    void fetch(List<String> servers, int divisions) {
        boolean done = false;
        int tries = 0;
        ArrayList<Callable<JobResult>> jobs = new ArrayList<>();
        this.divisions = divisions;
        ClientThreadFactory threadFactory = new ClientThreadFactory(servers);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(servers.size(), threadFactory);
        segmentXSize = xSize / divisions;
        segmentYSize = ySize / divisions;
        image = new byte[divisions * divisions][segmentXSize * segmentYSize];
        for (int row = 0; row<divisions; row++) {
            for (int col = 0; col<divisions; col++) {
                jobs.add(getImageSegment(col, row, segmentXSize, segmentYSize, divisions));
            }
        }
        try {
            while (!done && tries < 5) {
                done = true;
                tries++;
                List<Future<JobResult>> res = executor.invokeAll(jobs);
                jobs = new ArrayList<>();
                for (Future<JobResult> future : res) {
                    JobResult result = future.get();
                    if (!result.getSuccess()) {
                        jobs.add(getImageSegment(result.getCol(), result.getRow(), segmentXSize, segmentYSize, divisions));
                        done = false;
                    }
                }
            }
            executor.shutdown();
        } catch (InterruptedException ex) {
            System.err.println("Interrupted: "+ex);
        } catch (ExecutionException ex) {
            System.err.println("Execution exception: "+ex);
        }
    }

    /**
     * Output an pgm image from the received image segments
     *
     * @param out stream to write image to
     */
    void write(OutputStream out) {
        try {
            out.write("P5\n".getBytes());
            out.write((xSize + " " + ySize + "\n").getBytes());
            out.write("255\n".getBytes());
            for (int row = 0; row<divisions; row++) {
                for (int segmentRow = 0; segmentRow < segmentYSize; segmentRow++) {
                    for (int col = 0; col<divisions; col++) {
                        out.write(image[col + row * divisions], segmentRow * segmentXSize, segmentXSize);
                    }
                }
            }
            out.flush();
        } catch (IOException ex) {
            System.err.println("Error writing file: "+ex);
        }
    }

    /**
     * Keeps track of result of job
     * row, col is used to keep track of which image segment to retry to get if not successful
     */
    private class JobResult {
        private int row;
        private int col;
        private Boolean success;

        int getRow() {
            return row;
        }

        void setRow(int row) {
            this.row = row;
        }

        int getCol() {
            return col;
        }

        void setCol(int col) {
            this.col = col;
        }

        Boolean getSuccess() {
            return success;
        }

        void setSuccess(Boolean success) {
            this.success = success;
        }
    }
}
