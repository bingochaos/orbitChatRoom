package ClientPackage;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.concurrent.Task;
import cloud.orbit.tuples.Pair;
import org.junit.Test;

import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

/**
 * Created by bingoc on 2017/2/23.
 */
public class ClientTest extends ActorBase {

    public static class SomeChatObserver implements ClientPackage.SomeChatObserver
    {
        BlockingQueue<Pair<ClientPackage.SomeChatObserver, String>> messagesReceived = new LinkedBlockingQueue<>();

        @Override
        public Task<Void> receiveMessage(final ClientPackage.SomeChatObserver sender, final String message)
        {
            messagesReceived.add(Pair.of(sender, message));
            return Task.done();
        }

        @Override
        public int sendMessage(String message) {
            return 0;
        }


    }

    @Test
    public void observerTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        Stage stage2 = createStage();
        Stage client1 = createClient();
        Stage client2 = createClient();

        SomeChatObserver observer1 = new SomeChatObserver();
        SomeChatObserver observer2 = new SomeChatObserver();

        {
            SomeChatRoom chatRoom = Actor.getReference(SomeChatRoom.class, "chat1");
            client1.bind();
            final ClientPackage.SomeChatObserver reference1 = client1.registerObserver(null, observer1);
            chatRoom.join(observer1).get();
            final ClientPackage.SomeChatObserver reference2 = client2.registerObserver(null, observer2);
            chatRoom.join(observer2).get();

            for (int i = 0; i < 10; i++) {
                chatRoom.sendMessage(reference1, "message" + i);
                Pair message1 = observer1.messagesReceived.poll(5, TimeUnit.SECONDS);
                Pair message2 = observer2.messagesReceived.poll(5, TimeUnit.SECONDS);
                System.out.println("1: " + message1);
                System.out.println("2: " + message2);

            }
        }

//        assertEquals("bla", observer1.messagesReceived.poll(5, TimeUnit.SECONDS).getRight());
//        assertEquals("bla", observer2.messagesReceived.poll(5, TimeUnit.SECONDS).getRight());



    }
}
