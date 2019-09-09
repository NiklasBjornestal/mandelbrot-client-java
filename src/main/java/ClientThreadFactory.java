import java.util.List;
import java.util.concurrent.ThreadFactory;

public class ClientThreadFactory implements ThreadFactory {

    private final List<String> servers;
    private int threadNumber;

    ClientThreadFactory(List<String> servers) {
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
