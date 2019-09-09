import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * Custom ThreadFactory that creates threads with a string containing
 * their associated server
 */
public class ClientThreadFactory implements ThreadFactory {

    private final List<String> servers;
    private int threadNumber;

    /**
     * @param servers list with servers to use
     */
    ClientThreadFactory(List<String> servers) {
        this.servers = servers;
    }

    /**
     * @param runnable
     * @return ClientThread with name of server to use
     */
    @Override
    public ClientThread newThread(Runnable runnable) {
        ClientThread thread;
        synchronized (this) {
            thread = new ClientThread(runnable, servers.get(threadNumber++));
            threadNumber = (threadNumber) % servers.size();
        }
        return thread;
    }

    /**
     * Custom Thread with getServer method containing name of server
     * to fetch data from
     */
    class ClientThread extends Thread {
        private final String server;

        ClientThread(Runnable runnable, String server) {
            super(runnable);
            this.server = server;
        }

        String getServer() {
            return server;
        }
    }
}
