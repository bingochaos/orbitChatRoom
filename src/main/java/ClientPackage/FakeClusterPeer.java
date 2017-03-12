package ClientPackage;

import cloud.orbit.actors.cluster.ClusterPeer;
import cloud.orbit.actors.cluster.MessageListener;
import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.cluster.ViewListener;
import cloud.orbit.concurrent.Task;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by bingoc on 2017/2/23.
 */
public class FakeClusterPeer implements ClusterPeer{
    private ViewListener viewListener;
    private MessageListener messageListener;
    private FakeGroup group;
    private NodeAddress address;
    private AtomicLong messagesSent = new AtomicLong();
    private AtomicLong messagesSentOk = new AtomicLong();
    private AtomicLong messagesReceived = new AtomicLong();
    private AtomicLong messagesReceivedOk = new AtomicLong();
    private CompletableFuture<?> startFuture = new CompletableFuture<>();

    public FakeClusterPeer()
    {
    }

    public Task<Void> join(final String clusterName, final String nodeName)
    {
        group = FakeGroup.get(clusterName);
        return Task.fromFuture(CompletableFuture.runAsync(() -> {
            address = group.join(this);
            startFuture.complete(null);
        }, group.pool()));
    }

    @Override
    public void leave()
    {
        group.leave(this);
    }

    public void onViewChanged(final List<NodeAddress> newView)
    {
        viewListener.onViewChange(newView);
    }

    public void onMessageReceived(final NodeAddress from, final byte[] buff)
    {
        messagesReceived.incrementAndGet();
        messageListener.receive(from, buff);
        messagesReceivedOk.incrementAndGet();
    }

    @Override
    public NodeAddress localAddress()
    {
        return address;
    }

    @Override
    public void registerViewListener(final ViewListener viewListener)
    {
        this.viewListener = viewListener;
    }

    @Override
    public void registerMessageReceiver(final MessageListener messageListener)
    {
        this.messageListener = messageListener;
    }


    @Override
    public void sendMessage(final NodeAddress to, final byte[] message)
    {
        startFuture.join();
        messagesSent.incrementAndGet();
        group.sendMessage(address, to, message);
        messagesSentOk.incrementAndGet();
    }

    @Override
    public <K, V> ConcurrentMap<K, V> getCache(final String name)
    {
        return group.getCache(name);
    }

    void setAddress(final NodeAddress address)
    {
        this.address = address;
    }
}
