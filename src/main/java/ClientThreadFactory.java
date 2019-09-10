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
     * @param runnable a runnable to be executed by new thread instance
     * @return constructed ClientThread, or null if the request to create a thread is rejected
     */
    @Override
    public ClientThread newThread(Runnable runnable) {
        ClientThread thread;
        synchronized (this) {
            thread = new ClientThread(runnable, servers.get(threadNumber++), servers);
            threadNumber = (threadNumber) % servers.size();
        }
        return thread;
    }

    /**
     * Custom Thread with getServer method containing name of server
     * to fetch data from
     */
    class ClientThread extends Thread {
        private String server;
        private final List<String> servers;

        ClientThread(Runnable runnable, String server, List<String> servers) {
            super(runnable);
            this.server = server;
            this.servers = servers;
        }

        String getServer() {
            return server;
        }

        /**
         * Remove a server from the server list and select a new random server from the list of
         * remaining servers
         */
        public void removeAndSwitchServer() {
            servers.remove(server);
            if (servers.size() > 0)
                server = servers.get((int)(Math.random()*servers.size()));
        }
    }
}
