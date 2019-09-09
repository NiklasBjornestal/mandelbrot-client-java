import com.sun.glass.ui.Size;
import com.sun.javafx.geom.Rectangle;
import com.sun.prism.image.Coords;
import javafx.concurrent.Task;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.Thread.sleep;

public class MandelbrotBuilder {

    final double minCRe, minCIm, maxCRe, maxCIm;
    final int maxIterations, xSize, ySize;
    final double rePixSize, imPixSize;
    int divisions;
    int segmentXSize, segmentYSize;
    byte[][] image;
    private static String baseUrl = "mandelbrot";

    public MandelbrotBuilder(double minCRe, double minCIm, double maxCRe, double maxCIm, int maxIterations, int xSize, int ySize) {
        this.minCRe = minCRe;
        this.minCIm = minCIm;
        this.maxCRe = maxCRe;
        this.maxCIm = maxCIm;
        this.maxIterations = maxIterations;
        this.xSize = xSize;
        this.ySize = ySize;
        this.rePixSize = (maxCRe - minCRe) / xSize;
        this.imPixSize = (maxCIm - minCIm) / ySize;
        //image = new byte[xSize * ySize];
    }

    public String getUrl(String server, String baseUrl, double minCRe, double minCIm, double maxCRe, double maxCIm, int xSize, int ySize, int maxIterations) {
        return server+"/"+baseUrl+"/"+minCRe+"/"+minCIm+"/"+maxCRe+"/"+maxCIm+"/"+xSize+"/"+ySize+"/"+maxIterations;
    }

    public Callable<JobResult> getImageSegment(int col, int row, int sizeX, int sizeY, int divisions) {

        JobResult result = new JobResult();
        result.setCol(col);
        result.setRow(row);
        result.setSuccess(true);
        double minCRe, minCIm, maxCRe, maxCIm;

        minCRe = this.minCRe + col * sizeX * rePixSize;
        minCIm = this.minCIm + row * sizeY * imPixSize;
        maxCRe = minCRe + (sizeX - 1) * rePixSize;
        maxCIm = minCIm + (sizeY - 1) * imPixSize;

        Callable callable = () -> {
            try {
                String server = ((ServerThreadFactory.ServerThread)Thread.currentThread()).getServer();
                URL url = new URL(getUrl(server, baseUrl, minCRe, minCIm, maxCRe, maxCIm, sizeX, sizeY, maxIterations));
                System.out.println("Reading: "+col+":"+row+" : " + url.toString());
                InputStream is = url.openStream();
                image[col+row*divisions] = IOUtils.toByteArray(is);
                //url.openStream().read(image[x + y * divisions], 0,sizeX*sizeY);
            } catch (MalformedURLException ex){
                System.err.println("Bad url: "+ex);
                result.setSuccess(false);
            } catch (IOException ex) {
                System.err.println("IOException: "+ex);
                result.setSuccess(false);
            }
            return result;
        };
        return callable;
    }

/*    public Runnable getImageSegment(int x, int y, int sizeX, int sizeY, int divisions) {
        String server = "http://localhost:8080"; //((ServerThreadFactory.ServerThread)Thread.currentThread()).getServer();
        double minCRe, minCIm, maxCRe, maxCIm;

        minCRe = this.minCRe + x * sizeX * rePixSize;
        minCIm = this.minCIm + y * sizeY * imPixSize;
        maxCRe = minCRe + (sizeX - 1) * rePixSize;
        maxCIm = minCIm + (sizeY - 1) * rePixSize;

        Runnable runnable = () -> {
            try {
                URL url = new URL(getUrl(server, baseUrl, minCRe, minCIm, maxCRe, maxCIm, maxIterations, sizeX, sizeY));
                System.out.println("Reading: "+x+":"+y+" : " + url.toString());
                url.openStream().read(image[x + y * divisions]);
            } catch (MalformedURLException ex){
                System.err.println("Bad url: "+ex);
            } catch (IOException ex) {
                System.err.println("IOException: "+ex);
            }
        };
        return runnable;
    }*/

    public void fetch(List<String> servers, int divisions) {
        boolean done = false;
        int tries = 0;
        ArrayList<Callable<JobResult>> jobs = new ArrayList<>();
        this.divisions = divisions;
        ServerThreadFactory threadFactory = new ServerThreadFactory(servers);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(servers.size(), threadFactory);
        segmentXSize = xSize / divisions;
        segmentYSize = ySize / divisions;
        image = new byte[divisions * divisions][segmentXSize * segmentYSize];
        for (int row = 0; row<divisions; row++) {
            for (int col = 0; col<divisions; col++) {
                jobs.add(getImageSegment(col, row, segmentXSize, segmentYSize, divisions));
                //executor.submit(getImageSegment(col, row, segmentXSize, segmentYSize, divisions));
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
            System.err.println("Interupted: "+ex);
        } catch (ExecutionException ex) {
            System.err.println("Execution exception: "+ex);
        }
    }

    public void write(OutputStream out) {
        try {
            out.write("P5\n".getBytes());
            out.write((xSize + " " + ySize + "\n").getBytes());
            out.write("8\n".getBytes());
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

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public int getCol() {
            return col;
        }

        public void setCol(int col) {
            this.col = col;
        }

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }
    }
}
