package ClientPackage;

import cloud.orbit.actors.ActorObserver;
import cloud.orbit.concurrent.Task;

/**
 * Created by bingoc on 2017/2/23.
 */
public interface SomeChatObserver extends ActorObserver {
    Task<Void> receiveMessage(final SomeChatObserver sender, String message);
    int sendMessage(String message);
}
