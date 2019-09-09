import java.util.List;
import java.util.concurrent.ThreadFactory;

public class ServerThreadFactory implements ThreadFactory {

    private List<String> servers;
    private int threadNumber;

    public ServerThreadFactory(List<String> servers) {
        this.servers = servers;
    }

    @Override
    public ServerThread newThread(Runnable runnable) {
        ServerThread thread;
        synchronized (this) {
            thread = new ServerThread(runnable, servers.get(threadNumber++));
            threadNumber = (threadNumber) % servers.size();
        }
        return thread;
    }

    public class ServerThread extends Thread {
        private String server;

        public ServerThread(Runnable runnable, String server) {
            super(runnable);
            this.server = server;
        }

        public String getServer() {
            return server;
        }
    }
}
