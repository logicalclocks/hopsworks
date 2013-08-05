/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import static com.google.common.base.Charsets.UTF_8;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.jclouds.scriptbuilder.domain.OsFamily;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class SSHClientTest {

    private SSHClient client;
    private KeyProvider keys;

    public static void main(String[] args) throws IOException {
        SSHClientTest test = new SSHClientTest();
        test.init();
        test.startRunSSHCommand("193.10.64.127", "ubuntu");
    }

    public void init() {
        client = new SSHClient();
//        String publicKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDJwbktwUvTf1pKAL6TaGqZhD"
//                + "9sMQ0irG+9XHaKVVmg1Z970GYaojtgm/a56zQd1OCfoijVaVTLNJjNXAjbFSR8sg89+hSbf0S"
//                + "qhzWF5Xb99UbIDe48ZdhrKDqbCNIadUUMgbvbG7mziCtWUsDYP7rwskih4mVO09lK6ecXY"
//                + "c/RMsp09bnLf6TrM9JNJErQR0w8XsqI/TxMovjiBsOQH1TopdbrLRymVoBT/GGjgjy6Yy/yAJwqG"
//                + "c66ZsM1kIN0F32l5w8fiflesASGQyGxUutoCAgkpYY3DYlt7hdJXhTP/wuWCoI6zHWNCkYT8FG"
//                + "CPTVn650WZY9hR1Zn3Ycn4rlH a.lorenteleal@gmail.com";
//
//        String privateKey = "-----BEGIN RSA PRIVATE KEY-----"
//                + "MIICXgIBAAKBgQDkRFkyQ8yRzE9WjXnjjaroCznG72CUALvqsCFS4UcuVx+KB2xK"
//                + "FpnTqalUaTChWjsK6mOTjb2qzOqHJDsNZO8o5Ev/l/4BORbUFXf/GQlvwT9AIRL1"
//                + "qVGwRRLSRcmyJtNrX5K0iKprFY9YP4NrV+ZmoEBJFwmb9Eb/Ekpbv4p3QwIDAQAB"
//                + "AoGALE+gSQOkSIEyvYiFKskrbhQPyTBavSBPWkWKkn4sxTAgbTj3qoIspkv/FOW+"
//                + "jPPpFjtdzRzsvqU8ubMsy3LWgenC7znam3L9GpcMq4agDbmt8ybb67V3oCPi4HTQ"
//                + "T8EfmUQGJdWFuQcMp7m6lb+wmG+xAWwmMmbYGaoeXrKEhqECQQD0+p3sg5lki0QI"
//                + "2KiTA/ye1ce/fTKteBNKcLAOu4D3fqpGU/+y7mJ9wVwMvACbFARgeSzS6BnhQ+x9"
//                + "u0bCPlcrAkEA7olDJehUUcb5IGXQ4hxh6jIoE13uGUhgQj/ZDjDpj0nrpTrOMi/O"
//                + "3aLyiCIE/fjQwb2rrUyj0a8SDX7EKLzUSQJBAJcxIUw5/+50oP2QsaFiQYPJzqiY"
//                + "3TEAPW+g0peVE0gr7WzQJKxKwZB5SJU3ZmxPU1AzGP3lbyt+3zLN5SK2lNcCQQCk"
//                + "nCC0hjG6BV9iViDiCMghP9+cDdQDqoiS71CwlFx5P3/YlE47H/bXyF0qSJ+9S/lz"
//                + "2Zohi6P5TaFdor9nhXfRAkEA3qQo97plSYK935dGhyVS16c+aCptJD0xWLrYWXnR"
//                + "Ol6Nod4yjHi+dvcOuaFLBnl3eX2m8TXbkEnOEaYan3O56Q=="
//                + "-----END RSA PRIVATE KEY-----";


        try {
            //client.loadKnownHosts(new File(System.getProperty("user.home") + "/.ssh/known_hosts"));
            client.addHostKeyVerifier(new PromiscuousVerifier());
//            String publicKey = Files.toString(
//                    new File(System.getProperty("user.home") + "/.ssh/gitkey.pub"), UTF_8);
            String privateKey = Files.toString(
                    new File(System.getProperty("user.home") + "/Downloads/hortonnodes.pem"), UTF_8);
            keys = client.loadKeys(privateKey, null, null);


        } catch (IOException e) {
            System.out.println("Dafuq!!!!!");
        }
    }

    public void startRunSSHCommand(String host, String user) throws IOException {
        String publicKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDJwbktwUvTf1pKAL6TaGqZhD"
                + "9sMQ0irG+9XHaKVVmg1Z970GYaojtgm/a56zQd1OCfoijVaVTLNJjNXAjbFSR8sg89+hSbf0S"
                + "qhzWF5Xb99UbIDe48ZdhrKDqbCNIadUUMgbvbG7mziCtWUsDYP7rwskih4mVO09lK6ecXY"
                + "c/RMsp09bnLf6TrM9JNJErQR0w8XsqI/TxMovjiBsOQH1TopdbrLRymVoBT/GGjgjy6Yy/yAJwqG"
                + "c66ZsM1kIN0F32l5w8fiflesASGQyGxUutoCAgkpYY3DYlt7hdJXhTP/wuWCoI6zHWNCkYT8FG"
                + "CPTVn650WZY9hR1Zn3Ycn4rlH a.lorenteleal@gmail.com";
        client.connect(host);

        client.authPublickey(user, keys);
        try {
            final Session session = client.startSession();
            try {
                JHDFSScriptBuilder initScript = JHDFSScriptBuilder.builder()
                        .scriptType(JHDFSScriptBuilder.ScriptType.INIT)
                        .publicKey(publicKey)
                        .build();
                System.out.println(initScript.render(OsFamily.UNIX));
//                Command cmd = session.exec("sudo echo " +"'"+"#!/bin/bash\n"+initScript.render(OsFamily.UNIX)+
//                "'"+" >> " +" ~/initScript.sh");
                final Command cmd = session.exec(initScript.render(OsFamily.UNIX));
                System.out.println(IOUtils.readFully(cmd.getInputStream()).toString());
//                cmd.join(36000, TimeUnit.SECONDS);
//                cmd=session.exec("chmod u+x initScript.sh");
//                System.out.println(IOUtils.readFully(cmd.getInputStream()).toString());
//                cmd.join(36000, TimeUnit.SECONDS);
//                cmd=session.exec("./initScript.sh");
//                cmd.join(36000, TimeUnit.SECONDS);
                System.out.println("\n** exit status: " + cmd.getExitStatus());
            } finally {
                session.close();
            }
        } finally {
            client.disconnect();
        }
    }
}
