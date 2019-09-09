import java.io.IOException;
import java.util.ArrayList;

import static java.lang.System.exit;

class Main {

    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println(System.getProperty("sun.java.command") + " min_c_re min_c_im max_c_re max_c_im max_n x y divisions list-of-servers");
        System.err.println();
        System.err.println("Example:\n");
        System.err.println(System.getProperty("sun.java.command") + "-1 -1.5 2 1.5 1024 10000 10000 4 localhost:4444 localhost:3333 192.168.33.3:4444");
        exit(0);
    }

    public static void main(String[] args) {
        if (args.length < 9) {
            printUsage();
        }
        double min_c_re, min_c_im, max_c_re, max_c_im;
        int max_n, x_size, y_size, divisions;
        ArrayList<String> servers = new ArrayList<>();
        try {
            min_c_re = Double.parseDouble(args[0]);
            min_c_im = Double.parseDouble(args[1]);
            max_c_re = Double.parseDouble(args[2]);
            max_c_im = Double.parseDouble(args[3]);
            max_n = Integer.parseInt(args[4]);
            x_size = Integer.parseInt(args[5]);
            y_size = Integer.parseInt(args[6]);
            divisions = Integer.parseInt(args[7]);
            for (int i = 8; i < args.length; i++) {
                String server = (args[i].startsWith("http://") ? "" : "http://") + args[i];
                if (server.matches("http://.*:\\d+")) { // Check for valid server name
                    System.err.println("Added: "+server);
                    servers.add(server);
                } else {
                    System.err.println("Invalid server: " + args[i]);
                    printUsage();
                }
            }

            MandelbrotBuilder mandel = new MandelbrotBuilder(min_c_re, min_c_im, max_c_re, max_c_im, max_n, x_size, y_size);
            mandel.fetch(servers, divisions);
            mandel.write(System.out);
            System.out.flush();
        } catch (NumberFormatException ex) {
            System.err.println("Bad argument");
            printUsage();
        }

    }
}
