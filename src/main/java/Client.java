import ClientPackage.ActorBase;
import cloud.orbit.actors.Stage;
import org.junit.Test;

/**
 * Created by bingoc on 2017/2/23.
 */
public class Client extends ActorBase {

    @Test(timeout = 10_000L)
    public void observerTest() {
        Stage stage1 = createStage();
        Stage stage2 = createStage();
        Stage client1 = createClient();
        Stage client2 = createClient();

    }
}
