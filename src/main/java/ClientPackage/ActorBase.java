package ClientPackage;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.client.ClientPeer;
import cloud.orbit.actors.cloner.ExecutionObjectCloner;
import cloud.orbit.actors.cloner.KryoCloner;
import cloud.orbit.actors.extensions.LifetimeExtension;
import cloud.orbit.actors.extensions.json.InMemoryJSONStorageExtension;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.ActorFactoryGenerator;
import cloud.orbit.concurrent.ExecutorUtils;
import cloud.orbit.concurrent.ForwardingExecutorService;
import cloud.orbit.concurrent.Task;
import com.sun.deploy.cache.InMemoryLocalApplicationProperties;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

import javax.xml.ws.Service;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Created by bingoc on 2017/2/23.
 */
public class ActorBase {

    protected List<Stage> stages = new ArrayList<>();
    protected List<ClientPeer> clientPeers = new ArrayList<>();
    protected String clusterName = "cluster." + Math.random() + "." + getClass().getSimpleName();
    protected ConcurrentHashMap<Object, Object> fakeDatabase = new ConcurrentHashMap<>();


    protected FakeClock clock = new FakeClock() {
        @Override
        public long incrementTimeMillis(long offsetMillis) {
            return super.incrementTimeMillis(offsetMillis);
        }
    };
    protected FakeSync fakeSync = new FakeSync();

    protected ServiceLocator serviceLocator;

    public ActorBase()
    {
        ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();
        serviceLocator = factory.create(UUID.randomUUID().toString());
        ServiceLocatorUtilities.addOneConstant(serviceLocator, fakeSync);
    }

    protected static final ExecutorService commonPool = new ForwardingExecutorService<ExecutorService>(ExecutorUtils.newScalingThreadPool(200)) {

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return true;
        }

        @Override
        public void shutdown() {

        }
    };

    public Stage createStage() {

        LifetimeExtension lifetimeExtension = new LifetimeExtension() {
            @Override
            public Task<?> preActivation(AbstractActor<?> actor) {
                serviceLocator.inject(actor);
                return Task.done();
            }
        };
        Stage stage = new Stage.Builder()
                .extensions(lifetimeExtension, new InMemoryJSONStorageExtension(fakeDatabase))
                .mode(Stage.StageMode.HOST)
                .executionPool(commonPool)
                .objectCloner(getExecutionObjectCloner())
                .clock(clock)
                .clusterName(clusterName)
                .clusterPeer(new FakeClusterPeer())
                .build();

        stages.add(stage);
        stage.start().join();

        ActorFactoryGenerator afg = new ActorFactoryGenerator();
        Stream.of(getClass().getClasses())
                .forEach(c -> {
                    if (Actor.class.isAssignableFrom(c) && c.isInterface())
                    {
                        afg.getFactoryFor(c);
                        stage.getHosting().canActivate(c.getName()).join();
                    }
                    if (AbstractActor.class.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers()))
                    {
                        afg.getInvokerFor(c);
                    }
                });
        stage.bind();
        return stage;
    }

    public Stage createClient() {
        LifetimeExtension lifetimeExtension = new LifetimeExtension() {
            @Override
            public Task<?> preActivation(AbstractActor<?> actor) {
                serviceLocator.inject(actor);
                return Task.done();
            }
        };

        Stage client = new Stage.Builder()
                .mode(Stage.StageMode.CLIENT)
                .executionPool(commonPool)
                .clock(clock)
                .clusterName(clusterName)
                .clusterPeer(new FakeClusterPeer())
                .extensions(lifetimeExtension)
                .build();
        client.start().join();
        client.bind();
        return client;
    }

    protected ExecutionObjectCloner getExecutionObjectCloner()
    {
        return new KryoCloner();
    }
}
