import cloud.orbit.actors.Stage;

/**
 * Created by bingoc on 2017/2/23.
 */
public class Server {

    public static void main(String[] args) throws Exception {
        Stage stage = new Stage.Builder().clusterName("orbit-chatRoom-cluster").build();
        stage.start().join();
        stage.bind();

        stage.stop().join();
    }
}
