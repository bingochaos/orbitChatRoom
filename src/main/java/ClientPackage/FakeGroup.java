package ClientPackage;

import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.cluster.NodeAddressImpl;
import cloud.orbit.concurrent.ExecutorUtils;
import cloud.orbit.concurrent.Task;
import cloud.orbit.exception.UncheckedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Created by bingoc on 2017/2/23.
 */
public class FakeGroup {
    private static final Logger logger = LoggerFactory.getLogger(FakeGroup.class);

    private static final LoadingCache<String, FakeGroup> groups = Caffeine.newBuilder()
            .weakValues()
            .build(new CacheLoader<String, FakeGroup>()
            {
                @Override
                public FakeGroup load(final String key) throws Exception
                {
                    return new FakeGroup(key);
                }
            });

    private final Map<NodeAddress, FakeClusterPeer> currentChannels = new HashMap<>();

    private final Object topologyMutex = new Object();
    @SuppressWarnings("rawtypes")
    private final LoadingCache<String, ConcurrentMap> maps = Caffeine.newBuilder()
            .build(new CacheLoader<String, ConcurrentMap>()
            {
                @Override
                public ConcurrentMap load(final String key) throws Exception
                {
                    return new ConcurrentHashMap();
                }
            });
    private int count = 0;
    private String clusterName;
    private static Executor pool = ExecutorUtils.newScalingThreadPool(20);

    public FakeGroup(final String clusterName)
    {
        this.clusterName = clusterName;
    }

    protected NodeAddressImpl join(final FakeClusterPeer fakeChannel)
    {
        Collection<CompletableFuture<?>> tasks;
        NodeAddressImpl nodeAddress;
        synchronized (topologyMutex)
        {
            final String name = "channel." + (++count) + "." + clusterName;
            nodeAddress = new NodeAddressImpl(new UUID(name.hashCode(), count));
            currentChannels.put(nodeAddress, fakeChannel);
            fakeChannel.setAddress(nodeAddress);

            final ArrayList<NodeAddress> newView = new ArrayList<>(currentChannels.keySet());

            tasks = currentChannels.values().stream().map(ch -> CompletableFuture.runAsync(() -> ch.onViewChanged(newView), pool)).collect(Collectors.toList());
        }
        Task.allOf(tasks).join();

        return nodeAddress;
    }

    public void leave(final FakeClusterPeer fakeClusterPeer)
    {
        List<CompletableFuture<?>> tasks;
        synchronized (topologyMutex)
        {
            currentChannels.remove(fakeClusterPeer.localAddress());
            final ArrayList<NodeAddress> newView = new ArrayList<>(currentChannels.keySet());
            tasks = currentChannels.values().stream().map(ch -> CompletableFuture.runAsync(() -> ch.onViewChanged(newView), pool)).collect(Collectors.toList());
        }
        Task.allOf(tasks).join();
    }


    public void sendMessage(final NodeAddress from, final NodeAddress to, final byte[] buff)
    {
        if (to == null)
        {
            throw new NullPointerException("Target address cannot be null");
        }
        CompletableFuture.runAsync(() -> {
            try
            {
                final FakeClusterPeer fakeClusterPeer = currentChannels.get(to);
                if (fakeClusterPeer == null)
                {
                    throw new UncheckedException("Unknown address: " + to);
                }
                fakeClusterPeer.onMessageReceived(from, buff);
            }
            catch (Exception ex)
            {
                logger.error("Error sending message", ex);
            }
        }, pool);
    }

    public static FakeGroup get(final String clusterName)
    {
        return groups.get(clusterName);
    }

    @SuppressWarnings("unchecked")
    public <K, V> ConcurrentMap<K, V> getCache(final String name)
    {
        return maps.get(name);
    }

    public Map<String, ConcurrentMap> getCaches()
    {
        return maps.asMap();
    }

    Executor pool()
    {
        return pool;
    }
}
