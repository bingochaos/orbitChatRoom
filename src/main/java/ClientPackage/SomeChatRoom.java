package ClientPackage;

import cloud.orbit.actors.Actor;
import cloud.orbit.concurrent.Task;

/**
 * Created by bingoc on 2017/2/23.
 */
public interface SomeChatRoom extends Actor {

    Task<Void> join(SomeChatObserver chatObserver);

    Task<Void> sendMessage(SomeChatObserver chatObserver, String message);

    Task<?> startCountdown(int count, String message);
}
