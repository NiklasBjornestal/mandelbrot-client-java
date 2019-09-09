import jdk.nashorn.internal.runtime.regexp.joni.Regex;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static java.lang.System.exit;

public class Main {

    public static void printUsage() {
        System.out.println("Usage:");
        System.out.println(System.getProperty("sun.java.command") + " min_c_re min_c_im max_c_re max_c_im max_n x y divisions list-of-servers");
        System.out.println();
        System.out.println("Example:\n");
        System.out.println(System.getProperty("sun.java.command") + "-1 -1.5 2 1.5 1024 10000 10000 4 localhost:4444 localhost:3333 192.168.33.3:4444");
        exit(0);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 9) {
            printUsage();
        }
        double min_c_re, min_c_im, max_c_re, max_c_im;
        int max_n, x_size, y_size, divisions;
        ArrayList<String> servers = new ArrayList<String>();
        try {
            min_c_re = Double.parseDouble(args[0]);
            min_c_im = Double.parseDouble(args[1]);
            max_c_re = Double.parseDouble(args[2]);
            max_c_im = Double.parseDouble(args[3]);
            max_n = Integer.parseInt(args[4]);
            x_size = Integer.parseInt(args[5]);
            y_size = Integer.parseInt(args[6]);
            divisions = Integer.parseInt(args[7]);
            int idx = 0;
            for (int i = 8; i < args.length; i++) {
                String server = (args[i].startsWith("http://") ? "" : "http://") + args[i];
                if (server.matches("http:\\/\\/.*:\\d+")) { // Check for valid server name
                    System.out.println("Added: "+server);
                    servers.add(server);
                } else {
                    System.err.println("Invalid server: " + args[i]);
                    printUsage();
                }
            }

            MandelbrotBuilder mandel = new MandelbrotBuilder(min_c_re, min_c_im, max_c_re, max_c_im, max_n, x_size, y_size);
            mandel.fetch(servers, divisions);
            OutputStream os = new FileOutputStream("c:\\temp\\image.pgm");
            mandel.write(os);
            os.close();
        } catch (NumberFormatException ex) {
            System.err.println("Bad argument");
            printUsage();
        }

    }
}