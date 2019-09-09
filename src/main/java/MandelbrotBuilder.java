import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;


class MandelbrotBuilder {

    private final double minCRe, minCIm, maxCRe, maxCIm;
    private final int maxIterations, xSize, ySize;
    private final double rePixSize, imPixSize;
    private int divisions;
    private int segmentXSize, segmentYSize;
    private byte[][] image;
    private static final String baseUrl = "mandelbrot";

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

    private String getUrl(String server, String baseUrl, double minCRe, double minCIm, double maxCRe, double maxCIm, int xSize, int ySize, int maxIterations) {
        return server+"/"+baseUrl+"/"+minCRe+"/"+minCIm+"/"+maxCRe+"/"+maxCIm+"/"+xSize+"/"+ySize+"/"+maxIterations;
    }

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
            try {
                String server = ((ClientThreadFactory.ClientThread)Thread.currentThread()).getServer();
                URL url = new URL(getUrl(server, baseUrl, minCRe, minCIm, maxCRe, maxCIm, sizeX, sizeY, maxIterations));
                System.err.println("Reading: "+col+":"+row+" : " + url.toString());
                InputStream is = url.openStream();
                image[col+row*divisions] = IOUtils.toByteArray(is);
            } catch (MalformedURLException ex){
                System.err.println("Bad url: "+ex);
                result.setSuccess(false);
            } catch (IOException ex) {
                System.err.println("IOException: "+ex);
                result.setSuccess(false);
            }
            return result;
        };
    }

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

    void write(OutputStream out) {
        try {
//            out.write("P5\n".getBytes());
//            out.write((xSize + " " + ySize + "\n").getBytes());
//            out.write("8\n".getBytes());
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
