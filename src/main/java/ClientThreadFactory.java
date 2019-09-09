import java.util.List;
import java.util.concurrent.ThreadFactory;

public class ClientThreadFactory implements ThreadFactory {

    private List<String> servers;
    private int threadNumber;

    public ClientThreadFactory(List<String> servers) {
        this.servers = servers;
    }

    @Override
    public ClientThread newThread(Runnable runnable) {
        ClientThread thread;
        synchronized (this) {
            thread = new ClientThread(runnable, servers.get(threadNumber++));
            threadNumber = (threadNumber) % servers.size();
        }
        return thread;
    }

    public class ClientThread extends Thread {
        private String server;

        public ClientThread(Runnable runnable, String server) {
            super(runnable);
            this.server = server;
        }

        public String getServer() {
            return server;
        }
    }
}
